package com.zeroday.handler

import com.zeroday.protocol.ServerError
import io.ktor.websocket.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Context provided to every message handler.
 *
 * Encapsulates dependencies that almost every handler needs (services, the
 * current connection, the current authenticated player id, and a sink for
 * broadcasting to other clients) so handler signatures stay focused on their
 * own logic.
 */
class HandlerContext(
    val session: WebSocketSession,
    val connection: ConnectionInfo,
    val services: ServiceRegistry,
    val router: MessageRouter
) {
    /** Currently authenticated player id, or null if the connection is anonymous. */
    val playerId: String? get() = connection.playerId

    fun authenticatedPlayerId(): String? = connection.playerId
}

/**
 * Metadata about a single open WebSocket connection. Owned by [ConnectionRegistry].
 *
 * [subscribedChannels] is a per-connection filter the client controls
 * via `subscribe`/`unsubscribe` request types. Handlers that produce
 * streaming updates (inventory, world events, reputation changes, …)
 * should check `connection.isSubscribed("inventory")` before pushing
 * so a client that only cares about player stats doesn't receive
 * inventory frames it will discard anyway.
 *
 * Default channels (always active):
 *   - "player"      — status, level, resources
 *   - "notifications" — inbox badge and new notifications
 *
 * Optional channels (opt-in):
 *   - "inventory"   — item drops, ZDE changes
 *   - "tasks"       — active task list changes
 *   - "faction"     — faction reputation, invites
 *   - "events"      — world event announcements
 *   - "chat"        — future chat messages
 *   - "network"     — network scan results
 */
data class ConnectionInfo(
    val sessionId: String,
    var playerId: String? = null,
    var username: String? = null,
    val connectedAt: Long = System.currentTimeMillis(),
    var lastActivity: Long = System.currentTimeMillis(),
    val remoteAddress: String? = null,
    /** Channels the client has opted into. */
    val subscribedChannels: MutableSet<String> = DEFAULT_CHANNELS.toMutableSet()
) {
    fun isAuthenticated(): Boolean = playerId != null

    fun isSubscribed(channel: String): Boolean = channel in subscribedChannels

    fun subscribe(channel: String): Boolean = subscribedChannels.add(channel)

    fun unsubscribe(channel: String): Boolean = subscribedChannels.remove(channel)

    companion object {
        val DEFAULT_CHANNELS = setOf("player", "notifications")
        val ALL_CHANNELS = setOf(
            "player", "notifications", "inventory", "tasks",
            "faction", "events", "chat", "network"
        )
    }
}

/**
 * Result returned by a [MessageHandler]. The router turns this into a JSON
 * envelope and ships it back to the client.
 */
sealed class HandlerResult {
    abstract fun toEnvelope(): Map<String, Any?>

    data class Ok(val type: String, val payload: Map<String, Any?> = emptyMap()) : HandlerResult() {
        override fun toEnvelope(): Map<String, Any?> = mapOf("type" to type) + payload
    }

    data class Err(val error: ServerError, val type: String = "error") : HandlerResult() {
        override fun toEnvelope(): Map<String, Any?> = mapOf(
            "type" to type,
            "error" to error.toMap()
        )
    }
}

/**
 * Implemented by every WebSocket message handler. A handler owns one logical
 * concern (auth, tasks, factions, ...) and one or more request types.
 *
 * Handlers should be small, focused classes — splitting them up keeps the
 * router loop trivial and avoids the "god class" anti-pattern that plagued
 * the previous monolithic WebSocketHandler.
 */
interface MessageHandler {
    /** The set of request type names this handler wants to receive. */
    val handledTypes: Set<String>

    /** Whether this handler requires an authenticated player. */
    val requiresAuth: Boolean get() = true

    suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult
}

internal fun JsonObject.string(name: String): String? =
    (this[name] as? JsonPrimitive)?.contentOrNull

private val JsonPrimitive.contentOrNull: String?
    get() = if (isString) content else null
