package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.reqStr
import com.zeroday.model.AchievementEvent
import com.zeroday.model.NotificationType
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import com.zeroday.service.AchievementService
import com.zeroday.service.NotificationService
import kotlinx.serialization.json.JsonObject

class AchievementHandler(
    private val achievementService: AchievementService,
    private val notificationService: NotificationService
) : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.GET_ACHIEVEMENTS,
        RequestTypes.CLAIM_ACHIEVEMENT
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.GET_ACHIEVEMENTS -> list(ctx, playerId)
            RequestTypes.CLAIM_ACHIEVEMENT -> claim(ctx, playerId, payload)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported achievement request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun list(ctx: HandlerContext, playerId: String): HandlerResult {
        val player = ctx.services.playerService.getPlayer(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        val visible = achievementService.visibleTo(player)
        val progressFor = player.achievementProgress
        val rows = visible.map { def ->
            val progress = progressFor[def.id]
            val percent = progress?.percent(def) ?: 0
            val unlockedAt = progress?.completedAt
            val claimed = player.unlockedAchievements.contains(def.id)
            mapOf(
                "id" to def.id,
                "name" to def.name,
                "description" to def.description,
                "category" to def.category.name,
                "target" to def.targetValue,
                "current" to (progress?.currentValue ?: 0L),
                "percent" to percent,
                "completed" to (progress?.completed == true),
                "unlocked_at" to unlockedAt,
                "reward_credits" to def.rewardCredits,
                "reward_reputation" to def.rewardReputation,
                "reward_xp" to def.rewardXp,
                "claimed" to claimed
            )
        }
        return HandlerResult.Ok(
            type = ResponseTypes.ACHIEVEMENTS,
            payload = mapOf(
                "achievements" to rows,
                "summary" to achievementService.summaryFor(player),
                "unread_achievements" to (player.unlockedAchievements.size - player.achievementProgress.count { it.value.completed })
            )
        )
    }

    private suspend fun claim(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val achievementId = try { payload.reqStr("achievement_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Achievement ID required", ErrorCategory.VALIDATION))
        }
        val player = ctx.services.playerService.getPlayer(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        val def = achievementService.get(achievementId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Unknown achievement: $achievementId", ErrorCategory.NOT_FOUND))
        val progress = player.achievementProgress[achievementId]
        if (progress?.completed != true) {
            return HandlerResult.Err(ServerError.fromMessage("Achievement not yet completed", ErrorCategory.VALIDATION))
        }
        // Idempotency: re-claiming is a no-op so the client can retry.
        if (player.unlockedAchievements.contains(achievementId)) {
            return HandlerResult.Ok(
                type = ResponseTypes.ACHIEVEMENT_UNLOCKED,
                payload = mapOf(
                    "achievement_id" to achievementId,
                    "name" to def.name,
                    "rewards" to mapOf("credits" to 0L, "reputation" to 0, "xp" to 0L),
                    "already_claimed" to true
                )
            )
        }
        player.unlockedAchievements.add(achievementId)
        ctx.services.playerService.addCredits(playerId, def.rewardCredits)
        ctx.services.playerService.addReputation(playerId, def.rewardReputation)
        ctx.services.playerService.addExperience(playerId, def.rewardXp)
        notificationService.push(
            player = player,
            type = NotificationType.ACHIEVEMENT_UNLOCKED,
            title = "Achievement unlocked: ${def.name}",
            message = def.description,
            data = mapOf("achievement_id" to def.id)
        )
        return HandlerResult.Ok(
            type = ResponseTypes.ACHIEVEMENT_UNLOCKED,
            payload = mapOf(
                "achievement_id" to achievementId,
                "name" to def.name,
                "rewards" to mapOf(
                    "credits" to def.rewardCredits,
                    "reputation" to def.rewardReputation,
                    "xp" to def.rewardXp
                )
            )
        )
    }

    /**
     * Public helper so other services (e.g. WorldEventService) can
     * emit achievement events and have the player's progress updated
     * without going through a web socket message.
     */
    suspend fun emit(
        playerId: String,
        event: AchievementEvent,
        delta: Long = 1L,
        services: com.zeroday.handler.ServiceRegistry
    ): List<com.zeroday.model.AchievementUpdate> {
        val player = services.playerService.getPlayer(playerId) ?: return emptyList()
        val updates = achievementService.record(player, event, delta)
        for (update in updates.filter { it.completed }) {
            val def = achievementService.get(update.achievementId) ?: continue
            notificationService.push(
                player = player,
                type = NotificationType.ACHIEVEMENT_UNLOCKED,
                title = "Achievement unlocked: ${def.name}",
                message = def.description,
                data = mapOf("achievement_id" to def.id, "unclaimed" to "true")
            )
        }
        return updates
    }
}
