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
 * Network discovery: listing the player's known hosts, scanning a subnet
 * for new ones, and probing a single IP.
 */
class NetworkHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.GET_NETWORK,
        RequestTypes.DISCOVER_NODE,
        RequestTypes.SCAN_SUBNET
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.GET_NETWORK -> getNetwork(ctx, playerId)
            RequestTypes.DISCOVER_NODE -> discoverNode(ctx, playerId, payload)
            RequestTypes.SCAN_SUBNET -> scanSubnet(ctx, playerId, payload)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported network request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun getNetwork(ctx: HandlerContext, playerId: String): HandlerResult {
        val discovered = ctx.services.networkService.getDiscoveredNodes(playerId)
        val allNodes = ctx.services.networkService.getAllNodes()
        val stats = ctx.services.networkService.getNetworkStats()
        return HandlerResult.Ok(
            type = ResponseTypes.NETWORK,
            payload = mapOf(
                "discovered_nodes" to discovered,
                "all_nodes_count" to allNodes.size,
                "stats" to stats
            )
        )
    }

    private suspend fun discoverNode(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val ip = try { payload.reqStr("ip") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("IP required", ErrorCategory.VALIDATION))
        }
        return ctx.services.networkService.discoverNode(playerId, ip).fold(
            onSuccess = { node -> HandlerResult.Ok(ResponseTypes.NODE_DISCOVERED, mapOf("node" to node)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Discovery failed", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun scanSubnet(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val subnet = try { payload.reqStr("subnet") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Subnet required", ErrorCategory.VALIDATION))
        }
        val found = ctx.services.networkService.scanSubnet(playerId, subnet)
        return HandlerResult.Ok(
            type = ResponseTypes.SUBNET_SCANNED,
            payload = mapOf(
                "subnet" to subnet,
                "found_nodes" to found,
                "count" to found.size
            )
        )
    }
}
