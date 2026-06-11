package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import com.zeroday.service.AdRewardService
import kotlinx.serialization.json.JsonObject

/**
 * Ad-reward requests. The actual cooldown and reward accounting lives in
 * [com.zeroday.service.AdRewardService]; this handler is just the
 * protocol-level wrapper.
 */
class AdHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(RequestTypes.WATCH_AD)

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (val result = ctx.services.adRewardService.processAdReward(playerId)) {
            is AdRewardService.AdRewardResult.SUCCESS -> HandlerResult.Ok(
                type = ResponseTypes.AD_REWARD,
                payload = mapOf(
                    "rewards" to result.rewards,
                    "message" to "Ad watched successfully! You earned rewards."
                )
            )
            is AdRewardService.AdRewardResult.ON_COOLDOWN -> HandlerResult.Ok(
                type = ResponseTypes.AD_COOLDOWN,
                payload = mapOf(
                    "minutesRemaining" to result.minutesRemaining,
                    "message" to "Ad available in ${result.minutesRemaining} minute(s)"
                )
            )
            is AdRewardService.AdRewardResult.FAILED -> HandlerResult.Err(
                ServerError.fromMessage(result.message, ErrorCategory.INTERNAL)
            )
        }
    }
}
