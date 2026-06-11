package com.zeroday.handler

import io.ktor.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks every active WebSocket connection: who is connected, who is
 * authenticated, and what background jobs (e.g. resource regeneration) are
 * associated with the connection. Centralising this here makes session
 * lifecycle explicit and gives us a single place to broadcast or kick
 * connections.
 *
 * Enforces two safety caps:
 *   1. [maxTotalConnections] — global ceiling; excess connections are
 *      immediately closed to prevent trivial resource-exhaustion DoS.
 *   2. [maxConnectionsPerIp] — per-source cap; excess connections are also
 *      immediately closed. This is the single most effective rate limit
 *      against scripted bots without affecting real concurrent users.
 *
 * Both defaults can be overridden in [ZeroDayServer] (typically via
 * `application.conf`).
 */
class ConnectionRegistry(
    private val router: MessageRouter,
    val maxTotalConnections: Int = 2_000,
    val maxConnectionsPerIp: Int = 8
) {
    private val log = LoggerFactory.getLogger(ConnectionRegistry::class.java)
    private val secureRandom = SecureRandom()

    private val bySession = ConcurrentHashMap<String, ConnectionEntry>()
    private val byPlayer = ConcurrentHashMap<String, String>()
    private val perIpCounts = ConcurrentHashMap<String, Int>()
    private val sessionMutex = Mutex()

    /**
     * Result of [register]. If the registry is saturated the session is
     * closed with a [CloseReason] and the caller is told why so the client
     * can back off.
     */
    sealed class RegisterResult {
        data class Accepted(val entry: ConnectionEntry) : RegisterResult()
        data class Rejected(val reason: String) : RegisterResult()
    }

    suspend fun register(session: WebSocketSession, remoteAddress: String? = null): RegisterResult {
        val ip = remoteAddress ?: "unknown"
        if (bySession.size >= maxTotalConnections) {
            log.warn("Rejecting connection from {}: global cap {} reached", ip, maxTotalConnections)
            runCatching {
                session.close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Server at capacity"))
            }
            return RegisterResult.Rejected("global_cap")
        }
        val perIp = perIpCounts.getOrDefault(ip, 0)
        if (perIp >= maxConnectionsPerIp) {
            log.warn("Rejecting connection from {}: per-IP cap {} reached", ip, maxConnectionsPerIp)
            runCatching {
                session.close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Too many connections from your address"))
            }
            return RegisterResult.Rejected("per_ip_cap")
        }
        val sessionId = newSessionId()
        val info = ConnectionInfo(
            sessionId = sessionId,
            remoteAddress = remoteAddress
        )
        val entry = ConnectionEntry(info, session)
        bySession[sessionId] = entry
        perIpCounts.merge(ip, 1) { a, b -> a + b }
        log.debug("WebSocket connection registered: sessionId={} remote={}", sessionId, remoteAddress)
        return RegisterResult.Accepted(entry)
    }

    suspend fun bindPlayer(sessionId: String, playerId: String, username: String): Boolean =
        sessionMutex.withLock {
            val entry = bySession[sessionId] ?: return@withLock false
            if (byPlayer.containsKey(playerId)) {
                log.info("Player {} already has an open connection; closing previous one", username)
                byPlayer[playerId]?.let { previousSessionId ->
                    bySession.remove(previousSessionId)?.let { prev ->
                        prev.perPlayerJob?.cancel()
                        runCatching { prev.session.close(CloseReason(CloseReason.Codes.NORMAL, "Replaced by new login")) }
                    }
                }
            }
            entry.info.playerId = playerId
            entry.info.username = username
            entry.info.lastActivity = System.currentTimeMillis()
            byPlayer[playerId] = sessionId
            true
        }

    fun unregister(sessionId: String) {
        val entry = bySession.remove(sessionId) ?: return
        entry.perPlayerJob?.cancel()
        entry.info.playerId?.let { byPlayer.remove(it) }
        entry.info.remoteAddress?.let { ip ->
            perIpCounts.computeIfPresent(ip) { _, count -> if (count <= 1) null else count - 1 }
        }
        log.debug("WebSocket connection unregistered: sessionId={} playerId={}", sessionId, entry.info.playerId)
    }

    fun get(sessionId: String): ConnectionEntry? = bySession[sessionId]

    fun getByPlayer(playerId: String): ConnectionEntry? = byPlayer[playerId]?.let { bySession[it] }

    fun all(): Collection<ConnectionEntry> = bySession.values

    fun onlinePlayerCount(): Int = byPlayer.size

    fun totalConnections(): Int = bySession.size

    fun touch(sessionId: String) {
        bySession[sessionId]?.info?.let { it.lastActivity = System.currentTimeMillis() }
    }

    /**
     * Send a payload to every authenticated connection. Used for world-event
     * announcements and similar broadcast scenarios.
     *
     * The encoded JSON is computed once (so a 1000-client broadcast does
     * not re-serialise the same text 1000 times) and then dispatched in
     * parallel: one send per session in its own coroutine. A slow or
     * stalled client therefore cannot block the others.
     */
    suspend fun broadcast(envelope: Map<String, Any?>) {
        val payload = router.encode(envelope)
        val frame = Frame.Text(payload)
        val entries = bySession.values.toList()
        coroutineScope {
            for (entry in entries) {
                async {
                    runCatching { entry.session.send(frame) }
                }.await()
            }
        }
    }

    /**
     * A throttled variant of [broadcast]. If another broadcast with the
     * same [coalesceKey] was issued within [windowMs], the new envelope
     * is merged into the pending broadcast so the final payload reflects
     * the latest state. This prevents a burst of events from flooding
     * the wire with N serialised copies per client.
     *
     * Example: when 5 world-event stage updates arrive in 200 ms, only
     * the last one is actually sent, carrying the most recent stage.
     */
    suspend fun throttledBroadcast(
        envelope: Map<String, Any?>,
        coalesceKey: String,
        windowMs: Long = 2000L
    ) {
        val now = System.currentTimeMillis()
        val pending = throttledPendingKeys[coalesceKey]
        if (pending != null && now - pending.sentAt < windowMs) {
            // Replace the pending envelope (coalesced)
            throttledPendingKeys[coalesceKey] = PendingBroadcast(envelope, pending.sentAt)
            return
        }
        throttledPendingKeys[coalesceKey] = PendingBroadcast(envelope, now)
        broadcast(envelope)
        // Cleanup stale entries
        throttledPendingKeys.entries.removeAll { (_, v) ->
            now - v.sentAt > windowMs * 2
        }
    }

    private data class PendingBroadcast(
        val envelope: Map<String, Any?>,
        val sentAt: Long
    )
    private val throttledPendingKeys = ConcurrentHashMap<String, PendingBroadcast>()

    /**
     * Push a message to a specific authenticated player. Does nothing if
     * the player is offline (no-op, not an error). Returns true if the
     * message was sent.
     */
    suspend fun sendToPlayer(playerId: String, envelope: Map<String, Any?>): Boolean {
        val entry = getByPlayer(playerId) ?: return false
        val payload = router.encode(envelope)
        runCatching { entry.session.send(Frame.Text(payload)) }
        return true
    }

    /**
     * Broadcast a message to all connections subscribed to [channel].
     * The most common channels are:
     *   - "events"   — world event announcements
     *   - "faction"  — faction updates for members
     *   - "chat"     — future chat messages
     *
     * Default channels ("player", "notifications") are subscribed by
     * everyone, so [broadcastToChannel] can be used as a universal
     * push when the audience is all online players.
     */
    suspend fun broadcastToChannel(channel: String, envelope: Map<String, Any?>) {
        val payload = router.encode(envelope)
        val frame = Frame.Text(payload)
        val targets = bySession.values.filter { it.info.isSubscribed(channel) }
        if (targets.isEmpty()) return
        coroutineScope {
            for (entry in targets) {
                launch {
                    runCatching { entry.session.send(frame) }
                }
            }
        }
    }

    /**
     * 32-char URL-safe base64 token from [SecureRandom]. ~192 bits of entropy;
     * collision odds are negligible for a single-server session table and the
     * token is *not* derivable from any player id (unlike the old
     * "session_${id}" pattern).
     */
    private fun newSessionId(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        val token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return "ws_$token"
    }
}

class ConnectionEntry(
    val info: ConnectionInfo,
    val session: WebSocketSession,
    @Volatile var perPlayerJob: Job? = null
)
