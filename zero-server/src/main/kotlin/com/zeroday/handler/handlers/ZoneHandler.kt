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

class ZoneHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.ZONE_INFO,
        RequestTypes.ZONE_LIST,
        RequestTypes.ZONE_TRAVEL,
        RequestTypes.ZONE_ATTACK,
        RequestTypes.ZONE_CLAIM,
        RequestTypes.FACTION_CYCLE_INFO
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.ZONE_INFO -> zoneInfo(ctx, payload)
            RequestTypes.ZONE_LIST -> zoneList(ctx)
            RequestTypes.ZONE_TRAVEL -> zoneTravel(ctx, playerId, payload)
            RequestTypes.ZONE_ATTACK -> zoneAttack(ctx, playerId, payload)
            RequestTypes.ZONE_CLAIM -> zoneClaim(ctx, playerId, payload)
            RequestTypes.FACTION_CYCLE_INFO -> factionCycle(ctx)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported zone request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun zoneInfo(ctx: HandlerContext, payload: JsonObject): HandlerResult {
        val zoneId = payload.str("zoneId")
            ?: return HandlerResult.Err(ServerError.fromMessage("zoneId required", ErrorCategory.VALIDATION))
        val snapshot = ctx.services.worldZoneService.getZoneSnapshot(zoneId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Zone not found", ErrorCategory.NOT_FOUND))
        return HandlerResult.Ok(ResponseTypes.ZONE_INFO, mapOf("zone" to snapshot))
    }

    private suspend fun zoneList(ctx: HandlerContext): HandlerResult {
        val zones = ctx.services.worldZoneService.getZoneSnapshots()
        return HandlerResult.Ok(ResponseTypes.ZONE_LIST, mapOf("zones" to zones))
    }

    private suspend fun zoneTravel(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val targetZoneId = payload.str("zoneId")
            ?: return HandlerResult.Err(ServerError.fromMessage("zoneId required", ErrorCategory.VALIDATION))
        val result = ctx.services.worldZoneService.initiateTravel(playerId, targetZoneId)
        return if (result.success) {
            HandlerResult.Ok(ResponseTypes.ZONE_TRAVEL_STARTED, mapOf(
                "message" to result.message,
                "travelTimeMs" to result.travelTimeMs
            ))
        } else {
            HandlerResult.Err(ServerError.fromMessage(result.message, ErrorCategory.VALIDATION))
        }
    }

    private suspend fun zoneAttack(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val targetZoneId = payload.str("zoneId")
            ?: return HandlerResult.Err(ServerError.fromMessage("zoneId required", ErrorCategory.VALIDATION))
        val result = ctx.services.worldZoneService.attackZone(playerId, targetZoneId)
        return if (result.success) {
            HandlerResult.Ok(ResponseTypes.ZONE_ATTACK_RESULT, mapOf(
                "message" to result.message,
                "threatGained" to result.threatGained,
                "repGained" to result.repGained
            ))
        } else {
            HandlerResult.Err(ServerError.fromMessage(result.message, ErrorCategory.VALIDATION))
        }
    }

    private suspend fun zoneClaim(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val targetZoneId = payload.str("zoneId")
            ?: return HandlerResult.Err(ServerError.fromMessage("zoneId required", ErrorCategory.VALIDATION))
        val result = ctx.services.worldZoneService.claimZone(playerId, targetZoneId)
        return if (result.success) {
            HandlerResult.Ok(ResponseTypes.ZONE_CLAIMED, mapOf(
                "message" to result.message
            ))
        } else {
            HandlerResult.Err(ServerError.fromMessage(result.message, ErrorCategory.VALIDATION))
        }
    }

    private suspend fun factionCycle(ctx: HandlerContext): HandlerResult {
        val cycle = ctx.services.worldZoneService.getActiveFactionCycle()
        return HandlerResult.Ok(ResponseTypes.FACTION_CYCLE, cycle)
    }
}
