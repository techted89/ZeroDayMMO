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
import kotlinx.serialization.json.jsonPrimitive

class AuctionHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.AUCTION_LIST, RequestTypes.AUCTION_SEARCH, RequestTypes.AUCTION_CREATE,
        RequestTypes.AUCTION_BID, RequestTypes.AUCTION_BUYOUT,
        RequestTypes.AUCTION_MY_LISTINGS, RequestTypes.AUCTION_CANCEL
    )

    override val requiresAuth: Boolean = true

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.AUCTION_LIST -> listActive(ctx)
            RequestTypes.AUCTION_SEARCH -> search(ctx, payload)
            RequestTypes.AUCTION_CREATE -> create(ctx, playerId, payload)
            RequestTypes.AUCTION_BID -> bid(ctx, playerId, payload)
            RequestTypes.AUCTION_BUYOUT -> buyout(ctx, playerId, payload)
            RequestTypes.AUCTION_MY_LISTINGS -> myListings(ctx, playerId)
            RequestTypes.AUCTION_CANCEL -> cancel(ctx, playerId, payload)
            else -> HandlerResult.Err(ServerError.fromMessage("Unknown auction request", ErrorCategory.VALIDATION))
        }
    }

    private fun JsonObject.strOr(key: String, default: String = ""): String =
        (this[key] as? JsonPrimitive)?.content ?: default

    private fun JsonObject.longOr(key: String, default: Long = 0L): Long =
        (this[key] as? JsonPrimitive)?.content?.toLongOrNull() ?: default

    private fun JsonObject.intOr(key: String, default: Int = 0): Int =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull() ?: default

    private suspend fun listActive(ctx: HandlerContext): HandlerResult {
        val listings = ctx.services.auctionService.searchListings()
        return HandlerResult.Ok(ResponseTypes.AUCTION_LISTINGS, mapOf(
            "listings" to listings.map { ctx.services.auctionService.serializeListing(it) },
            "count" to listings.size
        ))
    }

    private suspend fun search(ctx: HandlerContext, payload: JsonObject): HandlerResult {
        val results = ctx.services.auctionService.searchListings(
            query = payload.strOr("query", "").ifBlank { null },
            rarity = payload.strOr("rarity", "").ifBlank { null },
            slot = payload.strOr("slot", "").ifBlank { null },
            minPrice = payload.longOr("minPrice").takeIf { it > 0 },
            maxPrice = payload.longOr("maxPrice").takeIf { it > 0 }
        )
        return HandlerResult.Ok(ResponseTypes.AUCTION_SEARCH_RESULTS, mapOf(
            "results" to results.map { ctx.services.auctionService.serializeListing(it) },
            "count" to results.size
        ))
    }

    private suspend fun create(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val itemName = payload.str("itemName")
            ?: return HandlerResult.Err(ServerError.fromMessage("itemName required", ErrorCategory.VALIDATION))
        val itemId = payload.str("itemId")
            ?: return HandlerResult.Err(ServerError.fromMessage("itemId required", ErrorCategory.VALIDATION))
        val startingBid = payload.longOr("startingBid")
        if (startingBid <= 0) return HandlerResult.Err(ServerError.fromMessage("startingBid required", ErrorCategory.VALIDATION))

        val result = ctx.services.auctionService.createListing(
            sellerId = playerId,
            itemName = itemName,
            itemId = itemId,
            itemRarity = payload.strOr("itemRarity", "Common"),
            itemSlot = payload.strOr("itemSlot", "Any"),
            itemTier = payload.intOr("itemTier", 1),
            itemLevel = payload.intOr("itemLevel", 1),
            startingBid = startingBid,
            buyoutPrice = payload.longOr("buyoutPrice"),
            durationHours = payload.intOr("durationHours", 24)
        )
        return result.fold(
            onSuccess = { listing -> HandlerResult.Ok(ResponseTypes.AUCTION_CREATED, ctx.services.auctionService.serializeListing(listing)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not create listing", ErrorCategory.VALIDATION)) }
        )
    }

    private suspend fun bid(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val listingId = payload.str("listingId")
            ?: return HandlerResult.Err(ServerError.fromMessage("listingId required", ErrorCategory.VALIDATION))
        val amount = payload.longOr("amount")
        if (amount <= 0) return HandlerResult.Err(ServerError.fromMessage("amount required", ErrorCategory.VALIDATION))

        val result = ctx.services.auctionService.placeBid(listingId, playerId, amount)
        return result.fold(
            onSuccess = { listing -> HandlerResult.Ok(ResponseTypes.AUCTION_BID_PLACED, ctx.services.auctionService.serializeListing(listing)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Bid failed", ErrorCategory.VALIDATION)) }
        )
    }

    private suspend fun buyout(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val listingId = payload.str("listingId")
            ?: return HandlerResult.Err(ServerError.fromMessage("listingId required", ErrorCategory.VALIDATION))
        val result = ctx.services.auctionService.buyout(listingId, playerId)
        return result.fold(
            onSuccess = { listing -> HandlerResult.Ok(ResponseTypes.AUCTION_BOUGHT, ctx.services.auctionService.serializeListing(listing)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Buyout failed", ErrorCategory.VALIDATION)) }
        )
    }

    private suspend fun myListings(ctx: HandlerContext, playerId: String): HandlerResult {
        val listings = ctx.services.auctionService.getPlayerListings(playerId)
        return HandlerResult.Ok(ResponseTypes.AUCTION_MY_LISTINGS, mapOf(
            "listings" to listings.map { ctx.services.auctionService.serializeListing(it) },
            "count" to listings.size
        ))
    }

    private suspend fun cancel(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val listingId = payload.str("listingId")
            ?: return HandlerResult.Err(ServerError.fromMessage("listingId required", ErrorCategory.VALIDATION))
        val result = ctx.services.auctionService.cancelListing(listingId, playerId)
        return result.fold(
            onSuccess = { HandlerResult.Ok(ResponseTypes.AUCTION_CANCELLED, mapOf("listingId" to listingId)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Cancel failed", ErrorCategory.VALIDATION)) }
        )
    }
}
