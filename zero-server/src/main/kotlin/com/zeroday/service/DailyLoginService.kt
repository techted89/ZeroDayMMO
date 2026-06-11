package com.zeroday.service

import com.zeroday.model.NotificationType
import com.zeroday.model.Player

/**
 * Tracks daily login streaks, grants streak rewards, calculates offline
 * resource accumulation, and auto-rotates challenges on login.
 *
 * All methods are thread-safe because they operate on [Player] only through
 * [PlayerService.withPlayerAction], which takes the per-player lock.
 */
class DailyLoginService(
    private val playerService: PlayerService,
    private val notificationService: NotificationService? = null,
    private val challengeService: ChallengeService? = null
) {
    companion object {
        /** Maximum offline accumulation window (millis). */
        const val MAX_OFFLINE_MILLIS: Long = 28_800_000L   // 8h

        /** Minimum offline duration to grant idle rewards. */
        const val MIN_OFFLINE_MILLIS: Long = 3_600_000L    // 1h

        /** Resources granted per hour of offline time. */
        const val OFFLINE_CPU_PER_HOUR: Int = 2
        const val OFFLINE_RAM_PER_HOUR: Int = 4
        const val OFFLINE_CREDITS_PER_HOUR_PER_LEVEL: Long = 15

        /** Minimum time between logins to count as a new day (16h). */
        const val STREAK_WINDOW_MILLIS: Long = 57_600_000L

        /** One day in millis (~40h max gap to allow skipping a day). */
        const val DAY_MILLIS: Long = 86_400_000L

        fun dailyStreakReward(streakDay: Int): StreakReward {
            // Cycle every 7 days for predictability.
            val day = ((streakDay - 1) % 7) + 1
            return when (day) {
                1 -> StreakReward(credits = 100, xp = 50)
                2 -> StreakReward(credits = 200, xp = 100)
                3 -> StreakReward(credits = 350, xp = 175)
                4 -> StreakReward(credits = 500, xp = 250)
                5 -> StreakReward(credits = 750, xp = 400)
                6 -> StreakReward(credits = 1_000, xp = 600)
                7 -> StreakReward(credits = 2_000, xp = 1_000, reputation = 5)
                else -> StreakReward(credits = 100, xp = 50)
            }
        }
    }

    data class StreakReward(
        val credits: Long = 0,
        val xp: Long = 0,
        val reputation: Int = 0
    )

    data class LoginResult(
        val streak: Int,
        val isNewStreak: Boolean,
        val streakReward: StreakReward?,
        val offlineReward: OfflineAccumulation?,
        val message: String
    )

    data class OfflineAccumulation(
        val cpuGained: Int,
        val ramGained: Int,
        val creditsGained: Long,
        val offlineHours: Double
    )

    /**
     * Process a player login. Updates streak, grants rewards, and pushes
     * notifications. Must be called after [PlayerService.login] succeeds.
     * Returns null if the player does not exist.
     */
    suspend fun processLogin(
        playerId: String,
        ipAddress: String? = null
    ): LoginResult? {
        return playerService.withPlayerAction(playerId) { player ->
            val now = System.currentTimeMillis()
            val offline = calculateOffline(player, now)
            val streakResult = updateStreak(player, now)
            val reward = streakResult.reward

            // Apply offline accumulation.
            if (offline != null) {
                player.cpu = (player.cpu + offline.cpuGained).coerceAtMost(player.maxCpu)
                player.ram = (player.ram + offline.ramGained).coerceAtMost(player.maxRam)
                player.credits += offline.creditsGained
                player.lifetimeCreditsEarned += offline.creditsGained
            }

            // Apply streak reward.
            if (reward != null) {
                player.credits += reward.credits
                player.lifetimeCreditsEarned += reward.credits
                player.experience += reward.xp
                player.reputation += reward.reputation
                checkForLevelUp(player)
            }

            // Update login tracking.
            player.lastLoginAt = now
            player.totalLogins++
            if (streakResult.isNewStreak) player.loginStreak = streakResult.streak
            if (player.loginStreak > player.longestStreak) player.longestStreak = player.loginStreak
            player.lastLoginIp = ipAddress ?: player.lastLoginIp

            // Auto-rotate challenges.
            challengeService?.rotateIfNeeded(player)

            // Build message.
            val parts = mutableListOf<String>()
            if (offline != null && offline.creditsGained > 0) {
                parts.add("While you were away (+${offline.offlineHours.format(1)}h): +${offline.creditsGained}cr, +${offline.cpuGained}CPU, +${offline.ramGained}RAM")
            }
            if (reward != null) {
                parts.add("Day ${streakResult.streak} streak reward: +${reward.credits}cr, +${reward.xp}xp${if (reward.reputation > 0) ", +${reward.reputation} rep" else ""}")
                parts.add("Next reward in: day ${(streakResult.streak % 7) + 1}")
            }
            val message = if (parts.isEmpty()) "Welcome back!" else parts.joinToString("\n")

            // Push notifications.
            if (reward != null) {
                notificationService?.push(
                    player = player,
                    type = NotificationType.DAILY_LOGIN,
                    title = "Daily Login Streak: Day ${streakResult.streak}",
                    message = "You earned +${reward.credits}cr, +${reward.xp}xp${if (reward.reputation > 0) " and +${reward.reputation} rep" else ""} for logging in today!",
                    data = mapOf(
                        "streak" to streakResult.streak.toString(),
                        "credits" to reward.credits.toString(),
                        "xp" to reward.xp.toString(),
                        "reputation" to reward.reputation.toString()
                    )
                )
            }

            LoginResult(
                streak = streakResult.streak,
                isNewStreak = streakResult.isNewStreak,
                streakReward = reward,
                offlineReward = offline,
                message = message
            )
        } ?: run {
            // Player was deleted between login and here.
            LoginResult(
                streak = 0,
                isNewStreak = false,
                streakReward = null,
                offlineReward = null,
                message = "Player not found"
            )
        }
    }

    /**
     * Non-mutating: calculate what the player would have earned while
     * offline without applying anything.
     */
    fun calculateOffline(player: Player, now: Long = System.currentTimeMillis()): OfflineAccumulation? {
        if (player.lastLoginAt <= 0L) return null
        val elapsed = now - player.lastLoginAt
        if (elapsed < MIN_OFFLINE_MILLIS) return null
        val capped = elapsed.coerceAtMost(MAX_OFFLINE_MILLIS)
        val hours = capped.toDouble() / 3_600_000.0
        return OfflineAccumulation(
            cpuGained = (hours * OFFLINE_CPU_PER_HOUR).toInt().coerceAtLeast(0),
            ramGained = (hours * OFFLINE_RAM_PER_HOUR).toInt().coerceAtLeast(0),
            creditsGained = (hours * player.level * OFFLINE_CREDITS_PER_HOUR_PER_LEVEL).toLong().coerceAtLeast(0),
            offlineHours = hours
        )
    }

    // ---- internal ----

    private data class StreakResult(
        val streak: Int,
        val isNewStreak: Boolean,
        val reward: StreakReward?
    )

    /**
     * Evaluate and return the new streak value and reward. Does NOT mutate
     * [player] — the caller [processLogin] applies rewards.
     */
    private fun updateStreak(player: Player, now: Long): StreakResult {
        val sinceLast = now - player.lastLoginAt

        // First ever login.
        if (player.lastLoginAt <= 0L) {
            return StreakResult(1, true, dailyStreakReward(1))
        }

        // Same calendar day: no streak change, no reward.
        if (sinceLast < STREAK_WINDOW_MILLIS) {
            return StreakResult(player.loginStreak.coerceAtLeast(1), false, null)
        }

        // Consecutive day (within ~40h window to allow some flexibility):
        // the player logged in yesterday or the day before.
        val maxGap = STREAK_WINDOW_MILLIS + DAY_MILLIS // ~40h
        if (sinceLast < maxGap) {
            val newStreak = player.loginStreak + 1
            return StreakResult(newStreak, true, dailyStreakReward(newStreak))
        }

        // Streak broken: more than 40h since last login.
        return StreakResult(1, true, dailyStreakReward(1))
    }

    private fun checkForLevelUp(player: Player) {
        var leveledUp = false
        var reachedLevel = player.level
        while (player.experience >= player.experienceToNext) {
            player.experience -= player.experienceToNext
            player.level++
            player.experienceToNext = (player.experienceToNext * 1.5).toLong()
            player.maxCpu += 5
            player.maxRam += 16
            player.maxBandwidth += 2
            player.cpu = player.maxCpu
            player.ram = player.maxRam
            player.bandwidth = player.maxBandwidth
            player.skillPoints += if (player.level % 5 == 0) 2 else 1
            leveledUp = true
            reachedLevel = player.level
        }
        if (leveledUp) player.lastLevelNotified = reachedLevel
    }
}

/** Format double to N decimal places without pulling in extra deps. */
private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
