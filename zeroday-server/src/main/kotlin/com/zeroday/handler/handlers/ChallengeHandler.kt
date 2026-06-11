package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.reqStr
import com.zeroday.model.NotificationType
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import com.zeroday.service.ChallengeService
import com.zeroday.service.NotificationService
import kotlinx.serialization.json.JsonObject

class ChallengeHandler(
    private val challengeService: ChallengeService,
    private val notificationService: NotificationService
) : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.GET_CHALLENGES,
        RequestTypes.CLAIM_CHALLENGE,
        RequestTypes.ROTATE_CHALLENGES
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.GET_CHALLENGES -> list(ctx, playerId)
            RequestTypes.CLAIM_CHALLENGE -> claim(ctx, playerId, payload)
            RequestTypes.ROTATE_CHALLENGES -> rotate(ctx, playerId)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported challenge request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun list(ctx: HandlerContext, playerId: String): HandlerResult {
        val player = ctx.services.playerService.getPlayer(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        val now = System.currentTimeMillis()
        val rows = player.activeChallenges.map { c ->
            val def = challengeService.get(c.challengeId) ?: return@map mapOf("id" to c.challengeId)
            mapOf(
                "id" to def.id,
                "name" to def.name,
                "description" to def.description,
                "cadence" to def.cadence.name,
                "target" to def.targetValue,
                "current" to c.currentValue,
                "percent" to c.percent(def.targetValue),
                "completed" to c.completed,
                "claimed" to c.claimed,
                "expires_at" to c.expiresAt,
                "expires_in_ms" to (c.expiresAt - now).coerceAtLeast(0L),
                "rewards" to mapOf(
                    "credits" to def.rewardCredits,
                    "xp" to def.rewardXp,
                    "reputation" to def.rewardReputation
                )
            )
        }
        return HandlerResult.Ok(
            type = ResponseTypes.CHALLENGES,
            payload = mapOf(
                "challenges" to rows,
                "last_rotation" to player.lastChallengeRotation
            )
        )
    }

    private suspend fun claim(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val challengeId = try { payload.reqStr("challenge_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Challenge ID required", ErrorCategory.VALIDATION))
        }
        val player = ctx.services.playerService.getPlayer(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        val active = player.activeChallenges.firstOrNull { it.challengeId == challengeId }
            ?: return HandlerResult.Err(ServerError.fromMessage("Unknown active challenge: $challengeId", ErrorCategory.NOT_FOUND))
        if (!active.completed) {
            return HandlerResult.Err(ServerError.fromMessage("Challenge not yet completed", ErrorCategory.VALIDATION))
        }
        if (active.claimed) {
            return HandlerResult.Ok(
                type = ResponseTypes.CHALLENGE_COMPLETED,
                payload = mapOf("challenge_id" to challengeId, "already_claimed" to true, "rewards" to emptyMap<String, Any>())
            )
        }
        val def = challengeService.get(challengeId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Unknown challenge definition: $challengeId", ErrorCategory.NOT_FOUND))
        active.claimed = true
        ctx.services.playerService.addCredits(playerId, def.rewardCredits)
        ctx.services.playerService.addReputation(playerId, def.rewardReputation)
        ctx.services.playerService.addExperience(playerId, def.rewardXp)
        notificationService.push(
            player = player,
            type = NotificationType.REWARD_GRANTED,
            title = "Challenge complete: ${def.name}",
            message = "You earned ${def.rewardCredits} credits and ${def.rewardXp} XP.",
            data = mapOf("challenge_id" to def.id, "credits" to def.rewardCredits.toString())
        )
        return HandlerResult.Ok(
            type = ResponseTypes.CHALLENGE_COMPLETED,
            payload = mapOf(
                "challenge_id" to challengeId,
                "rewards" to mapOf(
                    "credits" to def.rewardCredits,
                    "xp" to def.rewardXp,
                    "reputation" to def.rewardReputation
                )
            )
        )
    }

    private suspend fun rotate(ctx: HandlerContext, playerId: String): HandlerResult {
        val player = ctx.services.playerService.getPlayer(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        challengeService.forceRotate(player)
        return HandlerResult.Ok(
            type = ResponseTypes.CHALLENGE_ROTATED,
            payload = mapOf("challenges" to player.activeChallenges.map { it.challengeId })
        )
    }
}
