package com.zeroday.model

import kotlinx.serialization.Serializable

@Serializable
data class MissionModifier(
    val id: String,
    val name: String,
    val description: String,
    val modifierType: ModifierType,
    val value: Double,
    val rarity: ModifierRarity,
    val durationMs: Long = 0, // 0 means permanent for mission
    val stacking: Boolean = false
)

@Serializable
enum class ModifierType {
    TIME_LIMIT,
    STEALTH_REQUIRED,
    NO_DETECTION,
    SPEED_RUN,
    GHOST_MODE,
    LIMITED_RESOURCES,
    BOOSTED_REWARDS,
    REDUCED_REWARDS,
    INCREASED_DIFFICULTY,
    DECREASED_DIFFICULTY,
    SPECIAL_OBJECTIVE,
    TEAM_REQUIRED,
    SOLO_ONLY
}

@Serializable
enum class ModifierRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

@Serializable
data class EnhancedContract(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val targetType: TaskTargetType,
    val targetIp: String,
    val requiredCommands: List<String> = emptyList(),
    val difficulty: TaskDifficulty,
    val suggestedLevel: Int = 1,
    val baseXp: Long = 0,
    val baseCredits: Long = 0,
    val modifiers: List<MissionModifier> = emptyList(),
    val finalRewardMultiplier: Double = 1.0,
    val finalXp: Long = 0,
    val finalCredits: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val timeLimitMs: Long = 600_000L
)

object MissionModifiers {
    val timeLimited = MissionModifier(
        "time_limited", "Time Limited", "Complete within reduced time",
        ModifierType.TIME_LIMIT, 0.7, ModifierRarity.UNCOMMON
    )
    
    val stealthRequired = MissionModifier(
        "stealth_required", "Stealth Required", "Avoid detection systems",
        ModifierType.STEALTH_REQUIRED, 1.0, ModifierRarity.RARE
    )
    
    val noDetection = MissionModifier(
        "no_detection", "Ghost Mode", "Leave no traces behind",
        ModifierType.NO_DETECTION, 1.0, ModifierRarity.EPIC
    )
    
    val speedRun = MissionModifier(
        "speed_run", "Speed Run", "2x rewards for completing under half time",
        ModifierType.SPEED_RUN, 2.0, ModifierRarity.RARE
    )
    
    val limitedResources = MissionModifier(
        "limited_resources", "Limited Resources", "Reduced starting resources",
        ModifierType.LIMITED_RESOURCES, 0.5, ModifierRarity.UNCOMMON
    )
    
    val boostedRewards = MissionModifier(
        "boosted_rewards", "Boosted Rewards", "Increased payout for risk",
        ModifierType.BOOSTED_REWARDS, 1.5, ModifierRarity.UNCOMMON
    )
    
    val reducedRewards = MissionModifier(
        "reduced_rewards", "Reduced Rewards", "Lower payout for ease",
        ModifierType.REDUCED_REWARDS, 0.7, ModifierRarity.COMMON
    )
    
    val increasedDifficulty = MissionModifier(
        "increased_difficulty", "Increased Difficulty", "Enhanced security measures",
        ModifierType.INCREASED_DIFFICULTY, 1.0, ModifierRarity.RARE
    )
    
    val decreasedDifficulty = MissionModifier(
        "decreased_difficulty", "Decreased Difficulty", "Weakened defenses",
        ModifierType.DECREASED_DIFFICULTY, 1.0, ModifierRarity.UNCOMMON
    )
    
    val specialObjective = MissionModifier(
        "special_objective", "Special Objective", "Additional objective required",
        ModifierType.SPECIAL_OBJECTIVE, 1.0, ModifierRarity.RARE
    )
    
    val teamRequired = MissionModifier(
        "team_required", "Team Required", "Must complete with 2+ players",
        ModifierType.TEAM_REQUIRED, 1.0, ModifierRarity.RARE
    )
    
    val soloOnly = MissionModifier(
        "solo_only", "Solo Only", "Must be completed alone",
        ModifierType.SOLO_ONLY, 1.0, ModifierRarity.UNCOMMON
    )
    
    val allModifiers = listOf(
        timeLimited, stealthRequired, noDetection, speedRun,
        limitedResources, boostedRewards, reducedRewards,
        increasedDifficulty, decreasedDifficulty, specialObjective,
        teamRequired, soloOnly
    )
    
    fun getRandomModifier(count: Int = 1): List<MissionModifier> {
        val available = allModifiers
        val takeCount = count.coerceAtMost(available.size)
        return (0 until takeCount)
            .map { available.random() }
            .distinct()
            .toList()
    }
}