using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public class AuctionService : IHostedService
{
    private readonly IPlayerService _playerService;
    private readonly GameEventBus? _gameEventBus;

    private readonly Dictionary<string, AuctionListing> _listings = new();
    private readonly SemaphoreSlim _mutex = new(1, 1);
    private CancellationTokenSource? _cts;
    private Task? _expiryTask;

    public class AuctionListing
    {
        public string ListingId { get; set; } = Guid.NewGuid().ToString();
        public string SellerId { get; set; } = "";
        public string SellerName { get; set; } = "";
        public string ItemName { get; set; } = "";
        public string ItemId { get; set; } = "";
        public string ItemRarity { get; set; } = "";
        public string ItemSlot { get; set; } = "";
        public int ItemTier { get; set; }
        public int ItemLevel { get; set; }
        public long StartingBid { get; set; }
        public long BuyoutPrice { get; set; }
        public long CurrentBid { get; set; }
        public string? HighestBidderId { get; set; }
        public string? HighestBidderName { get; set; }
        public int BidCount { get; set; }
        public long CreatedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        public long ExpiresAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() + 86_400_000;
        public ListingStatus Status { get; set; } = ListingStatus.ACTIVE;
    }

    public enum ListingStatus { ACTIVE, SOLD, EXPIRED, CANCELLED }

    public AuctionService(IPlayerService playerService, GameEventBus? gameEventBus = null)
    {
        _playerService = playerService;
        _gameEventBus = gameEventBus;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _expiryTask = ExpiryLoopAsync(_cts.Token);
        return Task.CompletedTask;
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        _cts?.Cancel();
        if (_expiryTask is not null)
        {
            try { await _expiryTask; } catch (OperationCanceledException) { }
        }
    }

    private async Task ExpiryLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay(60_000, ct);
            await ProcessExpired();
        }
    }

    public async Task<AuctionListing> CreateListing(
        string sellerId, string itemName, string itemId,
        string itemRarity, string itemSlot, int itemTier,
        int itemLevel, long startingBid, long buyoutPrice,
        int durationHours = 24)
    {
        if (startingBid < 1) throw new InvalidOperationException("Starting bid must be at least 1 credit");
        var fee = Math.Max((long)(startingBid * 0.05), 1L);
        var expiresIn = Math.Clamp(durationHours * 3_600_000L, 3_600_000L, 7 * 86_400_000L);

        var seller = _playerService.GetPlayer(sellerId);
        if (seller is null) throw new InvalidOperationException("Player not found");
        if (seller.Credits < fee) throw new InvalidOperationException("Insufficient credits for listing fee");

        seller.Credits -= fee;
        _playerService.UpdatePlayer(seller);

        await _mutex.WaitAsync();
        try
        {
            var listing = new AuctionListing
            {
                SellerId = sellerId,
                SellerName = seller.Username,
                ItemName = itemName,
                ItemId = itemId,
                ItemRarity = itemRarity,
                ItemSlot = itemSlot,
                ItemTier = itemTier,
                ItemLevel = itemLevel,
                StartingBid = startingBid,
                BuyoutPrice = buyoutPrice,
                ExpiresAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() + expiresIn
            };
            _listings[listing.ListingId] = listing;
            _gameEventBus?.Publish(new GameEvent { PlayerId = sellerId, Type = AchievementEvent.ITEM_CRAFTED, Value = startingBid });
            return listing;
        }
        finally { _mutex.Release(); }
    }

    public async Task<AuctionListing> PlaceBid(string listingId, string bidderId, long amount)
    {
        AuctionListing listing;
        long prevBid;
        string prevHighestBidderId;

        await _mutex.WaitAsync();
        try
        {
            if (!_listings.TryGetValue(listingId, out var l) || l.Status != ListingStatus.ACTIVE || l.SellerId == bidderId)
                throw new InvalidOperationException("Bid failed: listing invalid");
            var minBid = Math.Max(l.StartingBid, l.CurrentBid + Math.Max((long)(l.CurrentBid * 0.05), 1L));
            if (amount < minBid) throw new InvalidOperationException("Bid too low");
            prevBid = l.CurrentBid;
            prevHighestBidderId = l.HighestBidderId ?? "";
            listing = l;
        }
        finally { _mutex.Release(); }

        if (!string.IsNullOrEmpty(prevHighestBidderId) && prevBid > 0)
        {
            var prevBidder = _playerService.GetPlayer(prevHighestBidderId);
            if (prevBidder is not null)
            {
                prevBidder.Credits += prevBid;
                _playerService.UpdatePlayer(prevBidder);
            }
        }

        var bidder = _playerService.GetPlayer(bidderId);
        if (bidder is null)
        {
            if (!string.IsNullOrEmpty(prevHighestBidderId) && prevBid > 0)
            {
                var refundPlayer = _playerService.GetPlayer(prevHighestBidderId);
                if (refundPlayer is not null)
                {
                    refundPlayer.Credits -= prevBid;
                    _playerService.UpdatePlayer(refundPlayer);
                }
            }
            throw new InvalidOperationException("Bidder not found");
        }

        if (bidder.Credits < amount)
        {
            if (!string.IsNullOrEmpty(prevHighestBidderId) && prevBid > 0)
            {
                var refundPlayer = _playerService.GetPlayer(prevHighestBidderId);
                if (refundPlayer is not null)
                {
                    refundPlayer.Credits -= prevBid;
                    _playerService.UpdatePlayer(refundPlayer);
                }
            }
            throw new InvalidOperationException("Insufficient credits");
        }

        bidder.Credits -= amount;
        _playerService.UpdatePlayer(bidder);

        await _mutex.WaitAsync();
        try
        {
            listing.CurrentBid = amount;
            listing.HighestBidderId = bidderId;
            listing.HighestBidderName = bidder.Username;
            listing.BidCount++;
        }
        finally { _mutex.Release(); }

        return listing;
    }

    public async Task<AuctionListing> Buyout(string listingId, string buyerId)
    {
        AuctionListing listing;
        string sellerId;
        long price;

        await _mutex.WaitAsync();
        try
        {
            if (!_listings.TryGetValue(listingId, out var l))
                throw new InvalidOperationException("Listing not found");
            if (l.Status != ListingStatus.ACTIVE)
                throw new InvalidOperationException("Listing is not active");
            if (l.BuyoutPrice <= 0)
                throw new InvalidOperationException("No buyout price set");
            if (l.SellerId == buyerId)
                throw new InvalidOperationException("Cannot buy your own listing");

            var buyer = _playerService.GetPlayer(buyerId);
            if (buyer is null || buyer.Credits < l.BuyoutPrice)
                throw new InvalidOperationException("Insufficient credits for buyout");

            listing = l;
            sellerId = l.SellerId;
            price = l.BuyoutPrice;
        }
        finally { _mutex.Release(); }

        var buyerPlayer = _playerService.GetPlayer(buyerId);
        if (buyerPlayer is null) throw new InvalidOperationException("Buyer not found");
        if (buyerPlayer.Credits < price)
            throw new InvalidOperationException("Insufficient credits for buyout");

        buyerPlayer.Credits -= price;
        _playerService.UpdatePlayer(buyerPlayer);

        var sellerCut = (long)(price * 0.95);
        var seller = _playerService.GetPlayer(sellerId);
        if (seller is null)
        {
            buyerPlayer.Credits += price;
            _playerService.UpdatePlayer(buyerPlayer);
            throw new InvalidOperationException("Seller not found");
        }
        seller.Credits += sellerCut;
        _playerService.UpdatePlayer(seller);

        await _mutex.WaitAsync();
        try
        {
            if (listing.Status != ListingStatus.ACTIVE)
            {
                buyerPlayer.Credits += price;
                _playerService.UpdatePlayer(buyerPlayer);
                throw new InvalidOperationException("Listing was already sold");
            }
            listing.Status = ListingStatus.SOLD;
        }
        finally { _mutex.Release(); }

        return listing;
    }

    public async Task<List<AuctionListing>> SearchListings(
        string? query = null, string? rarity = null, string? slot = null,
        long? minPrice = null, long? maxPrice = null)
    {
        await _mutex.WaitAsync();
        try
        {
            var results = _listings.Values.Where(l => l.Status == ListingStatus.ACTIVE).AsEnumerable();

            if (!string.IsNullOrWhiteSpace(query))
                results = results.Where(l => l.ItemName.Contains(query, StringComparison.OrdinalIgnoreCase));
            if (!string.IsNullOrWhiteSpace(rarity))
                results = results.Where(l => string.Equals(l.ItemRarity, rarity, StringComparison.OrdinalIgnoreCase));
            if (!string.IsNullOrWhiteSpace(slot))
                results = results.Where(l => string.Equals(l.ItemSlot, slot, StringComparison.OrdinalIgnoreCase));
            if (minPrice.HasValue)
                results = results.Where(l => l.CurrentBid >= minPrice.Value || l.BuyoutPrice >= minPrice.Value);
            if (maxPrice.HasValue)
                results = results.Where(l => l.CurrentBid <= maxPrice.Value || l.BuyoutPrice <= maxPrice.Value);

            return results.OrderByDescending(l => l.CurrentBid).ToList();
        }
        finally { _mutex.Release(); }
    }

    public async Task<List<AuctionListing>> GetPlayerListings(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            return _listings.Values
                .Where(l => l.SellerId == playerId)
                .OrderByDescending(l => l.CreatedAt)
                .ToList();
        }
        finally { _mutex.Release(); }
    }

    public async Task<List<AuctionListing>> GetPlayerBids(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            return _listings.Values
                .Where(l => l.HighestBidderId == playerId && l.Status == ListingStatus.ACTIVE)
                .ToList();
        }
        finally { _mutex.Release(); }
    }

    public async Task CancelListing(string listingId, string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            if (!_listings.TryGetValue(listingId, out var listing))
                throw new InvalidOperationException("Listing not found");
            if (listing.SellerId != playerId) throw new InvalidOperationException("Not your listing");
            if (listing.Status != ListingStatus.ACTIVE) throw new InvalidOperationException("Listing is no longer active");
            if (listing.BidCount > 0) throw new InvalidOperationException("Cannot cancel listing with active bids");

            listing.Status = ListingStatus.CANCELLED;
        }
        finally { _mutex.Release(); }
    }

    private record PendingPayout(string SellerId, long Amount);

    private async Task ProcessExpired()
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        List<PendingPayout> payouts;

        await _mutex.WaitAsync();
        try
        {
            payouts = _listings.Values
                .Where(l => l.Status == ListingStatus.ACTIVE && now > l.ExpiresAt)
                .Select(listing =>
                {
                    listing.Status = ListingStatus.EXPIRED;
                    if (listing.HighestBidderId is not null && listing.CurrentBid > 0)
                    {
                        var sellerCut = (long)(listing.CurrentBid * 0.95);
                        listing.Status = ListingStatus.SOLD;
                        return new PendingPayout(listing.SellerId, sellerCut);
                    }
                    return null;
                })
                .Where(p => p is not null)
                .Cast<PendingPayout>()
                .ToList();
        }
        finally { _mutex.Release(); }

        foreach (var payout in payouts)
        {
            var seller = _playerService.GetPlayer(payout.SellerId);
            if (seller is not null)
            {
                seller.Credits += payout.Amount;
                _playerService.UpdatePlayer(seller);
            }
        }
    }

    public AuctionListing? GetListing(string listingId) =>
        _listings.TryGetValue(listingId, out var listing) ? listing : null;

    public Dictionary<string, object?> SerializeListing(AuctionListing listing) => new()
    {
        ["listingId"] = listing.ListingId,
        ["sellerName"] = listing.SellerName,
        ["itemName"] = listing.ItemName,
        ["itemRarity"] = listing.ItemRarity,
        ["itemSlot"] = listing.ItemSlot,
        ["itemTier"] = listing.ItemTier,
        ["itemLevel"] = listing.ItemLevel,
        ["startingBid"] = listing.StartingBid,
        ["buyoutPrice"] = listing.BuyoutPrice,
        ["currentBid"] = listing.CurrentBid,
        ["highestBidderName"] = listing.HighestBidderName ?? "",
        ["bidCount"] = listing.BidCount,
        ["timeRemainingHours"] = (listing.ExpiresAt - DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()) / 3_600_000.0,
        ["status"] = listing.Status.ToString()
    };
}
