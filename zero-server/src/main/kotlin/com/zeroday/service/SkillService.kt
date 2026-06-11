package com.zeroday.service

import com.zeroday.model.Player
import com.zeroday.model.SkillDefinition
import com.zeroday.model.SkillEffect
import com.zeroday.model.SkillMultipliers
import com.zeroday.model.SkillTree

/**
 * Skill tree and skill-point economy.
 *
 * Skills live in four [SkillTree]s (offense, defense, intel, income)
 * and form a directed acyclic graph by [SkillDefinition.prerequisites].
 * Players earn skill points as they level up and spend them here.
 *
 * The actual numerical effects are stored once on the player (see
 * [effectiveMultipliers]); other services consult that single value
 * rather than walking the tree every time.
 */
class SkillService {
    val trees: List<SkillDefinition> = listOf(
        // Offense
        SkillDefinition(
            id = "offense_t1_fast_scan",
            tree = SkillTree.OFFENSE,
            tier = 1,
            name = "Fast Scan",
            description = "Reduce CPU cost of `nmap` and `scan` by 15%.",
            cost = 1,
            effects = listOf(SkillEffect.ReduceResourceCost(0.85))
        ),
        SkillDefinition(
            id = "offense_t2_crit_exploit",
            tree = SkillTree.OFFENSE,
            tier = 2,
            name = "Critical Exploit",
            description = "+25% XP from successful exploits.",
            cost = 2,
            prerequisites = listOf("offense_t1_fast_scan"),
            effects = listOf(SkillEffect.MultiplyXpEarned(1.25))
        ),
        SkillDefinition(
            id = "offense_t3_master_hacker",
            tree = SkillTree.OFFENSE,
            tier = 3,
            name = "Master Hacker",
            description = "+50% credits from tasks; +10 CPU cap.",
            cost = 3,
            prerequisites = listOf("offense_t2_crit_exploit"),
            effects = listOf(
                SkillEffect.MultiplyCreditsEarned(1.5),
                SkillEffect.AddMaxResource(cpu = 10)
            )
        ),
        // Defense
        SkillDefinition(
            id = "defense_t1_thick_firewall",
            tree = SkillTree.DEFENSE,
            tier = 1,
            name = "Thick Firewall",
            description = "+20 RAM and +10 Bandwidth caps.",
            cost = 1,
            effects = listOf(SkillEffect.AddMaxResource(ram = 20, bandwidth = 10))
        ),
        SkillDefinition(
            id = "defense_t2_quick_repair",
            tree = SkillTree.DEFENSE,
            tier = 2,
            name = "Quick Repair",
            description = "Resource regeneration 50% faster.",
            cost = 2,
            prerequisites = listOf("defense_t1_thick_firewall"),
            effects = listOf(SkillEffect.IncreaseRegenRate(1.5))
        ),
        SkillDefinition(
            id = "defense_t3_stealth_ops",
            tree = SkillTree.DEFENSE,
            tier = 3,
            name = "Stealth Ops",
            description = "All command resource costs reduced by 20%; +20 CPU cap.",
            cost = 3,
            prerequisites = listOf("defense_t2_quick_repair"),
            effects = listOf(
                SkillEffect.ReduceResourceCost(0.80),
                SkillEffect.AddMaxResource(cpu = 20)
            )
        ),
        // Intel
        SkillDefinition(
            id = "intel_t1_scholar",
            tree = SkillTree.INTEL,
            tier = 1,
            name = "Scholar",
            description = "+10% XP from research.",
            cost = 1,
            effects = listOf(SkillEffect.MultiplyXpEarned(1.10))
        ),
        SkillDefinition(
            id = "intel_t2_linguist",
            tree = SkillTree.INTEL,
            tier = 2,
            name = "Linguist",
            description = "+20% XP from research and +1 skill point on level-up.",
            cost = 2,
            prerequisites = listOf("intel_t1_scholar"),
            effects = listOf(SkillEffect.MultiplyXpEarned(1.20))
        ),
        SkillDefinition(
            id = "intel_t3_oracle",
            tree = SkillTree.INTEL,
            tier = 3,
            name = "Oracle",
            description = "+50% credits from research, +20 RAM cap.",
            cost = 3,
            prerequisites = listOf("intel_t2_linguist"),
            effects = listOf(
                SkillEffect.MultiplyCreditsEarned(1.5),
                SkillEffect.AddMaxResource(ram = 20)
            )
        ),
        // Income
        SkillDefinition(
            id = "income_t1_deal_maker",
            tree = SkillTree.INCOME,
            tier = 1,
            name = "Deal Maker",
            description = "+15% credits from tasks and ad rewards.",
            cost = 1,
            effects = listOf(SkillEffect.MultiplyCreditsEarned(1.15))
        ),
        SkillDefinition(
            id = "income_t2_investor",
            tree = SkillTree.INCOME,
            tier = 2,
            name = "Investor",
            description = "+30% credits from tasks; +10 Bandwidth cap.",
            cost = 2,
            prerequisites = listOf("income_t1_deal_maker"),
            effects = listOf(
                SkillEffect.MultiplyCreditsEarned(1.30),
                SkillEffect.AddMaxResource(bandwidth = 10)
            )
        ),
        SkillDefinition(
            id = "income_t3_tycoon",
            tree = SkillTree.INCOME,
            tier = 3,
            name = "Tycoon",
            description = "+75% credits; +20 CPU, +20 RAM, +20 Bandwidth caps.",
            cost = 3,
            prerequisites = listOf("income_t2_investor"),
            effects = listOf(
                SkillEffect.MultiplyCreditsEarned(1.75),
                SkillEffect.AddMaxResource(cpu = 20, ram = 20, bandwidth = 20)
            )
        )
    )

    private val byId: Map<String, SkillDefinition> = trees.associateBy { it.id }

    fun get(id: String): SkillDefinition? = byId[id]

    fun byTree(tree: SkillTree): List<SkillDefinition> = trees.filter { it.tree == tree }

    /**
     * Result of an attempt to unlock a skill. [UnlockResult.AlreadyOwned]
     * and [UnlockResult.MissingPrereqs] both carry the missing ids so the
     * client can show a useful error.
     */
    sealed class UnlockResult {
        data class Success(val skill: SkillDefinition) : UnlockResult()
        data class AlreadyOwned(val skill: SkillDefinition) : UnlockResult()
        data class InsufficientPoints(val skill: SkillDefinition, val cost: Int, val available: Int) : UnlockResult()
        data class MissingPrereqs(val skill: SkillDefinition, val missing: List<String>) : UnlockResult()
        data class Unknown(val id: String) : UnlockResult()
    }

    fun unlock(player: Player, skillId: String): UnlockResult {
        val def = byId[skillId] ?: return UnlockResult.Unknown(skillId)
        if (player.unlockedSkills.contains(skillId)) return UnlockResult.AlreadyOwned(def)
        if (player.skillPoints < def.cost) {
            return UnlockResult.InsufficientPoints(def, def.cost, player.skillPoints)
        }
        val missing = def.prerequisites.filterNot { player.unlockedSkills.contains(it) }
        if (missing.isNotEmpty()) return UnlockResult.MissingPrereqs(def, missing)
        player.skillPoints -= def.cost
        player.unlockedSkills.add(skillId)
        // Apply the additive resource bonuses immediately so the player sees
        // the benefit right away; multiplicative multipliers are computed
        // on the fly by [effectiveMultipliers].
        for (effect in def.effects) {
            if (effect is SkillEffect.AddMaxResource) {
                player.maxCpu += effect.cpu
                player.cpu += effect.cpu
                player.maxRam += effect.ram
                player.ram += effect.ram
                player.maxBandwidth += effect.bandwidth
                player.bandwidth += effect.bandwidth
            }
        }
        return UnlockResult.Success(def)
    }

    fun grantPoints(player: Player, points: Int) {
        player.skillPoints += points
    }

    /**
     * Compute the [SkillMultipliers] the player currently has by folding
     * over the effects of their unlocked skills. Multiplicative effects
     * compose; additive effects (resource caps) are applied at unlock
     * time and don't appear here.
     */
    fun effectiveMultipliers(player: Player): SkillMultipliers {
        var m = SkillMultipliers()
        for (id in player.unlockedSkills) {
            val def = byId[id] ?: continue
            for (effect in def.effects) {
                m = when (effect) {
                    is SkillEffect.MultiplyCreditsEarned -> m.copy(creditsEarned = m.creditsEarned * effect.factor)
                    is SkillEffect.MultiplyXpEarned -> m.copy(xpEarned = m.xpEarned * effect.factor)
                    is SkillEffect.ReduceResourceCost -> m.copy(resourceCost = m.resourceCost * effect.factor)
                    is SkillEffect.IncreaseRegenRate -> m.copy(regenRate = m.regenRate * effect.factor)
                    is SkillEffect.AddMaxResource -> m // applied at unlock time
                }
            }
        }
        return m
    }
}
