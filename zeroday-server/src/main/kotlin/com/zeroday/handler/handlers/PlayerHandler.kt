package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.model.toMap
import com.zeroday.model.toSnapshot
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import kotlinx.serialization.json.JsonObject

/**
 * Self-service player actions and read-only queries about the player or
 * the global game state that doesn't belong in any other domain.
 */
class PlayerHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.GET_STATUS,
        RequestTypes.GET_ONLINE_PLAYERS,
        RequestTypes.GET_LEADERBOARD,
        RequestTypes.CREATE_PARTY,
        RequestTypes.JOIN_PARTY
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId() ?: return unauthenticated()
        return when (type) {
            RequestTypes.GET_STATUS -> getStatus(ctx, playerId)
            RequestTypes.GET_ONLINE_PLAYERS -> getOnlinePlayers(ctx)
            RequestTypes.GET_LEADERBOARD -> getLeaderboard(ctx)
            RequestTypes.CREATE_PARTY -> createParty(playerId)
            RequestTypes.JOIN_PARTY -> joinParty(ctx, playerId, payload)
            else -> unsupported(type)
        }
    }

    private suspend fun getStatus(ctx: HandlerContext, playerId: String): HandlerResult {
        val snapshot = ctx.services.playerService.getSnapshot(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        return HandlerResult.Ok(ResponseTypes.STATUS, mapOf("player" to snapshot.toMap()))
    }

    private suspend fun getOnlinePlayers(ctx: HandlerContext): HandlerResult {
        val players = ctx.services.playerService.getOnlinePlayers().map { it.toSnapshot().toMap() }
        return HandlerResult.Ok(ResponseTypes.ONLINE_PLAYERS, mapOf("players" to players))
    }

    private suspend fun getLeaderboard(ctx: HandlerContext): HandlerResult {
        val rows = ctx.services.playerService.getAllPlayers()
            .sortedByDescending { it.level }
            .take(20)
            .mapIndexed { index, p ->
                mapOf(
                    "rank" to (index + 1),
                    "username" to p.username,
                    "level" to p.level,
                    "reputation" to p.reputation,
                    "credits" to p.credits
                )
            }
        return HandlerResult.Ok(ResponseTypes.LEADERBOARD, mapOf("leaderboard" to rows))
    }

    private suspend fun createParty(playerId: String): HandlerResult {
        val partyId = "party_${playerId.take(8)}_${System.currentTimeMillis()}"
        return HandlerResult.Ok(ResponseTypes.PARTY_CREATED, mapOf("party_id" to partyId))
    }

    private suspend fun joinParty(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val partyId = payload["party_id"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            ?: return HandlerResult.Err(ServerError.fromMessage("Party ID required", ErrorCategory.VALIDATION))
        ctx.services.playerService.setParty(playerId, partyId)
        return HandlerResult.Ok(ResponseTypes.PARTY_JOINED, mapOf("party_id" to partyId))
    }

    private suspend fun unauthenticated(): HandlerResult =
        HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))

    private suspend fun unsupported(type: String): HandlerResult =
        HandlerResult.Err(ServerError.fromMessage("Unsupported player request: $type", ErrorCategory.VALIDATION))
}
