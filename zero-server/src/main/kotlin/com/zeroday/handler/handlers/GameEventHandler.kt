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
import kotlinx.serialization.json.JsonPrimitive

class GameEventHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.EVENT_LIST, RequestTypes.EVENT_JOIN, RequestTypes.EVENT_LEAVE,
        RequestTypes.EVENT_LEADERBOARD, RequestTypes.EVENT_SCORE, RequestTypes.EVENT_MY_EVENTS
    )

    override val requiresAuth: Boolean = true

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.EVENT_LIST -> listEvents(ctx)
            RequestTypes.EVENT_JOIN -> joinEvent(ctx, playerId, payload)
            RequestTypes.EVENT_LEAVE -> leaveEvent(ctx, playerId, payload)
            RequestTypes.EVENT_LEADERBOARD -> leaderboard(ctx, payload)
            RequestTypes.EVENT_SCORE -> updateScore(ctx, playerId, payload)
            RequestTypes.EVENT_MY_EVENTS -> myEvents(ctx, playerId)
            else -> HandlerResult.Err(ServerError.fromMessage("Unknown event request", ErrorCategory.VALIDATION))
        }
    }

    private fun JsonObject.strOr(key: String, default: String = ""): String =
        (this[key] as? JsonPrimitive)?.content ?: default

    private fun JsonObject.longOr(key: String, default: Long = 0L): Long =
        (this[key] as? JsonPrimitive)?.content?.toLongOrNull() ?: default

    private suspend fun listEvents(ctx: HandlerContext): HandlerResult {
        val events = ctx.services.gameEventService.getActiveEvents()
        return HandlerResult.Ok(ResponseTypes.EVENT_LIST, mapOf(
            "events" to events.map { ctx.services.gameEventService.serializeEvent(it) },
            "count" to events.size
        ))
    }

    private suspend fun joinEvent(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val eventId = payload.str("eventId")
            ?: return HandlerResult.Err(ServerError.fromMessage("eventId required", ErrorCategory.VALIDATION))
        val result = ctx.services.gameEventService.joinEvent(eventId, playerId)
        return result.fold(
            onSuccess = { event -> HandlerResult.Ok(ResponseTypes.EVENT_JOINED, ctx.services.gameEventService.serializeEvent(event)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not join event", ErrorCategory.VALIDATION)) }
        )
    }

    private suspend fun leaveEvent(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val eventId = payload.str("eventId")
            ?: return HandlerResult.Err(ServerError.fromMessage("eventId required", ErrorCategory.VALIDATION))
        val result = ctx.services.gameEventService.leaveEvent(eventId, playerId)
        return result.fold(
            onSuccess = { HandlerResult.Ok(ResponseTypes.EVENT_LEFT, mapOf("eventId" to eventId)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not leave event", ErrorCategory.VALIDATION)) }
        )
    }

    private suspend fun leaderboard(ctx: HandlerContext, payload: JsonObject): HandlerResult {
        val eventId = payload.str("eventId")
            ?: return HandlerResult.Err(ServerError.fromMessage("eventId required", ErrorCategory.VALIDATION))
        val participants = ctx.services.gameEventService.getEventLeaderboard(eventId)
        return HandlerResult.Ok(ResponseTypes.EVENT_LEADERBOARD, mapOf(
            "eventId" to eventId,
            "participants" to participants.mapIndexed { idx, p -> mapOf(
                "playerName" to p.playerName,
                "score" to p.score,
                "rank" to (idx + 1)
            )}
        ))
    }

    private suspend fun updateScore(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val eventId = payload.str("eventId")
            ?: return HandlerResult.Err(ServerError.fromMessage("eventId required", ErrorCategory.VALIDATION))
        val delta = payload.longOr("delta")
        if (delta <= 0) return HandlerResult.Err(ServerError.fromMessage("delta must be positive", ErrorCategory.VALIDATION))

        val result = ctx.services.gameEventService.updateScore(eventId, playerId, delta)
        return result.fold(
            onSuccess = { HandlerResult.Ok(ResponseTypes.EVENT_SCORE_UPDATED, mapOf("eventId" to eventId, "playerId" to playerId, "delta" to delta)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Score update failed", ErrorCategory.VALIDATION)) }
        )
    }

    private suspend fun myEvents(ctx: HandlerContext, playerId: String): HandlerResult {
        val events = ctx.services.gameEventService.getPlayerEvents(playerId)
        return HandlerResult.Ok(ResponseTypes.EVENT_MY_EVENTS, mapOf(
            "events" to events.map { ctx.services.gameEventService.serializeEvent(it) },
            "count" to events.size
        ))
    }
}
