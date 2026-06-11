package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.str
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import kotlinx.serialization.json.JsonObject


class CareerHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.CAREER_CHOOSE,
        RequestTypes.CAREER_STATUS,
        RequestTypes.CAREER_ADD_HEAT,
        RequestTypes.CAREER_ARREST,
        RequestTypes.GET_HUNTER_STATUS
    )

    override val requiresAuth: Boolean = true

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId() ?: return HandlerResult.Err(
            ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION)
        )
        return when (type) {
            RequestTypes.CAREER_CHOOSE -> chooseCareer(ctx, playerId, payload)
            RequestTypes.CAREER_STATUS -> getCareerStatus(ctx, playerId)
            RequestTypes.CAREER_ADD_HEAT -> addHeat(ctx, playerId, payload)
            RequestTypes.CAREER_ARREST -> arrest(ctx, playerId, payload)
            RequestTypes.GET_HUNTER_STATUS -> hunterStatus(ctx, playerId)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported career request", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun chooseCareer(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val career = payload.str("career")?.lowercase() ?: return HandlerResult.Err(
            ServerError.fromMessage("Career type required (whitehat/blackhat)", ErrorCategory.VALIDATION)
        )
        val result = ctx.services.careerService.chooseCareer(playerId, career)
        return if (result.success) {
            HandlerResult.Ok(ResponseTypes.CAREER_CHOSEN, mapOf(
                "career" to career,
                "message" to result.message,
                "justiceDelta" to result.justiceDelta,
                "heatDelta" to result.heatDelta
            ))
        } else {
            HandlerResult.Err(ServerError.fromMessage(result.message, ErrorCategory.VALIDATION))
        }
    }

    private suspend fun getCareerStatus(ctx: HandlerContext, playerId: String): HandlerResult {
        val report = ctx.services.careerService.getCareerReport(playerId)
        return HandlerResult.Ok(ResponseTypes.CAREER_STATUS, report)
    }

    private suspend fun addHeat(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val amount = payload["amount"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() }
            ?: return HandlerResult.Err(ServerError.fromMessage("Amount required", ErrorCategory.VALIDATION))
        val reason = payload.str("reason") ?: ""
        val result = ctx.services.careerService.addHeat(playerId, amount, reason)
        return if (result.success) {
            HandlerResult.Ok(ResponseTypes.HEAT_UPDATED, mapOf(
                "heatDelta" to amount,
                "message" to result.message
            ))
        } else {
            HandlerResult.Err(ServerError.fromMessage(result.message, ErrorCategory.VALIDATION))
        }
    }

    private suspend fun hunterStatus(ctx: HandlerContext, playerId: String): HandlerResult {
        val hunters = ctx.services.heatCascadeService.getActiveHunters()
        val isHunted = ctx.services.heatCascadeService.isPlayerHunted(playerId)
        val relevantHunters = hunters.filter { it.targetId == playerId }
        return HandlerResult.Ok(ResponseTypes.HUNTER_STATUS, mapOf(
            "hunters" to relevantHunters.map { mapOf(
                "hunterId" to it.hunterId,
                "targetName" to it.targetName,
                "targetHeat" to it.targetHeat,
                "attemptCount" to it.attemptCount
            )},
            "isHunted" to isHunted
        ))
    }

    private suspend fun arrest(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val targetId = payload.str("targetId")
            ?: return HandlerResult.Err(ServerError.fromMessage("Target player ID required", ErrorCategory.VALIDATION))
        val result = ctx.services.careerService.attemptArrest(playerId, targetId)
        return if (result.success) {
            HandlerResult.Ok(ResponseTypes.ARREST_SUCCESS, mapOf(
                "message" to result.message,
                "justiceDelta" to result.justiceDelta
            ))
        } else {
            HandlerResult.Err(ServerError.fromMessage(result.message, ErrorCategory.VALIDATION))
        }
    }
}
