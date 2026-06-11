package com.zeroday.service

import com.zeroday.model.TaskRewards
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class AdRewardService(
    private val playerService: PlayerService
) {
    private val adCooldownMs = TimeUnit.MINUTES.toMillis(5)
    private val lastAdWatch = mutableMapOf<String, Long>()
    private val mutex = Mutex()

    suspend fun processAdReward(playerId: String): AdRewardResult = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock AdRewardResult.FAILED("Player not found")
        val now = System.currentTimeMillis()
        val lastWatch = lastAdWatch[playerId] ?: 0L

        if (now - lastWatch < adCooldownMs) {
            val remainingMs = adCooldownMs - (now - lastWatch)
            val remainingMin = (remainingMs / 60_000).toInt() + 1
            return@withLock AdRewardResult.ON_COOLDOWN(remainingMin)
        }

        val credits = Random.nextLong(10L, 51L)
        val cpu = Random.nextInt(1, 4)
        val ram = Random.nextInt(1, 4)
        val bandwidth = Random.nextInt(1, 3)

        val rewards = TaskRewards(
            experience = 0L,
            credits = credits,
            reputation = 0,
            cpuUpgrade = cpu,
            ramUpgrade = ram,
            bandwidthUpgrade = bandwidth
        )

        playerService.addResources(playerId, rewards)
        lastAdWatch[playerId] = now

        AdRewardResult.SUCCESS(rewards)
    }

    suspend fun getAdCooldownRemaining(playerId: String): Long = mutex.withLock {
        val lastWatch = lastAdWatch[playerId] ?: 0L
        val elapsed = System.currentTimeMillis() - lastWatch
        if (elapsed >= adCooldownMs) 0L else adCooldownMs - elapsed
    }

    sealed class AdRewardResult {
        data class SUCCESS(val rewards: TaskRewards) : AdRewardResult()
        data class ON_COOLDOWN(val minutesRemaining: Int) : AdRewardResult()
        data class FAILED(val message: String) : AdRewardResult()
    }
}
