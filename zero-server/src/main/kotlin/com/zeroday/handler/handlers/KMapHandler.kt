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
 * Knowledge-map endpoints. Players can view discovered fragments, see
 * available fragments to discover, and "discover" one (paying the
 * resource cost and unlocking the associated command or research hook).
 */
class KMapHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.KMAP_GET,
        RequestTypes.KMAP_DISCOVER,
        RequestTypes.KMAP_FRAGMENTS
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.KMAP_GET -> get(ctx, playerId)
            RequestTypes.KMAP_DISCOVER -> discover(ctx, playerId, payload)
            RequestTypes.KMAP_FRAGMENTS -> fragments(ctx, playerId)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported kmap request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun get(ctx: HandlerContext, playerId: String): HandlerResult {
        val kmap = ctx.services.hackToLearnService.getKnowledgeMap(playerId)
        return HandlerResult.Ok(ResponseTypes.KNOWLEDGE_MAP, mapOf("kmap" to kmap))
    }

    private suspend fun discover(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val fragmentId = try { payload.reqStr("fragment_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Fragment ID required", ErrorCategory.VALIDATION))
        }
        return ctx.services.hackToLearnService.discoverFragment(playerId, fragmentId).fold(
            onSuccess = { fragment ->
                HandlerResult.Ok(
                    type = ResponseTypes.FRAGMENT_DISCOVERED,
                    payload = mapOf(
                        "fragment" to fragment,
                        "message" to "Discovered: ${fragment.name}" +
                            (fragment.unlocksCommand?.let { " - Unlocked command: $it" } ?: "")
                    )
                )
            },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Discovery failed", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun fragments(ctx: HandlerContext, playerId: String): HandlerResult {
        val available = ctx.services.hackToLearnService.getAvailableFragments(playerId)
        return HandlerResult.Ok(ResponseTypes.AVAILABLE_FRAGMENTS, mapOf("fragments" to available))
    }
}
