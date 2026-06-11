package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.reqStr
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import kotlinx.serialization.json.JsonObject

/**
 * Routes story / narrative progression messages to the event service.
 */
class StorylineHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.GET_STORYLINES,
        RequestTypes.START_STORYLINE,
        RequestTypes.ADVANCE_STORY
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.GET_STORYLINES -> getStorylines(ctx, playerId)
            RequestTypes.START_STORYLINE -> startStoryline(ctx, playerId, payload)
            RequestTypes.ADVANCE_STORY -> advance(ctx, playerId)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported storyline request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun getStorylines(ctx: HandlerContext, playerId: String): HandlerResult {
        val storylines = ctx.services.eventService.getAvailableStorylines(playerId)
        val current = ctx.services.eventService.getCurrentStoryline(playerId)
        val stage = ctx.services.eventService.getCurrentStage(playerId)
        return HandlerResult.Ok(
            type = ResponseTypes.STORYLINES,
            payload = mapOf(
                "available" to storylines.toList(),
                "current" to current,
                "current_stage" to stage
            )
        )
    }

    private suspend fun startStoryline(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val storylineId = try { payload.reqStr("storyline_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Storyline ID required", ErrorCategory.VALIDATION))
        }
        return ctx.services.eventService.startStoryline(playerId, storylineId).fold(
            onSuccess = { story ->
                HandlerResult.Ok(
                    type = ResponseTypes.STORYLINE_STARTED,
                    payload = mapOf(
                        "storyline" to story,
                        "current_stage" to ctx.services.eventService.getCurrentStage(playerId)
                    )
                )
            },
            onFailure = { error ->
                HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not start storyline", ErrorCategory.INTERNAL))
            }
        )
    }

    private suspend fun advance(ctx: HandlerContext, playerId: String): HandlerResult =
        ctx.services.eventService.advanceStoryStage(playerId).fold(
            onSuccess = { story ->
                HandlerResult.Ok(
                    type = ResponseTypes.STORY_ADVANCED,
                    payload = mapOf(
                        "storyline" to story,
                        "current_stage" to ctx.services.eventService.getCurrentStage(playerId)
                    )
                )
            },
            onFailure = { error ->
                HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not advance story", ErrorCategory.INTERNAL))
            }
        )
}
