package com.zeroday.model

import kotlinx.serialization.Serializable

/**
 * Static definition of a daily/weekly challenge. Challenges have a
 * cadence (`Daily` or `Weekly`) and a target metric the player has to
 * hit before they expire.
 */
@Serializable
data class ChallengeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val cadence: ChallengeCadence,
    val event: AchievementEvent,
    val targetValue: Long,
    val rewardCredits: Long = 0L,
    val rewardXp: Long = 0L,
    val rewardReputation: Int = 0
)

enum class ChallengeCadence {
    DAILY,
    WEEKLY
}

/**
 * The player's active instance of a challenge. The server creates a new
 * list of these on each rotation (24h for daily, 7d for weekly); the
 * client just renders them.
 */
@Serializable
data class ActiveChallenge(
    val challengeId: String,
    val assignedAt: Long,
    val expiresAt: Long,
    var currentValue: Long = 0L,
    var completed: Boolean = false,
    var completedAt: Long? = null,
    var claimed: Boolean = false
) {
    fun percent(target: Long): Int =
        if (target == 0L) 100
        else ((currentValue.toDouble() / target) * 100).toInt().coerceIn(0, 100)
}
