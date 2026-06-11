package com.zeroday.service

import com.zeroday.model.AchievementCategory
import com.zeroday.model.AchievementDefinition
import com.zeroday.model.AchievementEvent
import com.zeroday.model.AchievementProgress
import com.zeroday.model.AchievementRewards
import com.zeroday.model.AchievementUpdate
import com.zeroday.model.Player

/**
 * Static catalog + progress tracking for achievements.
 *
 * The service holds the immutable [definitions] list (data-driven; trivially
 * extensible) and exposes a single [record] entry point that other services
 * call when something interesting happens (e.g. network discovery, task
 * completion). [record] returns the list of [AchievementUpdate]s so the
 * caller can apply rewards and notify the player.
 *
 * The service is intentionally side-effect free: it does not mutate the
 * player's credits/rep/xp. That's the caller's job, so we keep the
 * achievement logic decoupled from the rest of the game economy.
 */
class AchievementService {
    val definitions: List<AchievementDefinition> = listOf(
        // Combat
        AchievementDefinition(
            id = "first_blood",
            name = "First Blood",
            description = "Compromise your first network node.",
            category = AchievementCategory.COMBAT,
            targetValue = 1,
            rewardCredits = 100,
            rewardXp = 50
        ),
        AchievementDefinition(
            id = "node_hunter",
            name = "Node Hunter",
            description = "Discover 25 unique network nodes.",
            category = AchievementCategory.COMBAT,
            targetValue = 25,
            rewardCredits = 500,
            rewardReputation = 5
        ),
        AchievementDefinition(
            id = "network_king",
            name = "Network King",
            description = "Discover 100 unique network nodes.",
            category = AchievementCategory.COMBAT,
            targetValue = 100,
            rewardCredits = 5000,
            rewardReputation = 25,
            rewardXp = 1000
        ),
        // Progression
        AchievementDefinition(
            id = "level_5",
            name = "Apprentice",
            description = "Reach level 5.",
            category = AchievementCategory.PROGRESSION,
            targetValue = 5,
            rewardCredits = 250,
            rewardXp = 100
        ),
        AchievementDefinition(
            id = "level_10",
            name = "Veteran",
            description = "Reach level 10.",
            category = AchievementCategory.PROGRESSION,
            targetValue = 10,
            rewardCredits = 1000,
            rewardReputation = 10,
            rewardXp = 500
        ),
        AchievementDefinition(
            id = "level_20",
            name = "Elite",
            description = "Reach level 20.",
            category = AchievementCategory.PROGRESSION,
            targetValue = 20,
            rewardCredits = 5000,
            rewardReputation = 50,
            rewardXp = 2500
        ),
        AchievementDefinition(
            id = "story_complete",
            name = "Storyteller",
            description = "Complete 7 storylines.",
            category = AchievementCategory.PROGRESSION,
            targetValue = 7,
            rewardCredits = 2000,
            rewardReputation = 15,
            rewardXp = 1000
        ),
        // Economy
        AchievementDefinition(
            id = "first_credits",
            name = "Money Moves",
            description = "Earn 1,000 credits in total.",
            category = AchievementCategory.ECONOMY,
            targetValue = 1_000,
            rewardCredits = 100
        ),
        AchievementDefinition(
            id = "rich",
            name = "Crypto Rich",
            description = "Earn 100,000 credits in total.",
            category = AchievementCategory.ECONOMY,
            targetValue = 100_000,
            rewardCredits = 5_000,
            rewardReputation = 20
        ),
        AchievementDefinition(
            id = "whale",
            name = "Whale",
            description = "Earn 1,000,000 credits in total.",
            category = AchievementCategory.ECONOMY,
            targetValue = 1_000_000,
            rewardCredits = 50_000,
            rewardReputation = 100,
            rewardXp = 5_000
        ),
        // Exploration
        AchievementDefinition(
            id = "scanner",
            name = "Scanner",
            description = "Run 50 scan commands.",
            category = AchievementCategory.EXPLORATION,
            targetValue = 50,
            rewardCredits = 200
        ),
        AchievementDefinition(
            id = "subnet_explorer",
            name = "Subnet Explorer",
            description = "Scan 20 unique subnets.",
            category = AchievementCategory.EXPLORATION,
            targetValue = 20,
            rewardCredits = 800
        ),
        // Social
        AchievementDefinition(
            id = "team_player",
            name = "Team Player",
            description = "Join a faction.",
            category = AchievementCategory.SOCIAL,
            targetValue = 1,
            rewardCredits = 500,
            rewardReputation = 10
        ),
        // Scripting
        AchievementDefinition(
            id = "script_kiddie",
            name = "Script Kiddie",
            description = "Save your first Nexus script.",
            category = AchievementCategory.SCRIPTING,
            targetValue = 1,
            rewardCredits = 250,
            rewardXp = 100
        ),
        AchievementDefinition(
            id = "fragment_finder",
            name = "Fragment Finder",
            description = "Discover 10 knowledge fragments.",
            category = AchievementCategory.SCRIPTING,
            targetValue = 10,
            rewardCredits = 1000,
            rewardReputation = 5,
            rewardXp = 250
        ),
        // World events
        AchievementDefinition(
            id = "event_joiner",
            name = "Joiner",
            description = "Participate in 5 world events.",
            category = AchievementCategory.WORLD_EVENT,
            targetValue = 5,
            rewardCredits = 1000,
            rewardReputation = 10
        ),
        AchievementDefinition(
            id = "event_regular",
            name = "World Shaper",
            description = "Participate in 25 world events.",
            category = AchievementCategory.WORLD_EVENT,
            targetValue = 25,
            rewardCredits = 5000,
            rewardReputation = 50
        ),
        // Tasks
        AchievementDefinition(
            id = "task_10",
            name = "Contractor",
            description = "Complete 10 tasks.",
            category = AchievementCategory.PROGRESSION,
            targetValue = 10,
            rewardCredits = 1000,
            rewardReputation = 5,
            rewardXp = 500
        ),
        AchievementDefinition(
            id = "task_100",
            name = "Pro Contractor",
            description = "Complete 100 tasks.",
            category = AchievementCategory.PROGRESSION,
            targetValue = 100,
            rewardCredits = 25_000,
            rewardReputation = 100,
            rewardXp = 5_000
        )
    )

    private val byId: Map<String, AchievementDefinition> = definitions.associateBy { it.id }

    fun get(id: String): AchievementDefinition? = byId[id]

    fun visibleTo(player: Player): List<AchievementDefinition> =
        definitions.filter { !it.hidden || player.unlockedAchievements.contains(it.id) }

    /**
     * Apply [delta] to the player's progress on all achievements that
     * listen for [event]. Returns the list of achievements that were
     * affected, including any that just completed (the caller is
     * expected to apply the rewards to the player and surface a
     * notification).
     */
    fun record(player: Player, event: AchievementEvent, delta: Long = 1L): List<AchievementUpdate> {
        val updates = mutableListOf<AchievementUpdate>()
        for (def in definitions) {
            if (!listensTo(def, event)) continue
            val progress = player.achievementProgress.getOrPut(def.id) {
                AchievementProgress(achievementId = def.id)
            }
            if (progress.completed) continue
            progress.currentValue = (progress.currentValue + delta).coerceAtMost(def.targetValue)
            if (progress.currentValue >= def.targetValue) {
                progress.completed = true
                progress.completedAt = System.currentTimeMillis()
                player.unlockedAchievements.add(def.id)
                updates += AchievementUpdate(
                    achievementId = def.id,
                    newValue = progress.currentValue,
                    completed = true,
                    rewardsAwarded = AchievementRewards(
                        credits = def.rewardCredits,
                        reputation = def.rewardReputation,
                        xp = def.rewardXp
                    )
                )
            } else {
                updates += AchievementUpdate(
                    achievementId = def.id,
                    newValue = progress.currentValue,
                    completed = false,
                    rewardsAwarded = AchievementRewards.NONE
                )
            }
        }
        return updates
    }

    private fun listensTo(def: AchievementDefinition, event: AchievementEvent): Boolean =
        when (event) {
            AchievementEvent.NODE_DISCOVERED -> def.id in setOf("node_hunter", "network_king", "first_blood")
            AchievementEvent.NETWORK_COMPROMISED -> def.id in setOf("first_blood", "node_hunter", "network_king")
            AchievementEvent.COMMAND_EXECUTED -> def.id in setOf("scanner", "subnet_explorer")
            AchievementEvent.TASK_COMPLETED -> def.id in setOf("task_10", "task_100")
            AchievementEvent.STORYLINE_COMPLETED -> def.id in setOf("story_complete")
            AchievementEvent.LEVEL_REACHED -> def.id in setOf("level_5", "level_10", "level_20")
            AchievementEvent.CREDITS_EARNED -> def.id in setOf("first_credits", "rich", "whale")
            AchievementEvent.CREDITS_SPENT -> false
            AchievementEvent.FRAGMENT_GATHERED -> def.id in setOf("fragment_finder")
            AchievementEvent.SCRIPT_SAVED -> def.id in setOf("script_kiddie")
            AchievementEvent.FACTION_JOINED -> def.id in setOf("team_player")
            AchievementEvent.WORLD_EVENT_PARTICIPATED -> def.id in setOf("event_joiner", "event_regular")
            AchievementEvent.COMMAND_UNLOCKED -> def.id in setOf("command_master")
            AchievementEvent.CAREER_CHOSEN -> false
            AchievementEvent.HEAT_CHANGED -> false
            AchievementEvent.JUSTICE_CHANGED -> false
            AchievementEvent.ARREST_EXECUTED -> false
            AchievementEvent.BOUNTY_PLACED -> false
            AchievementEvent.ITEM_CRAFTED -> false
        }

    fun summaryFor(player: Player): Map<String, Int> {
        val byCategory = player.unlockedAchievements
            .mapNotNull { byId[it] }
            .groupingBy { it.category }
            .eachCount()
        return AchievementCategory.values().associate { it.name to (byCategory[it] ?: 0) }
    }
}
