package com.zeroday.service

import com.zeroday.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class AuctionService(
    private val playerService: PlayerService,
    private val gameEventBus: GameEventBus? = null
) {
    data class AuctionListing(
        val listingId: String = UUID.randomUUID().toString(),
        val sellerId: String,
        val sellerName: String,
        val itemName: String,
        val itemId: String,
        val itemRarity: String,
        val itemSlot: String,
        val itemTier: Int,
        val itemLevel: Int,
        val startingBid: Long,
        val buyoutPrice: Long = 0L,
        var currentBid: Long = 0L,
        var highestBidderId: String? = null,
        var highestBidderName: String? = null,
        var bidCount: Int = 0,
        val createdAt: Long = System.currentTimeMillis(),
        var expiresAt: Long = System.currentTimeMillis() + 86_400_000L, // 24h default
        var status: ListingStatus = ListingStatus.ACTIVE
    )

    enum class ListingStatus { ACTIVE, SOLD, EXPIRED, CANCELLED }

    private val listings = mutableMapOf<String, AuctionListing>()
    private val mutex = Mutex()
    private var expiryJob: Job? = null

    fun start(scope: CoroutineScope) {
        expiryJob = scope.launch {
            while (isActive) {
                delay(60_000L)
                processExpired()
            }
        }
    }

    fun stop() {
        expiryJob?.cancel()
    }

    suspend fun createListing(
        sellerId: String,
        itemName: String,
        itemId: String,
        itemRarity: String,
        itemSlot: String,
        itemTier: Int,
        itemLevel: Int,
        startingBid: Long,
        buyoutPrice: Long,
        durationHours: Int = 24
    ): Result<AuctionListing> {
        if (startingBid < 1) return Result.failure(Exception("Starting bid must be at least 1 credit"))
        val fee = (startingBid * 0.05).toLong().coerceAtLeast(1L)
        val expiresIn = (durationHours * 3_600_000L).coerceIn(3_600_000L, 7 * 86_400_000L)

        // Phase 1: debit seller outside listing mutex
        val sellerName = playerService.withPlayerAction(sellerId) { seller ->
            if (seller.credits < fee) return@withPlayerAction null
            seller.credits -= fee
            seller.username
        } ?: return Result.failure(Exception("Player not found or insufficient credits"))

        // Phase 2: create listing under mutex
        val listing = mutex.withLock {
            val l = AuctionListing(
                sellerId = sellerId,
                sellerName = sellerName,
                itemName = itemName,
                itemId = itemId,
                itemRarity = itemRarity,
                itemSlot = itemSlot,
                itemTier = itemTier,
                itemLevel = itemLevel,
                startingBid = startingBid,
                buyoutPrice = buyoutPrice,
                expiresAt = System.currentTimeMillis() + expiresIn
            )
            listings[l.listingId] = l
            l
        }
        gameEventBus?.emit(GameEvent(sellerId, AchievementEvent.ITEM_CRAFTED, startingBid))
        return Result.success(listing)
    }

    suspend fun placeBid(listingId: String, bidderId: String, amount: Long): Result<AuctionListing> {
        // Phase 1: validate listing state and get metadata under mutex
        val listingData = mutex.withLock {
            val listing = listings[listingId]
            if (listing == null || listing.status != ListingStatus.ACTIVE || listing.sellerId == bidderId)
                return@withLock null
            val minBid = maxOf(listing.startingBid,
                listing.currentBid + (listing.currentBid * 0.05).toLong().coerceAtLeast(1L))
            if (amount < minBid) return@withLock null
            listing.currentBid to (listing.highestBidderId ?: "") to listing
        } ?: return Result.failure(Exception("Bid failed: listing invalid or bid too low"))
        val (prevBid, prevHighestBidderId) = listingData.first
        val listing = listingData.second

        // Phase 2: refund previous highest bidder (outside listing mutex)
        if (prevHighestBidderId.isNotEmpty() && prevBid > 0) {
            playerService.withPlayerAction(prevHighestBidderId) { player ->
                player.credits += prevBid
            }
        }

        // Phase 3: debit bidder atomically
        val bidderName = playerService.withPlayerAction(bidderId) { player ->
            if (player.credits < amount) return@withPlayerAction null
            player.credits -= amount
            player.username
        } ?: run {
            // Refund the previous high bidder if we already refunded them
            if (prevHighestBidderId.isNotEmpty() && prevBid > 0) {
                playerService.withPlayerAction(prevHighestBidderId) { player ->
                    player.credits -= prevBid
                }
            }
            return Result.failure(Exception("Insufficient credits"))
        }

        // Phase 4: update listing state (under mutex)
        mutex.withLock {
            listing.currentBid = amount
            listing.highestBidderId = bidderId
            listing.highestBidderName = bidderName
            listing.bidCount++
        }

        return Result.success(listing)
    }

    suspend fun buyout(listingId: String, buyerId: String): Result<AuctionListing> {
        // Phase 1: validate listing and compute payout under mutex
        val (listingSnap, sellerId, price) = mutex.withLock {
            val listing = listings[listingId]
                ?: return@withLock Triple(null, "", 0L)
            if (listing.status != ListingStatus.ACTIVE)
                return@withLock Triple(null, "", 0L)
            if (listing.buyoutPrice <= 0)
                return@withLock Triple(null, "", 0L)
            if (listing.sellerId == buyerId)
                return@withLock Triple(null, "", 0L)

            val buyer = playerService.getPlayer(buyerId)
            if (buyer == null || buyer.credits < listing.buyoutPrice)
                return@withLock Triple(null, "", 0L)

            Triple(listing, listing.sellerId, listing.buyoutPrice)
        }
        val listing = listingSnap ?: return Result.failure(
            Exception("Listing not found, inactive, or insufficient credits")
        )

        // Phase 2: debit buyer (outside listing mutex)
        val debitOk = playerService.withPlayerAction(buyerId) { buyer ->
            if (buyer.credits < price) return@withPlayerAction false
            buyer.credits -= price
            true
        } ?: return Result.failure(Exception("Buyer not found"))
        if (!debitOk) return Result.failure(Exception("Insufficient credits for buyout"))

        // Phase 3: credit seller (outside listing mutex)
        val sellerCut = (price * 0.95).toLong()
        val sellerCredited = playerService.withPlayerAction(sellerId) { seller ->
            seller.credits += sellerCut
            true
        } ?: false
        if (!sellerCredited) {
            // Rollback: refund buyer
            playerService.withPlayerAction(buyerId) { buyer ->
                buyer.credits += price
            }
            return Result.failure(Exception("Seller not found"))
        }

        // Phase 4: mark sold (under mutex) — fail if already sold
        val marked = mutex.withLock {
            if (listing.status != ListingStatus.ACTIVE) false
            else {
                listing.status = ListingStatus.SOLD
                true
            }
        }
        if (!marked) {
            // Rollback: refund buyer
            playerService.withPlayerAction(buyerId) { buyer ->
                buyer.credits += price
            }
            // No need to refund seller — they were never credited if the sale failed
            return Result.failure(Exception("Listing was already sold"))
        }
        return Result.success(listing)
    }

    suspend fun searchListings(
        query: String? = null,
        rarity: String? = null,
        slot: String? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null
    ): List<AuctionListing> = mutex.withLock {
        var results = listings.values.filter { it.status == ListingStatus.ACTIVE }

        if (!query.isNullOrBlank())
            results = results.filter { it.itemName.contains(query, ignoreCase = true) }
        if (!rarity.isNullOrBlank())
            results = results.filter { it.itemRarity.equals(rarity, ignoreCase = true) }
        if (!slot.isNullOrBlank())
            results = results.filter { it.itemSlot.equals(slot, ignoreCase = true) }
        if (minPrice != null)
            results = results.filter { it.currentBid >= minPrice || it.buyoutPrice >= minPrice }
        if (maxPrice != null)
            results = results.filter { it.currentBid <= maxPrice || it.buyoutPrice <= maxPrice }

        results.sortedByDescending { it.currentBid }
    }

    suspend fun getPlayerListings(playerId: String): List<AuctionListing> = mutex.withLock {
        listings.values.filter { it.sellerId == playerId }.sortedByDescending { it.createdAt }
    }

    suspend fun getPlayerBids(playerId: String): List<AuctionListing> = mutex.withLock {
        listings.values.filter { it.highestBidderId == playerId && it.status == ListingStatus.ACTIVE }
    }

    suspend fun cancelListing(listingId: String, playerId: String): Result<Unit> = mutex.withLock {
        val listing = listings[listingId] ?: return@withLock Result.failure(Exception("Listing not found"))
        if (listing.sellerId != playerId) return@withLock Result.failure(Exception("Not your listing"))
        if (listing.status != ListingStatus.ACTIVE) return@withLock Result.failure(Exception("Listing is no longer active"))
        if (listing.bidCount > 0) return@withLock Result.failure(Exception("Cannot cancel listing with active bids"))

        listing.status = ListingStatus.CANCELLED
        Result.success(Unit)
    }

    private data class PendingPayout(val sellerId: String, val amount: Long)

    private suspend fun processExpired() {
        val now = System.currentTimeMillis()
        val payouts = mutex.withLock {
            val expired = listings.values.filter {
                it.status == ListingStatus.ACTIVE && now > it.expiresAt
            }
            expired.map { listing ->
                listing.status = ListingStatus.EXPIRED
                if (listing.highestBidderId != null && listing.currentBid > 0) {
                    val sellerCut = (listing.currentBid * 0.95).toLong()
                    listing.status = ListingStatus.SOLD
                    PendingPayout(listing.sellerId, sellerCut)
                } else {
                    null
                }
            }.filterNotNull()
        }
        // Process payouts outside the outer lock to avoid lock ordering issues
        for (payout in payouts) {
            playerService.withPlayerAction(payout.sellerId) { seller ->
                seller.credits += payout.amount
            }
        }
    }

    fun getListing(listingId: String): AuctionListing? = listings[listingId]

    fun serializeListing(listing: AuctionListing): Map<String, Any?> = mapOf(
        "listingId" to listing.listingId,
        "sellerName" to listing.sellerName,
        "itemName" to listing.itemName,
        "itemRarity" to listing.itemRarity,
        "itemSlot" to listing.itemSlot,
        "itemTier" to listing.itemTier,
        "itemLevel" to listing.itemLevel,
        "startingBid" to listing.startingBid,
        "buyoutPrice" to listing.buyoutPrice,
        "currentBid" to listing.currentBid,
        "highestBidderName" to (listing.highestBidderName ?: ""),
        "bidCount" to listing.bidCount,
        "timeRemainingHours" to ((listing.expiresAt - System.currentTimeMillis()) / 3_600_000.0),
        "status" to listing.status.name
    )
}
