package com.zeroday.model

import kotlinx.serialization.Serializable

/**
 * The high-level bucket an achievement falls into. Used for the player's
 * profile breakdown and for "category completion" achievements.
 */
enum class AchievementCategory {
    COMBAT,        // successful exploits, networks compromised
    PROGRESSION,   // levels, storylines, rank
    ECONOMY,       // credits earned/spent
    EXPLORATION,   // nodes discovered, subnets scanned
    SOCIAL,        // faction, party
    SCRIPTING,     // nexus scripts, knowledge fragments
    WORLD_EVENT,   // participation in world events
    META           // meta-achievements (unlock every other achievement in a category)
}

/**
 * A static definition of an achievement: id, name, description, and the
 * criteria the player must satisfy to earn it. The runtime state lives on
 * the player as [AchievementProgress].
 */
@Serializable
data class AchievementDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    /** What the player has to accumulate. e.g. 10 nodes discovered. */
    val targetValue: Long,
    val rewardCredits: Long = 0L,
    val rewardReputation: Int = 0,
    val rewardXp: Long = 0L,
    val hidden: Boolean = false  // hidden until earned
)

/**
 * Tracks a player's progress against a single [AchievementDefinition].
 * Stored on [Player.achievementProgress] as a Map keyed by achievement id.
 */
@Serializable
data class AchievementProgress(
    val achievementId: String,
    var currentValue: Long = 0L,
    var completed: Boolean = false,
    var completedAt: Long? = null
) {
    fun percent(definition: AchievementDefinition): Int =
        if (definition.targetValue == 0L) 100
        else ((currentValue.toDouble() / definition.targetValue) * 100).toInt().coerceIn(0, 100)
}

/**
 * The kind of event a player's actions produce that achievements
 * listen for. Achievements subscribe to a subset of these event types.
 */
enum class AchievementEvent {
    NODE_DISCOVERED,
    NETWORK_COMPROMISED,
    COMMAND_EXECUTED,
    TASK_COMPLETED,
    STORYLINE_COMPLETED,
    LEVEL_REACHED,
    CREDITS_EARNED,
    CREDITS_SPENT,
    FRAGMENT_GATHERED,
    SCRIPT_SAVED,
    FACTION_JOINED,
    WORLD_EVENT_PARTICIPATED,
    COMMAND_UNLOCKED,
    CAREER_CHOSEN,
    HEAT_CHANGED,
    JUSTICE_CHANGED,
    ARREST_EXECUTED,
    BOUNTY_PLACED,
    ITEM_CRAFTED
}

/**
 * Result of awarding progress. Returned by [com.zeroday.service.AchievementService]
 * so callers (other services) know whether the event triggered an
 * unlock and can apply rewards.
 */
@Serializable
data class AchievementUpdate(
    val achievementId: String,
    val newValue: Long,
    val completed: Boolean,
    val rewardsAwarded: AchievementRewards
)

@Serializable
data class AchievementRewards(
    val credits: Long,
    val reputation: Int,
    val xp: Long
) {
    companion object {
        val NONE = AchievementRewards(0L, 0, 0L)
    }
}
