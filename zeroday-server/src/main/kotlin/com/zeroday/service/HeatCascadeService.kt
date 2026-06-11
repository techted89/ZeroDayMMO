package com.zeroday.service

import com.zeroday.model.AchievementEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HeatCascadeService(
    private val playerService: PlayerService,
    private val careerService: CareerService,
    private val gameEventBus: GameEventBus? = null
) {
    private val hunters = mutableMapOf<String, HunterSession>()
    private val mutex = Mutex()
    private var loopJob: Job? = null

    companion object {
        private const val HEAT_THRESHOLD = 60
        private const val HUNT_INTERVAL_MS = 30_000L
    }

    data class HunterSession(
        val hunterId: String,
        val targetId: String,
        val targetName: String,
        val targetHeat: Int,
        val startedAt: Long = System.currentTimeMillis(),
        var lastAttemptAt: Long = System.currentTimeMillis(),
        var attemptCount: Int = 0
    )

    data class HunterInfo(
        val hunterId: String,
        val targetId: String,
        val targetName: String,
        val targetHeat: Int,
        val timeSinceLastAttemptMs: Long,
        val attemptCount: Int
    )

    fun start(scope: CoroutineScope) {
        loopJob = scope.launch {
            while (isActive) {
                delay(HUNT_INTERVAL_MS)
                processHeatCascade()
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
    }

    private suspend fun processHeatCascade() {
        for (player in playerService.getAllPlayers()) {
            if (player.careerPath != "blackhat") continue
            if (player.isInJail) continue

            if (player.heatLevel >= HEAT_THRESHOLD) {
                ensureHunter(player.id, player.username, player.heatLevel)
            } else {
                mutex.withLock { hunters.remove(player.id) }
            }
        }

        // Process active hunters
        val now = System.currentTimeMillis()
        val active = mutex.withLock { hunters.values.toList() }
        for (hunter in active) {
            if (now - hunter.lastAttemptAt >= HUNT_INTERVAL_MS) {
                executeHunterAttempt(hunter)
            }
        }
    }

    private suspend fun ensureHunter(targetId: String, targetName: String, targetHeat: Int) {
        mutex.withLock {
            if (!hunters.containsKey(targetId)) {
                val hunterId = "hunter_${targetId}_${System.currentTimeMillis()}"
                hunters[targetId] = HunterSession(
                    hunterId = hunterId,
                    targetId = targetId,
                    targetName = targetName,
                    targetHeat = targetHeat
                )
                gameEventBus?.emit(GameEvent(targetId, AchievementEvent.HEAT_CHANGED, targetHeat.toLong()))
            }
        }
    }

    private suspend fun executeHunterAttempt(hunter: HunterSession) {
        val target = playerService.getPlayer(hunter.targetId) ?: return
        if (target.careerPath != "blackhat" || target.heatLevel < HEAT_THRESHOLD || target.isInJail) {
            mutex.withLock { hunters.remove(hunter.targetId) }
            return
        }

        val successChance = 0.1 + (target.heatLevel - HEAT_THRESHOLD) * 0.005
        val success = Math.random() < successChance.coerceIn(0.05, 0.5)

        if (success) {
            playerService.withPlayerAction(hunter.targetId) { p ->
                p.isInJail = true
                p.jailTimeRemaining = (p.heatLevel * 0.2f).coerceIn(60f, 1800f)
                p.heatLevel = (p.heatLevel / 2).coerceAtLeast(0)
                p.bountyPrice = 0L
            }
            gameEventBus?.emit(GameEvent(hunter.targetId, AchievementEvent.ARREST_EXECUTED, 1))
            mutex.withLock { hunters.remove(hunter.targetId) }
        } else {
            val updated = hunters[hunter.targetId]?.apply {
                lastAttemptAt = System.currentTimeMillis()
                attemptCount += 1
            }
            if (updated != null && updated.attemptCount >= 5) {
                // After 5 failed attempts, escalate
                playerService.withPlayerAction(hunter.targetId) { p ->
                    p.heatLevel = (p.heatLevel + 5).coerceAtMost(p.maxHeatLevel)
                }
            }
        }
    }

    suspend fun getActiveHunters(): List<HunterInfo> {
        val now = System.currentTimeMillis()
        return mutex.withLock {
            hunters.values.map { h ->
                HunterInfo(
                    hunterId = h.hunterId,
                    targetId = h.targetId,
                    targetName = h.targetName,
                    targetHeat = h.targetHeat,
                    timeSinceLastAttemptMs = now - h.lastAttemptAt,
                    attemptCount = h.attemptCount
                )
            }
        }
    }

    suspend fun isPlayerHunted(playerId: String): Boolean =
        mutex.withLock { hunters.containsKey(playerId) }
}
