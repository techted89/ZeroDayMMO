using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class AuctionHandler : IHandler
{
    private readonly AuctionService _auctionService;

    public string MessageType => "auction_list";

    public AuctionHandler(AuctionService auctionService)
    {
        _auctionService = auctionService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        try
        {
            var listings = await _auctionService.SearchListings();
            return new MessageResult("auction_listings", connectionId, new
            {
                listings = listings.Select(l => _auctionService.SerializeListing(l)),
                count = listings.Count
            });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleSearch(string connectionId, JsonElement? payload, string? requestId)
    {
        try
        {
            var query = payload?.Str("query");
            var rarity = payload?.Str("rarity");
            var slot = payload?.Str("slot");
            var minPrice = payload?.Long("minPrice");
            var maxPrice = payload?.Long("maxPrice");

            var results = await _auctionService.SearchListings(
                query: string.IsNullOrEmpty(query) ? null : query,
                rarity: string.IsNullOrEmpty(rarity) ? null : rarity,
                slot: string.IsNullOrEmpty(slot) ? null : slot,
                minPrice: minPrice.GetValueOrDefault() > 0 ? minPrice : null,
                maxPrice: maxPrice.GetValueOrDefault() > 0 ? maxPrice : null
            );
            return new MessageResult("auction_search_results", connectionId, new
            {
                results = results.Select(l => _auctionService.SerializeListing(l)),
                count = results.Count
            });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleCreate(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var itemName = payload?.Str("itemName");
        var itemId = payload?.Str("itemId");
        var startingBid = payload?.Long("startingBid") ?? 0L;

        if (string.IsNullOrEmpty(itemName))
            return new MessageResult("error", connectionId, new { message = "itemName required" });
        if (string.IsNullOrEmpty(itemId))
            return new MessageResult("error", connectionId, new { message = "itemId required" });
        if (startingBid <= 0)
            return new MessageResult("error", connectionId, new { message = "startingBid required" });

        try
        {
            var listing = await _auctionService.CreateListing(
                sellerId: playerId,
                itemName: itemName,
                itemId: itemId,
                itemRarity: payload?.Str("itemRarity") ?? "Common",
                itemSlot: payload?.Str("itemSlot") ?? "Any",
                itemTier: payload?.Int("itemTier") ?? 1,
                itemLevel: payload?.Int("itemLevel") ?? 1,
                startingBid: startingBid,
                buyoutPrice: payload?.Long("buyoutPrice") ?? 0L,
                durationHours: payload?.Int("durationHours") ?? 24
            );
            return new MessageResult("auction_created", connectionId, _auctionService.SerializeListing(listing));
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleBid(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var listingId = payload?.Str("listingId");
        var amount = payload?.Long("amount") ?? 0L;

        if (string.IsNullOrEmpty(listingId))
            return new MessageResult("error", connectionId, new { message = "listingId required" });
        if (amount <= 0)
            return new MessageResult("error", connectionId, new { message = "amount required" });

        try
        {
            var listing = await _auctionService.PlaceBid(listingId, playerId, amount);
            return new MessageResult("auction_bid_placed", connectionId, _auctionService.SerializeListing(listing));
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleBuyout(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var listingId = payload?.Str("listingId");
        if (string.IsNullOrEmpty(listingId))
            return new MessageResult("error", connectionId, new { message = "listingId required" });

        try
        {
            var listing = await _auctionService.Buyout(listingId, playerId);
            return new MessageResult("auction_bought", connectionId, _auctionService.SerializeListing(listing));
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleMyListings(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        try
        {
            var listings = await _auctionService.GetPlayerListings(playerId);
            return new MessageResult("auction_my_listings", connectionId, new
            {
                listings = listings.Select(l => _auctionService.SerializeListing(l)),
                count = listings.Count
            });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleCancel(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var listingId = payload?.Str("listingId");
        if (string.IsNullOrEmpty(listingId))
            return new MessageResult("error", connectionId, new { message = "listingId required" });

        try
        {
            await _auctionService.CancelListing(listingId, playerId);
            return new MessageResult("auction_cancelled", connectionId, new { listingId });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }
}
