using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class ZoneHandler : IHandler
{
    private readonly WorldZoneService _worldZoneService;
    private readonly IPlayerService _playerService;

    public string MessageType => "zone_info";

    public ZoneHandler(WorldZoneService worldZoneService, IPlayerService playerService)
    {
        _worldZoneService = worldZoneService;
        _playerService = playerService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        var zoneId = payload?.Str("zoneId");
        if (string.IsNullOrEmpty(zoneId))
            return new MessageResult("error", connectionId, new { message = "zoneId required" });

        var snapshot = await _worldZoneService.GetZoneSnapshot(zoneId);
        if (snapshot is null)
            return new MessageResult("error", connectionId, new { message = "Zone not found" });

        return new MessageResult("zone_info", connectionId, new { zone = snapshot });
    }

    public async Task<IActionResult> HandleZoneList(string connectionId, JsonElement? payload, string? requestId)
    {
        var zones = await _worldZoneService.GetZoneSnapshots();
        return new MessageResult("zone_list", connectionId, new { zones });
    }

    public async Task<IActionResult> HandleZoneTravel(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        var zoneId = payload?.Str("zoneId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(zoneId))
            return new MessageResult("error", connectionId, new { message = "zoneId required" });

        var result = await _worldZoneService.InitiateTravel(playerId, zoneId);
        if (result.Success)
            return new MessageResult("zone_travel_started", connectionId, new
            {
                message = result.Message,
                travelTimeMs = result.TravelTimeMs
            });
        return new MessageResult("error", connectionId, new { message = result.Message });
    }

    public async Task<IActionResult> HandleZoneAttack(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        var zoneId = payload?.Str("zoneId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(zoneId))
            return new MessageResult("error", connectionId, new { message = "zoneId required" });

        var result = await _worldZoneService.AttackZone(playerId, zoneId);
        if (result.Success)
            return new MessageResult("zone_attack_result", connectionId, new
            {
                message = result.Message,
                threatGained = result.ThreatGained,
                repGained = result.RepGained
            });
        return new MessageResult("error", connectionId, new { message = result.Message });
    }

    public async Task<IActionResult> HandleZoneClaim(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        var zoneId = payload?.Str("zoneId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(zoneId))
            return new MessageResult("error", connectionId, new { message = "zoneId required" });

        var result = await _worldZoneService.ClaimZone(playerId, zoneId);
        if (result.Success)
            return new MessageResult("zone_claimed", connectionId, new { message = result.Message });
        return new MessageResult("error", connectionId, new { message = result.Message });
    }

    public IActionResult HandleFactionCycle(string connectionId, JsonElement? payload, string? requestId)
    {
        var cycle = _worldZoneService.GetActiveFactionCycle();
        return new MessageResult("faction_cycle", connectionId, cycle);
    }
}
