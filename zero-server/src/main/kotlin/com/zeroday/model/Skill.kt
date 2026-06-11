package com.zeroday.model

import kotlinx.serialization.Serializable

/**
 * A single node in the skill tree. Skills are organised by [tree];
 * within a tree they form a directed acyclic graph declared in
 * [SkillService.trees]. Each skill has a [cost] in skill points and
 * optional [effects] applied via [com.zeroday.service.SkillService]
 * to the player's [Player] state.
 */
@Serializable
data class SkillDefinition(
    val id: String,
    val tree: SkillTree,
    val tier: Int,                 // 1..N; higher tier requires more prerequisite skills
    val name: String,
    val description: String,
    val cost: Int,                 // skill-point cost to unlock
    val prerequisites: List<String> = emptyList(),
    val effects: List<SkillEffect> = emptyList()
)

enum class SkillTree {
    OFFENSE,    // faster exploits, higher command crit
    DEFENSE,    // better firewall, lower detection
    INTEL,      // faster research, cheaper scripts
    INCOME      // more credits from tasks, ad rewards
}

/**
 * Effects are applied at the moment a skill is unlocked. They are
 * declarations only — the live multipliers live on [Player] and are
 * computed by [com.zeroday.service.SkillService.effectiveMultipliers].
 */
@Serializable
sealed class SkillEffect {
    abstract val kind: String

    @Serializable
    data class MultiplyCreditsEarned(val factor: Double) : SkillEffect() {
        override val kind: String = "multiply_credits_earned"
    }

    @Serializable
    data class MultiplyXpEarned(val factor: Double) : SkillEffect() {
        override val kind: String = "multiply_xp_earned"
    }

    @Serializable
    data class AddMaxResource(val cpu: Int = 0, val ram: Int = 0, val bandwidth: Int = 0) : SkillEffect() {
        override val kind: String = "add_max_resource"
    }

    @Serializable
    data class ReduceResourceCost(val factor: Double) : SkillEffect() {
        override val kind: String = "reduce_resource_cost"
    }

    @Serializable
    data class IncreaseRegenRate(val factor: Double) : SkillEffect() {
        override val kind: String = "increase_regen_rate"
    }
}

/**
 * The aggregated multipliers the player gets from their unlocked skills.
 * Cached per-player and invalidated on unlock.
 */
@Serializable
data class SkillMultipliers(
    val creditsEarned: Double = 1.0,
    val xpEarned: Double = 1.0,
    val resourceCost: Double = 1.0,
    val regenRate: Double = 1.0,
    val bonusCpu: Int = 0,
    val bonusRam: Int = 0,
    val bonusBandwidth: Int = 0
) {
    fun combine(other: SkillMultipliers) = SkillMultipliers(
        creditsEarned = creditsEarned * other.creditsEarned,
        xpEarned = xpEarned * other.xpEarned,
        resourceCost = resourceCost * other.resourceCost,
        regenRate = regenRate * other.regenRate,
        bonusCpu = bonusCpu + other.bonusCpu,
        bonusRam = bonusRam + other.bonusRam,
        bonusBandwidth = bonusBandwidth + other.bonusBandwidth
    )
}
