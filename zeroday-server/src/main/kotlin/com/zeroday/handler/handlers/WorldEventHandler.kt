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

class WorldEventHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.WORLD_EVENTS,
        RequestTypes.WORLD_EVENT_JOIN,
        RequestTypes.WORLD_EVENT_LEAVE
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.WORLD_EVENTS -> list(ctx)
            RequestTypes.WORLD_EVENT_JOIN -> join(ctx, playerId, payload)
            RequestTypes.WORLD_EVENT_LEAVE -> leave(ctx, playerId, payload)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported world event request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun list(ctx: HandlerContext): HandlerResult {
        val events = ctx.services.worldEventService.getActiveEvents()
        return HandlerResult.Ok(ResponseTypes.WORLD_EVENTS, mapOf("events" to events))
    }

    private suspend fun join(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val eventId = try { payload.reqStr("event_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Event ID required", ErrorCategory.VALIDATION))
        }
        return ctx.services.worldEventService.joinEvent(playerId, eventId).fold(
            onSuccess = { event -> HandlerResult.Ok(ResponseTypes.WORLD_EVENT_JOINED, mapOf("event" to event)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Join failed", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun leave(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val eventId = try { payload.reqStr("event_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Event ID required", ErrorCategory.VALIDATION))
        }
        return ctx.services.worldEventService.leaveEvent(playerId, eventId).fold(
            onSuccess = { HandlerResult.Ok(ResponseTypes.WORLD_EVENT_LEFT, mapOf("message" to "Left event")) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Leave failed", ErrorCategory.INTERNAL)) }
        )
    }
}
