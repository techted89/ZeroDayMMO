package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.long
import com.zeroday.handler.reqStr
import com.zeroday.handler.str
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class FactionHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.FACTION_CREATE,
        RequestTypes.FACTION_JOIN,
        RequestTypes.FACTION_LEAVE,
        RequestTypes.FACTION_INFO,
        RequestTypes.FACTION_LIST,
        RequestTypes.FACTION_DONATE,
        RequestTypes.FACTION_UPGRADE
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.FACTION_CREATE -> create(ctx, playerId, payload)
            RequestTypes.FACTION_JOIN -> join(ctx, playerId, payload)
            RequestTypes.FACTION_LEAVE -> leave(ctx, playerId)
            RequestTypes.FACTION_INFO -> info(ctx, playerId)
            RequestTypes.FACTION_LIST -> list(ctx)
            RequestTypes.FACTION_DONATE -> donate(ctx, playerId, payload)
            RequestTypes.FACTION_UPGRADE -> upgrade(ctx, playerId, payload)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported faction request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun create(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val name = payload.str("name")
            ?: return HandlerResult.Err(ServerError.fromMessage("Faction name required", ErrorCategory.VALIDATION))
        val tag = payload.str("tag")
            ?: return HandlerResult.Err(ServerError.fromMessage("Faction tag required", ErrorCategory.VALIDATION))
        val desc = payload.str("description") ?: ""
        return ctx.services.factionService.createFaction(playerId, name, tag, desc).fold(
            onSuccess = { faction -> HandlerResult.Ok(ResponseTypes.FACTION_CREATED, mapOf("faction" to faction)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not create faction", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun join(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val factionId = payload.str("faction_id")
            ?: return HandlerResult.Err(ServerError.fromMessage("Faction ID required", ErrorCategory.VALIDATION))
        return ctx.services.factionService.joinFaction(playerId, factionId).fold(
            onSuccess = { f -> HandlerResult.Ok(ResponseTypes.FACTION_JOINED, mapOf("faction" to f)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not join faction", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun leave(ctx: HandlerContext, playerId: String): HandlerResult =
        ctx.services.factionService.leaveFaction(playerId).fold(
            onSuccess = { HandlerResult.Ok(ResponseTypes.FACTION_LEFT, mapOf("message" to "Left faction")) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not leave faction", ErrorCategory.INTERNAL)) }
        )

    private suspend fun info(ctx: HandlerContext, playerId: String): HandlerResult =
        ctx.services.factionService.getFaction(playerId).fold(
            onSuccess = { f -> HandlerResult.Ok(ResponseTypes.FACTION_INFO, mapOf("faction" to f)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "No faction", ErrorCategory.NOT_FOUND)) }
        )

    private suspend fun list(ctx: HandlerContext): HandlerResult {
        val factions = ctx.services.factionService.listFactions()
        return HandlerResult.Ok(ResponseTypes.FACTION_LIST, mapOf("factions" to factions))
    }

    private suspend fun donate(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val cpu = payload.long("cpu")
        val ram = payload.long("ram")
        val bw = payload.long("bandwidth")
        val credits = payload.long("credits")
        return ctx.services.factionService.donateToMainframe(playerId, cpu, ram, bw, credits).fold(
            onSuccess = { mf -> HandlerResult.Ok(ResponseTypes.FACTION_DONATED, mapOf("mainframe" to mf)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Donation failed", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun upgrade(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val upgradeId = payload.str("upgrade_id")
            ?: return HandlerResult.Err(ServerError.fromMessage("Upgrade ID required", ErrorCategory.VALIDATION))
        return ctx.services.factionService.upgradeMainframe(playerId, upgradeId).fold(
            onSuccess = { mf -> HandlerResult.Ok(ResponseTypes.FACTION_UPGRADED, mapOf("mainframe" to mf)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Upgrade failed", ErrorCategory.INTERNAL)) }
        )
    }
}
