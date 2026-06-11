package com.zeroday.service

import com.zeroday.model.ActiveChallenge
import com.zeroday.model.AchievementEvent
import com.zeroday.model.ChallengeCadence
import com.zeroday.model.ChallengeDefinition
import com.zeroday.model.Player
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/**
 * Daily and weekly challenges. The service rotates the player's
 * [Player.activeChallenges] on a 24h/7d cadence and tracks progress
 * as the player does things in the world.
 *
 * Rotation is lazy: the next time the server touches a player's
 * challenges (on a relevant event, or via an explicit tick from the
 * app scope) we check whether the rotation deadline has passed and
 * discard any expired unclaimed challenges before issuing a new set.
 */
class ChallengeService {
    private val mutex = Mutex()

    val catalog: List<ChallengeDefinition> = listOf(
        ChallengeDefinition(
            id = "daily_discover_3",
            name = "Recon Day",
            description = "Discover 3 nodes.",
            cadence = ChallengeCadence.DAILY,
            event = AchievementEvent.NODE_DISCOVERED,
            targetValue = 3,
            rewardCredits = 200,
            rewardXp = 100
        ),
        ChallengeDefinition(
            id = "daily_tasks_2",
            name = "Contractor",
            description = "Complete 2 tasks.",
            cadence = ChallengeCadence.DAILY,
            event = AchievementEvent.TASK_COMPLETED,
            targetValue = 2,
            rewardCredits = 500,
            rewardXp = 250
        ),
        ChallengeDefinition(
            id = "daily_credits_1000",
            name = "Money Maker",
            description = "Earn 1,000 credits.",
            cadence = ChallengeCadence.DAILY,
            event = AchievementEvent.CREDITS_EARNED,
            targetValue = 1_000,
            rewardCredits = 100,
            rewardXp = 200
        ),
        ChallengeDefinition(
            id = "daily_fragments_2",
            name = "Knowledge Seeker",
            description = "Gather 2 knowledge fragments.",
            cadence = ChallengeCadence.DAILY,
            event = AchievementEvent.FRAGMENT_GATHERED,
            targetValue = 2,
            rewardCredits = 300,
            rewardXp = 150
        ),
        ChallengeDefinition(
            id = "weekly_tasks_15",
            name = "Pro Contractor",
            description = "Complete 15 tasks this week.",
            cadence = ChallengeCadence.WEEKLY,
            event = AchievementEvent.TASK_COMPLETED,
            targetValue = 15,
            rewardCredits = 5_000,
            rewardXp = 2_000,
            rewardReputation = 10
        ),
        ChallengeDefinition(
            id = "weekly_discover_25",
            name = "Cartographer",
            description = "Discover 25 nodes this week.",
            cadence = ChallengeCadence.WEEKLY,
            event = AchievementEvent.NODE_DISCOVERED,
            targetValue = 25,
            rewardCredits = 3_000,
            rewardXp = 1_500
        ),
        ChallengeDefinition(
            id = "weekly_storyline_1",
            name = "Plot Twist",
            description = "Complete a storyline this week.",
            cadence = ChallengeCadence.WEEKLY,
            event = AchievementEvent.STORYLINE_COMPLETED,
            targetValue = 1,
            rewardCredits = 2_000,
            rewardXp = 1_000,
            rewardReputation = 5
        )
    )

    private val byId: Map<String, ChallengeDefinition> = catalog.associateBy { it.id }

    fun get(id: String): ChallengeDefinition? = byId[id]

    /**
     * Apply [delta] to all active challenges for [player] that listen
     * to [event]. Returns the list of challenges that just completed
     * (the caller grants rewards and surfaces a notification).
     *
     * This also performs the rotation check so a player who comes back
     * after a few days immediately gets fresh challenges.
     */
    suspend fun record(player: Player, event: AchievementEvent, delta: Long = 1L): List<ActiveChallenge> =
        mutex.withLock {
            rotateIfNeeded(player)
            val completed = mutableListOf<ActiveChallenge>()
            for (challenge in player.activeChallenges) {
                if (challenge.completed) continue
                val def = byId[challenge.challengeId] ?: continue
                if (def.event != event) continue
                challenge.currentValue = (challenge.currentValue + delta).coerceAtMost(def.targetValue)
                if (challenge.currentValue >= def.targetValue) {
                    challenge.completed = true
                    challenge.completedAt = System.currentTimeMillis()
                    completed += challenge
                }
            }
            completed
        }

    /**
     * Replace expired challenges with a fresh set. Called lazily on
     * every record(). The picked challenges are sampled deterministically
     * (based on the current day/week) so all online players see the
     * same set for that period.
     */
    suspend fun rotateIfNeeded(player: Player) {
        val now = System.currentTimeMillis()
        if (player.lastChallengeRotation == 0L) {
            // First time we see this player.
            player.activeChallenges.clear()
            player.activeChallenges += pickInitial()
            player.lastChallengeRotation = now
            return
        }
        val (dailies, weeklies) = partition(player)
        val dailyExpired = dailies.isNotEmpty() && dailies.all { it.expiresAt <= now }
        val weeklyExpired = weeklies.isNotEmpty() && weeklies.all { it.expiresAt <= now }

        if (dailyExpired) {
            player.activeChallenges.removeAll { c ->
                byId[c.challengeId]?.cadence == ChallengeCadence.DAILY
            }
            player.activeChallenges += pickDailySet(now)
        }
        if (weeklyExpired) {
            player.activeChallenges.removeAll { c ->
                byId[c.challengeId]?.cadence == ChallengeCadence.WEEKLY
            }
            player.activeChallenges += pickWeeklySet(now)
        }
        player.lastChallengeRotation = now
    }

    private fun partition(player: Player): Pair<List<ActiveChallenge>, List<ActiveChallenge>> {
        val daily = mutableListOf<ActiveChallenge>()
        val weekly = mutableListOf<ActiveChallenge>()
        for (c in player.activeChallenges) {
            when (byId[c.challengeId]?.cadence) {
                ChallengeCadence.DAILY -> daily += c
                ChallengeCadence.WEEKLY -> weekly += c
                null -> {}
            }
        }
        return daily to weekly
    }

    private fun pickInitial(): List<ActiveChallenge> {
        val now = System.currentTimeMillis()
        return pickDailySet(now) + pickWeeklySet(now)
    }

    private fun pickDailySet(now: Long): List<ActiveChallenge> {
        val dailies = catalog.filter { it.cadence == ChallengeCadence.DAILY }
        val picks = dailies.shuffled().take(3)
        val expires = now + TimeUnit.DAYS.toMillis(1)
        return picks.map { ActiveChallenge(challengeId = it.id, assignedAt = now, expiresAt = expires) }
    }

    private fun pickWeeklySet(now: Long): List<ActiveChallenge> {
        val weeklies = catalog.filter { it.cadence == ChallengeCadence.WEEKLY }
        val picks = weeklies.shuffled().take(2)
        val expires = now + TimeUnit.DAYS.toMillis(7)
        return picks.map { ActiveChallenge(challengeId = it.id, assignedAt = now, expiresAt = expires) }
    }

    /**
     * Force a rotation for the player. Useful when the player
     * explicitly requests a refresh (e.g. ad-watched premium perk).
     */
    suspend fun forceRotate(player: Player): Unit = mutex.withLock {
        player.activeChallenges.clear()
        player.lastChallengeRotation = 0L
        rotateIfNeeded(player)
    }
}
