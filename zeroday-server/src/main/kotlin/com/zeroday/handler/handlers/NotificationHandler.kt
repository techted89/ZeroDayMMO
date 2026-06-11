package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.reqStr
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import com.zeroday.service.NotificationService
import kotlinx.serialization.json.JsonObject

class NotificationHandler(
    private val notificationService: NotificationService
) : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.GET_NOTIFICATIONS,
        RequestTypes.MARK_NOTIFICATION_READ,
        RequestTypes.MARK_ALL_NOTIFICATIONS_READ
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.GET_NOTIFICATIONS -> list(ctx, playerId, payload)
            RequestTypes.MARK_NOTIFICATION_READ -> markRead(ctx, playerId, payload)
            RequestTypes.MARK_ALL_NOTIFICATIONS_READ -> markAllRead(ctx, playerId)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported notification request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun list(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val player = ctx.services.playerService.getPlayer(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        val includeRead = payload["include_read"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } != "false"
        val limit = payload["limit"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() } ?: 50
        val items = notificationService.list(player, includeRead, limit).map { it.toMap() }
        return HandlerResult.Ok(
            type = ResponseTypes.NOTIFICATIONS,
            payload = mapOf(
                "notifications" to items,
                "unread_count" to notificationService.unreadCount(player)
            )
        )
    }

    private suspend fun markRead(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val notifId = try { payload.reqStr("notification_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Notification ID required", ErrorCategory.VALIDATION))
        }
        val player = ctx.services.playerService.getPlayer(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        val ok = notificationService.markRead(player, notifId)
        if (!ok) return HandlerResult.Err(ServerError.fromMessage("Unknown notification: $notifId", ErrorCategory.NOT_FOUND))
        return HandlerResult.Ok(
            type = ResponseTypes.NOTIFICATIONS,
            payload = mapOf("notification_id" to notifId, "unread_count" to notificationService.unreadCount(player))
        )
    }

    private suspend fun markAllRead(ctx: HandlerContext, playerId: String): HandlerResult {
        val player = ctx.services.playerService.getPlayer(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        val n = notificationService.markAllRead(player)
        return HandlerResult.Ok(
            type = ResponseTypes.NOTIFICATIONS,
            payload = mapOf("marked_read" to n, "unread_count" to 0)
        )
    }
}

internal fun com.zeroday.model.Notification.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "type" to type.name,
    "title" to title,
    "message" to message,
    "created_at" to createdAt,
    "read" to read,
    "data" to data
)
