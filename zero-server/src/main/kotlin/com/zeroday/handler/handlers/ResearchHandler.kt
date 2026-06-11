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

class ResearchHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.RESEARCH_RECIPES,
        RequestTypes.RESEARCH_INVENTORY,
        RequestTypes.RESEARCH_GATHER,
        RequestTypes.RESEARCH_START,
        RequestTypes.RESEARCH_CLAIM,
        RequestTypes.RESEARCH_USE_ITEM,
        RequestTypes.RESEARCH_STATUS
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.RESEARCH_RECIPES -> recipes(ctx, playerId)
            RequestTypes.RESEARCH_INVENTORY -> inventory(ctx, playerId)
            RequestTypes.RESEARCH_GATHER -> gather(ctx, playerId)
            RequestTypes.RESEARCH_START -> start(ctx, playerId, payload)
            RequestTypes.RESEARCH_CLAIM -> claim(ctx, playerId, payload)
            RequestTypes.RESEARCH_USE_ITEM -> useItem(ctx, playerId, payload)
            RequestTypes.RESEARCH_STATUS -> status(ctx, playerId)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported research request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun recipes(ctx: HandlerContext, playerId: String): HandlerResult {
        val recipes = ctx.services.researchService.getRecipes(playerId)
        return HandlerResult.Ok(ResponseTypes.RESEARCH_RECIPES, mapOf("recipes" to recipes))
    }

    private suspend fun inventory(ctx: HandlerContext, playerId: String): HandlerResult =
        ctx.services.researchService.getInventory(playerId).fold(
            onSuccess = { items -> HandlerResult.Ok(ResponseTypes.RESEARCH_INVENTORY, mapOf("inventory" to items)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Inventory unavailable", ErrorCategory.INTERNAL)) }
        )

    private suspend fun gather(ctx: HandlerContext, playerId: String): HandlerResult =
        ctx.services.researchService.gatherFragment(playerId).fold(
            onSuccess = { item -> HandlerResult.Ok(ResponseTypes.FRAGMENT_GATHERED, mapOf("item" to item)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Gather failed", ErrorCategory.INTERNAL)) }
        )

    private suspend fun start(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val recipeId = try { payload.reqStr("recipe_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Recipe ID required", ErrorCategory.VALIDATION))
        }
        return ctx.services.researchService.startResearch(playerId, recipeId).fold(
            onSuccess = { progress -> HandlerResult.Ok(ResponseTypes.RESEARCH_STARTED, mapOf("progress" to progress)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Research start failed", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun claim(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val recipeId = try { payload.reqStr("recipe_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Recipe ID required", ErrorCategory.VALIDATION))
        }
        return ctx.services.researchService.claimResearch(playerId, recipeId).fold(
            onSuccess = { item -> HandlerResult.Ok(ResponseTypes.RESEARCH_CLAIMED, mapOf("item" to item)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Research claim failed", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun useItem(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val itemId = try { payload.reqStr("item_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Item ID required", ErrorCategory.VALIDATION))
        }
        return ctx.services.researchService.useItem(playerId, itemId).fold(
            onSuccess = { msg -> HandlerResult.Ok(ResponseTypes.ITEM_USED, mapOf("message" to msg)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Use failed", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun status(ctx: HandlerContext, playerId: String): HandlerResult {
        val status = ctx.services.researchService.getResearchStatus(playerId)
        return HandlerResult.Ok(ResponseTypes.RESEARCH_STATUS, mapOf("active_research" to status))
    }
}
