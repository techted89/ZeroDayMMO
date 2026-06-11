package com.zeroday.service

import com.zeroday.handler.ConnectionRegistry
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Periodic server-wide tasks that run in the background: stats logging
 * and maintenance cleanup.
 *
 * Tasks run in a single loop every 60 s. Each task's [block] is wrapped
 * in a try-catch so a single failing task cannot stall the scheduler.
 */
class ServerScheduler(
    private val playerService: PlayerService,
    private val connectionRegistry: ConnectionRegistry,
    private val persistence: PlayerPersistence? = null
) {
    private val log = LoggerFactory.getLogger(ServerScheduler::class.java)
    private var job: Job? = null

    /**
     * Start the background loop. Replaces any previously running loop.
     */
    fun start(scope: CoroutineScope) {
        stop()
        job = scope.launch {
            while (isActive) {
                try {
                    val online = playerService.getOnlinePlayers().size
                    val total = playerService.getAllPlayers().size
                    val conns = connectionRegistry.totalConnections()
                    if (online > 0 || conns > 0) {
                        log.info("scheduler stats: online={} total={} connections={}", online, total, conns)
                    }
                } catch (e: Exception) {
                    log.warn("scheduler task failed", e)
                }
                delay(60_000L)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
