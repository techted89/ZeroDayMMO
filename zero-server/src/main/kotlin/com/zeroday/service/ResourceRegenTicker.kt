package com.zeroday.service

import com.zeroday.util.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * Single-tick resource regeneration.
 *
 * The previous design spawned a coroutine per online player, each running
 * its own 5 s `delay` loop. With N players that means N coroutines, N
 * timer objects, and N wakeups per cycle. For 500 online players that's
 * a sustained 100 wakeups/s just for regen.
 *
 * This service instead runs **one** background coroutine that ticks
 * every [intervalMs] and calls [PlayerService.regenerateAllOnline].
 * The per-player lock is taken briefly per player inside that loop.
 *
 * The ticker also tracks how many regens actually fired
 * ([actualRegens]) versus how many ticks it has run
 * ([ticks]). Operators can use the ratio to verify the loop is doing
 * work without needing to scrape /health.
 */
class ResourceRegenTicker(
    private val playerService: PlayerService,
    val intervalMs: Long = 5_000L
) {
    private val log = LoggerFactory.getLogger(ResourceRegenTicker::class.java)
    private val mutex = Mutex()
    private val ticks = AtomicLong(0)
    private val actualRegens = AtomicLong(0)
    @Volatile private var lastTickAt: Long = 0
    @Volatile private var job: Job? = null

    suspend fun start(scope: CoroutineScope = AppScope.scope) {
        mutex.withLock {
            if (job?.isActive == true) return@withLock
            job = scope.launch {
                log.info("Resource-regen ticker started: interval={}ms", intervalMs)
                while (isActive) {
                    delay(intervalMs)
                    try {
                        val n = playerService.regenerateAllOnline()
                        actualRegens.addAndGet(n.toLong())
                        ticks.incrementAndGet()
                        lastTickAt = System.currentTimeMillis()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.warn("Resource-regen tick failed: {}", e.message)
                    }
                }
            }
        }
    }

    suspend fun stop() {
        mutex.withLock {
            job?.cancel()
            job = null
        }
    }

    fun status(): Map<String, Any?> = mapOf(
        "intervalMs" to intervalMs,
        "ticks" to ticks.get(),
        "actualRegens" to actualRegens.get(),
        "lastTickAt" to lastTickAt,
        "running" to (job?.isActive == true)
    )
}
