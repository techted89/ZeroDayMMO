using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public enum ZoneFaction
{
    Neutral,
    Syndicate,
    CorpNet,
    GhostNet,
    FreeWorld,
    ZeroDay,
    LawEnforcement
}

public enum ZoneState
{
    Safe,
    Contested,
    Controlled,
    Warzone,
    Locked
}

public class Zone
{
    public string Id { get; }
    public string Name { get; }
    public string Description { get; }
    public ZoneFaction ControllingFaction { get; set; }
    public ZoneState State { get; set; }
    public int ControlLevel { get; set; }
    public int MaxControlLevel { get; }
    public int SecurityLevel { get; set; }
    public int RequiredLevel { get; }
    public List<string> ConnectedZoneIds { get; }
    public bool HasBoss { get; set; }
    public string BossName { get; set; }
    public int BossLevel { get; set; }
    public int ThreatLevel { get; set; }

    public Zone(
        string id,
        string name,
        string description,
        ZoneFaction controllingFaction,
        ZoneState state,
        int maxControlLevel,
        int securityLevel,
        int requiredLevel,
        List<string> connectedZoneIds,
        bool hasBoss,
        string bossName,
        int bossLevel,
        int threatLevel)
    {
        Id = id;
        Name = name;
        Description = description;
        ControllingFaction = controllingFaction;
        State = state;
        ControlLevel = maxControlLevel;
        MaxControlLevel = maxControlLevel;
        SecurityLevel = securityLevel;
        RequiredLevel = requiredLevel;
        ConnectedZoneIds = connectedZoneIds;
        HasBoss = hasBoss;
        BossName = bossName;
        BossLevel = bossLevel;
        ThreatLevel = threatLevel;
    }
}

public record ZoneSnapshot(
    string Id,
    string Name,
    string Description,
    string ControllingFaction,
    string State,
    int ControlLevel,
    int MaxControlLevel,
    int SecurityLevel,
    int RequiredLevel,
    List<string> ConnectedZoneIds,
    bool HasBoss,
    string BossName,
    int BossLevel,
    int ThreatLevel
)
{
    public static ZoneSnapshot From(Zone zone) => new(
        zone.Id, zone.Name, zone.Description,
        zone.ControllingFaction.ToString(), zone.State.ToString(),
        zone.ControlLevel, zone.MaxControlLevel, zone.SecurityLevel,
        zone.RequiredLevel, new(zone.ConnectedZoneIds),
        zone.HasBoss, zone.BossName, zone.BossLevel, zone.ThreatLevel
    );
}

public class WorldZoneService : IWorldZoneService
{
    private readonly IPlayerService _playerService;
    private readonly IGameEventBus? _gameEventBus;
    private readonly Dictionary<string, Zone> _zones = new();
    private readonly SemaphoreSlim _mutex = new(1, 1);
    private CancellationTokenSource? _cts;
    private readonly Dictionary<string, TravelSession> _travelingPlayers = new();

    public record TravelResult(bool Success, string Message, long TravelTimeMs = 0);
    public record ZoneActionResult(bool Success, string Message, int ThreatGained = 0, int RepGained = 0);
    public record TravelSession(string PlayerId, string FromZoneId, string ToZoneId, long ArrivalTimeMs);

    public WorldZoneService(IPlayerService playerService, IGameEventBus? gameEventBus = null)
    {
        _playerService = playerService;
        _gameEventBus = gameEventBus;
    }

    public void Start()
    {
        InitializeZones();
        _cts = new CancellationTokenSource();
        var token = _cts.Token;

        _ = Task.Run(async () =>
        {
            while (!token.IsCancellationRequested)
            {
                await Task.Delay(10_000, token);
                await ProcessTravelArrivals();
            }
        }, token);
    }

    public void Stop()
    {
        _cts?.Cancel();
    }

    private void InitializeZones()
    {
        var zoneDefs = new List<Zone>
        {
            new("safehouse", "Safehouse", "Your personal hideout. No faction activity here.",
                ZoneFaction.Neutral, ZoneState.Safe, 100, 0, 1,
                new() { "downtown", "suburbs" }, false, "", 0, 0),
            new("downtown", "Downtown", "The bustling city center. Heavy network traffic.",
                ZoneFaction.CorpNet, ZoneState.Contested, 60, 2, 1,
                new() { "safehouse", "industrial", "financial", "suburbs" },
                true, "CorpSec AI", 5, 15),
            new("suburbs", "Suburbs", "Residential district with moderate security.",
                ZoneFaction.Neutral, ZoneState.Safe, 80, 1, 1,
                new() { "safehouse", "downtown", "industrial" },
                false, "", 0, 5),
            new("industrial", "Industrial Zone", "Factory sector with weak security but little of value.",
                ZoneFaction.Syndicate, ZoneState.Controlled, 90, 1, 3,
                new() { "suburbs", "downtown", "ports" },
                true, "Syndicate Foreman", 8, 20),
            new("financial", "Financial District", "High-security banking sector.",
                ZoneFaction.CorpNet, ZoneState.Controlled, 85, 4, 5,
                new() { "downtown", "ports" },
                true, "Trading Algorithm", 12, 30),
            new("ports", "Port Authority", "Shipping and logistics hub.",
                ZoneFaction.GhostNet, ZoneState.Contested, 55, 2, 4,
                new() { "industrial", "financial", "research" },
                true, "Ghost Net Controller", 10, 25),
            new("research", "Research Lab", "Underground research facility.",
                ZoneFaction.FreeWorld, ZoneState.Controlled, 70, 3, 6,
                new() { "ports", "darknet" },
                true, "Lead Scientist", 15, 35),
            new("darknet", "Darknet", "The hidden underbelly of the network.",
                ZoneFaction.ZeroDay, ZoneState.Warzone, 40, 5, 8,
                new() { "research", "safehouse" },
                true, "Darknet Overlord", 20, 50),
            new("govt", "Govt Network", "Classified government systems. Maximum security.",
                ZoneFaction.LawEnforcement, ZoneState.Locked, 100, 5, 10,
                new() { "downtown", "darknet" },
                true, "AI Sentinel", 25, 60)
        };

        foreach (var zone in zoneDefs)
        {
            _zones[zone.Id] = zone;
        }
    }

    public Task<Zone?> GetZone(string zoneId)
    {
        _zones.TryGetValue(zoneId, out var zone);
        return Task.FromResult(zone);
    }

    public Task<List<Zone>> GetAllZones()
    {
        return Task.FromResult(_zones.Values.ToList());
    }

    public Task<List<ZoneSnapshot>> GetZoneSnapshots()
    {
        return Task.FromResult(_zones.Values.Select(ZoneSnapshot.From).ToList());
    }

    public Task<ZoneSnapshot?> GetZoneSnapshot(string zoneId)
    {
        if (_zones.TryGetValue(zoneId, out var zone))
            return Task.FromResult<ZoneSnapshot?>(ZoneSnapshot.From(zone));
        return Task.FromResult<ZoneSnapshot?>(null);
    }

    public async Task<TravelResult> TravelTo(string playerId, string targetZoneId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player == null)
                return new TravelResult(false, "Player not found");

            if (!_zones.TryGetValue(targetZoneId, out var zone))
                return new TravelResult(false, "Zone not found");

            if (targetZoneId == player.CurrentZoneId)
                return new TravelResult(false, "Already in that zone");

            if (zone.State == ZoneState.Locked)
                return new TravelResult(false, "Zone is locked");

            if (player.Level < zone.RequiredLevel)
                return new TravelResult(false, $"Level {zone.RequiredLevel} required to enter {zone.Name}");

            if (_zones.TryGetValue(player.CurrentZoneId, out var currentZone))
            {
                var hasRoute = currentZone.ConnectedZoneIds.Contains(targetZoneId) ||
                               zone.ConnectedZoneIds.Contains(player.CurrentZoneId);
                if (!hasRoute)
                    return new TravelResult(false, $"No route from {currentZone.Name} to {zone.Name}");
            }

            var travelTimeMs = Math.Min(1000L + zone.SecurityLevel * 2000L + zone.ThreatLevel * 100L, 30_000L);

            await _playerService.WithPlayerActionAsync(playerId, p =>
            {
                p.CurrentZoneId = targetZoneId;
                return Task.CompletedTask;
            });

            return new TravelResult(true, $"Arrived at {zone.Name}", travelTimeMs);
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<ZoneActionResult> AttackZone(string playerId, string targetZoneId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player == null)
                return new ZoneActionResult(false, "Player not found");

            if (!_zones.TryGetValue(targetZoneId, out var zone))
                return new ZoneActionResult(false, "Zone not found");

            if (zone.State == ZoneState.Locked)
                return new ZoneActionResult(false, "Zone is locked");

            var playerPower = player.Level * 10 + (int)(player.BreakthroughMultiplier * 100);
            var zoneDefense = zone.SecurityLevel * 50 + zone.ControlLevel * 2 + zone.ThreatLevel * 3;
            var successProb = Math.Clamp((double)playerPower / (playerPower + zoneDefense), 0.05, 0.95);
            var success = Random.Shared.NextDouble() < successProb;

            if (success)
            {
                var controlGain = Math.Min(5 + player.Level / 3, 20);
                zone.ControlLevel = Math.Max(zone.ControlLevel - controlGain, 0);
                var repGain = controlGain / 2;

                // Use a local capture before the closure
                var capturedZoneId = targetZoneId;
                var capturedFactionName = zone.ControllingFaction.ToString();

                await _playerService.WithPlayerActionAsync(playerId, p =>
                {
                    p.ZoneControlContributions[capturedZoneId] =
                        p.ZoneControlContributions.GetValueOrDefault(capturedZoneId) + controlGain;
                    p.FactionReputations[capturedFactionName] =
                        p.FactionReputations.GetValueOrDefault(capturedFactionName) - repGain;
                    return Task.CompletedTask;
                });

                UpdateZoneState(zone);

                if (_gameEventBus != null)
                    await _gameEventBus.Emit(new GameEvent { PlayerId = playerId, Type = AchievementEvent.NETWORK_COMPROMISED, Value = controlGain });

                return new ZoneActionResult(true, $"Attack successful! Reduced {zone.Name} control by {controlGain}.", repGain, repGain);
            }
            else
            {
                await _playerService.WithPlayerActionAsync(playerId, p =>
                {
                    p.Cpu = Math.Max(p.Cpu - 10, 0);
                    return Task.CompletedTask;
                });

                return new ZoneActionResult(false, "Attack failed. Zone defense too strong. Lost 10 CPU.");
            }
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<ZoneActionResult> ClaimZone(string playerId, string targetZoneId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player == null)
                return new ZoneActionResult(false, "Player not found");

            if (!_zones.TryGetValue(targetZoneId, out var zone))
                return new ZoneActionResult(false, "Zone not found");

            if (zone.ControlLevel > 0)
                return new ZoneActionResult(false, $"Zone still has {zone.ControlLevel}% control remaining. Attack first.");

            var careerBonus = player.CareerPath == "blackhat" ? 10 : 5;
            var creditsReward = 500L + zone.ThreatLevel * 10L;

            await _playerService.WithPlayerActionAsync(playerId, p =>
            {
                p.Credits += creditsReward;
                p.FactionReputations[ZoneFaction.ZeroDay.ToString()] =
                    p.FactionReputations.GetValueOrDefault(ZoneFaction.ZeroDay.ToString()) + careerBonus;
                return Task.CompletedTask;
            });

            zone.ControllingFaction = ZoneFaction.ZeroDay;
            zone.State = ZoneState.Controlled;
            zone.ControlLevel = 20;

            if (_gameEventBus != null)
                await _gameEventBus.Emit(new GameEvent { PlayerId = playerId, Type = AchievementEvent.WORLD_EVENT_PARTICIPATED, Value = zone.ThreatLevel });

            return new ZoneActionResult(true, $"Zone claimed for ZeroDay! Earned {creditsReward} credits.", 0, careerBonus);
        }
        finally
        {
            _mutex.Release();
        }
    }

    public void UpdateZoneState(Zone zone)
    {
        zone.State = zone.ControlLevel switch
        {
            <= 0 => ZoneState.Warzone,
            <= 25 => ZoneState.Contested,
            >= 75 => ZoneState.Controlled,
            _ => ZoneState.Contested
        };
    }

    public ZoneFaction GetActiveFaction()
    {
        var weekMs = 7L * 24 * 60 * 60 * 1000;
        var factions = Enum.GetValues<ZoneFaction>();
        var cycleIndex = (int)((DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() / weekMs) % factions.Length);
        return factions[cycleIndex];
    }

    public Dictionary<string, object> GetActiveFactionCycle()
    {
        var activeFaction = GetActiveFaction();
        var weekMs = 7L * 24 * 60 * 60 * 1000;

        var buffedZoneIds = _zones.Values
            .Where(z => z.ControllingFaction == activeFaction)
            .Select(z => z.Id)
            .ToList();

        return new Dictionary<string, object>
        {
            ["activeFaction"] = activeFaction.ToString(),
            ["displayName"] = activeFaction.ToString(),
            ["bonus"] = $"1.5x reputation gain for {activeFaction} activities",
            ["cycleRemainingMs"] = weekMs - (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() % weekMs),
            ["buffedZoneIds"] = buffedZoneIds,
            ["repMultiplier"] = 1.5,
            ["threatBoost"] = 0.2,
            ["securityReduction"] = 0.15
        };
    }

    public Dictionary<string, object>? GetBuffedZoneSnapshot(string zoneId)
    {
        if (!_zones.TryGetValue(zoneId, out var zone)) return null;

        var active = GetActiveFaction();
        var isActiveFaction = zone.ControllingFaction == active;

        return new Dictionary<string, object>
        {
            ["id"] = zone.Id,
            ["name"] = zone.Name,
            ["controllingFaction"] = zone.ControllingFaction.ToString(),
            ["state"] = zone.State.ToString(),
            ["controlLevel"] = zone.ControlLevel,
            ["maxControlLevel"] = zone.MaxControlLevel,
            ["securityLevel"] = isActiveFaction
                ? Math.Max(1, (int)(zone.SecurityLevel * 0.85))
                : (int)(zone.SecurityLevel * 1.10),
            ["threatLevel"] = isActiveFaction
                ? (int)(zone.ThreatLevel * 1.20)
                : Math.Max(0, (int)(zone.ThreatLevel * 0.90)),
            ["requiredLevel"] = zone.RequiredLevel,
            ["isBuffed"] = isActiveFaction,
            ["hasBoss"] = zone.HasBoss,
            ["bossName"] = zone.BossName,
            ["bossLevel"] = zone.BossLevel
        };
    }

    public async Task<TravelResult> InitiateTravel(string playerId, string targetZoneId)
    {
        var result = await TravelTo(playerId, targetZoneId);
        if (result.Success && result.TravelTimeMs > 100)
        {
            var player = _playerService.GetPlayer(playerId);
            var fromZone = player?.CurrentZoneId ?? "safehouse";

            _travelingPlayers[playerId] = new TravelSession(
                playerId,
                fromZone,
                targetZoneId,
                DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() + result.TravelTimeMs
            );

            return new TravelResult(true, $"Traveling... ETA {result.TravelTimeMs / 1000}s", result.TravelTimeMs);
        }
        return result;
    }

    private async Task ProcessTravelArrivals()
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var arrived = _travelingPlayers
            .Where(kvp => kvp.Value.ArrivalTimeMs <= now)
            .Select(kvp => kvp.Key)
            .ToList();

        foreach (var pid in arrived)
        {
            _travelingPlayers.Remove(pid);
            if (_gameEventBus != null)
                await _gameEventBus.Emit(new GameEvent { PlayerId = pid, Type = AchievementEvent.NODE_DISCOVERED, Value = 1 });
        }
    }
}
