package com.zeroday.handler

import com.zeroday.security.AuditLog
import com.zeroday.util.AppScope
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Background watchdog that reclaims resources from stale WebSocket
 * connections.
 *
 * Every [sweepIntervalMs] the watchdog walks the active connection
 * table and closes any entry whose [ConnectionInfo.lastActivity] is
 * older than [idleTimeoutMs]. The close is graceful (NORMAL close
 * frame) so the client's `onClose` fires cleanly.
 *
 * The watchdog also logs a one-line summary per sweep so operators
 * can spot a slow-leak scenario without a separate monitor.
 */
class ConnectionWatchdog(
    private val registry: ConnectionRegistry,
    val idleTimeoutMs: Long = 300_000L,   // 5 minutes
    val sweepIntervalMs: Long = 30_000L   // sweep every 30 s
) {
    private val log = LoggerFactory.getLogger(ConnectionWatchdog::class.java)
    private val mutex = Mutex()
    private val sweeps = AtomicInteger(0)
    private val closedCount = AtomicInteger(0)
    @Volatile private var lastSweepAt: Long = 0

    suspend fun start(scope: CoroutineScope = AppScope.scope) {
        mutex.withLock {
            scope.launch {
                log.info(
                    "Connection watchdog started: idleTimeout={}ms sweepInterval={}ms",
                    idleTimeoutMs, sweepIntervalMs
                )
                while (isActive) {
                    delay(sweepIntervalMs)
                    try {
                        sweep()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.warn("Connection watchdog sweep failed: {}", e.message)
                    }
                }
            }
        }
    }

    /**
     * Exposed for testing. Normal operation drives [sweep] from the
     * background loop.
     */
    suspend fun sweepNow() = sweep()

    private suspend fun sweep() {
        val now = System.currentTimeMillis()
        val deadline = now - idleTimeoutMs
        val stale = registry.all().filter {
            now - it.info.lastActivity >= idleTimeoutMs
        }
        if (stale.isEmpty()) {
            sweeps.incrementAndGet()
            lastSweepAt = now
            return
        }
        for (entry in stale) {
            val sid = entry.info.sessionId
            val pid = entry.info.playerId
            AuditLog.event("watchdog.idle_close", playerId = pid, extra = mapOf(
                "sessionId" to sid,
                "sessions" to now - entry.info.lastActivity
            ))
            runCatching {
                entry.session.close(CloseReason(CloseReason.Codes.NORMAL, "Idle timeout"))
            }
            registry.unregister(sid)
            closedCount.incrementAndGet()
            log.info("Idle connection closed: sessionId={} playerId={}", sid, pid)
        }
        sweeps.incrementAndGet()
        lastSweepAt = now
        log.info("Sweep done: {} stale connections closed from {} total", stale.size, registry.totalConnections())
    }

    fun status(): Map<String, Any?> = mapOf(
        "idleTimeoutMs" to idleTimeoutMs,
        "sweepIntervalMs" to sweepIntervalMs,
        "sweeps" to sweeps.get(),
        "closedCount" to closedCount.get(),
        "lastSweepAt" to lastSweepAt
    )
}
