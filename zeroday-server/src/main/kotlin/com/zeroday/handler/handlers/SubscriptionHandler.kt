package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Manages per-connection channel subscriptions.
 *
 * By default every connection subscribes to `player` and `notifications`;
 * all other channels are opt-in so the client doesn't receive frames it
 * has no use for. This is the single most effective bandwidth optimisation
 * in the server: an idle player who never touches inventory or tasks
 * doesn't receive inventory-sync frames every tick.
 *
 * The client sends:
 *   `{"type":"subscribe","payload":{"channels":["inventory","tasks"]}}`
 *   `{"type":"unsubscribe","payload":{"channels":["tasks"]}}`
 */
class SubscriptionHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.SUBSCRIBE,
        RequestTypes.UNSUBSCRIBE
    )
    override val requiresAuth: Boolean = true

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val rawChannels = payload["channels"]
        val channelNames: List<String> = when (rawChannels) {
            is JsonArray -> rawChannels.mapNotNull {
                (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
            }
            else -> return HandlerResult.Err(
                ServerError.fromMessage("'channels' must be an array of channel names", ErrorCategory.VALIDATION)
            )
        }
        if (channelNames.isEmpty()) {
            return HandlerResult.Err(
                ServerError.fromMessage("Empty channel list", ErrorCategory.VALIDATION)
            )
        }
        return when (type) {
            RequestTypes.SUBSCRIBE -> handleSubscribe(ctx, channelNames)
            RequestTypes.UNSUBSCRIBE -> handleUnsubscribe(ctx, channelNames)
            else -> HandlerResult.Err(
                ServerError.fromMessage("Unsupported subscription operation", ErrorCategory.VALIDATION)
            )
        }
    }

    private fun handleSubscribe(ctx: HandlerContext, channels: List<String>): HandlerResult {
        val conn = ctx.connection
        val added = mutableListOf<String>()
        val invalid = mutableListOf<String>()
        for (ch in channels) {
            if (ch in conn.subscribedChannels) continue
            if (ch !in com.zeroday.handler.ConnectionInfo.ALL_CHANNELS) {
                invalid += ch
                continue
            }
            conn.subscribedChannels.add(ch)
            added += ch
        }
        return HandlerResult.Ok(
            type = ResponseTypes.SUBSCRIBED,
            payload = mapOf<String, Any?>(
                "subscribed" to added,
                "invalid" to invalid.ifEmpty { null },
                "active" to conn.subscribedChannels.toList()
            )
        )
    }

    private fun handleUnsubscribe(ctx: HandlerContext, channels: List<String>): HandlerResult {
        val conn = ctx.connection
        val removed = mutableListOf<String>()
        val protected = mutableListOf<String>()
        for (ch in channels) {
            if (ch in com.zeroday.handler.ConnectionInfo.DEFAULT_CHANNELS) {
                protected += ch
                continue
            }
            if (conn.subscribedChannels.remove(ch)) removed += ch
        }
        return HandlerResult.Ok(
            type = ResponseTypes.UNSUBSCRIBED,
            payload = mapOf<String, Any?>(
                "unsubscribed" to removed,
                "protected" to protected.ifEmpty { null },
                "active" to conn.subscribedChannels.toList()
            )
        )
    }
}
