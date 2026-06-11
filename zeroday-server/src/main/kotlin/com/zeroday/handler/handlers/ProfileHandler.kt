package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.str
import com.zeroday.model.SkillTree
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import com.zeroday.service.AchievementService
import com.zeroday.service.SkillService
import kotlinx.serialization.json.JsonObject

/**
 * Public profile for any player. The endpoint returns the same shape
 * for the requesting player and for any other username they ask about,
 * minus sensitive fields. The skill tree and achievement summary
 * provide the bulk of the social signal.
 */
class ProfileHandler(
    private val achievementService: AchievementService,
    private val skillService: SkillService
) : MessageHandler {
    override val handledTypes: Set<String> = setOf(RequestTypes.GET_PROFILE)

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val requester = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        // Either omit username (defaults to self) or pass `username` to look up another player.
        val target = payload.str("username")
        val player = if (target == null) {
            ctx.services.playerService.getPlayer(requester)
        } else {
            ctx.services.playerService.getPlayerByUsername(target)
        } ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))

        val multipliers = skillService.effectiveMultipliers(player)
        val achievementSummary = achievementService.summaryFor(player)
        val unlocked = achievementService.visibleTo(player)
            .filter { player.unlockedAchievements.contains(it.id) }
            .take(10)
            .map { mapOf("id" to it.id, "name" to it.name, "category" to it.category.name) }

        val skills = SkillTree.values().associateWith { tree ->
            skillService.byTree(tree).count { player.unlockedSkills.contains(it.id) }
        }

        val completionPercent = (player.unlockedAchievements.size.toDouble() / achievementService.definitions.size * 100).toInt()

        return HandlerResult.Ok(
            type = ResponseTypes.PROFILE,
            payload = mapOf(
                "id" to player.id,
                "username" to player.username,
                "level" to player.level,
                "experience" to player.experience,
                "reputation" to player.reputation,
                "prestige_level" to player.prestigeLevel,
                "prestige_points" to player.prestigePoints,
                "faction_id" to player.factionId,
                "is_online" to player.isOnline,
                "stats" to mapOf(
                    "nodes_discovered" to player.discoveredNodes.size,
                    "tasks_completed" to player.completedTasks.size,
                    "storylines_completed" to player.completedStorylines.size,
                    "commands_unlocked" to player.unlockedCommands.size,
                    "world_events_joined" to player.worldEventParticipation.size
                ),
                "achievements" to mapOf(
                    "summary" to achievementSummary,
                    "total_unlocked" to player.unlockedAchievements.size,
                    "completion_percent" to completionPercent,
                    "recent" to unlocked
                ),
                "skills" to mapOf(
                    "points" to player.skillPoints,
                    "unlocked_total" to player.unlockedSkills.size,
                    "by_tree" to skills.mapKeys { it.key.name }
                ),
                "multipliers" to mapOf(
                    "credits_earned" to multipliers.creditsEarned,
                    "xp_earned" to multipliers.xpEarned,
                    "resource_cost" to multipliers.resourceCost,
                    "regen_rate" to multipliers.regenRate
                )
            )
        )
    }
}
