using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class FactionHandler : IHandler
{
    private readonly FactionService _factionService;
    private readonly IPlayerService _playerService;

    public string MessageType => "faction_create";

    public FactionHandler(FactionService factionService, IPlayerService playerService)
    {
        _factionService = factionService;
        _playerService = playerService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var name = payload?.Str("name");
        var tag = payload?.Str("tag");
        var description = payload?.Str("description") ?? "";

        if (string.IsNullOrEmpty(name))
            return new MessageResult("error", connectionId, new { message = "Faction name required" });
        if (string.IsNullOrEmpty(tag))
            return new MessageResult("error", connectionId, new { message = "Faction tag required" });

        try
        {
            var faction = await _factionService.CreateFaction(playerId, name, tag, description);
            return new MessageResult("faction_created", connectionId, new { faction });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleJoin(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        var factionId = payload?.Str("faction_id");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(factionId))
            return new MessageResult("error", connectionId, new { message = "Faction ID required" });

        try
        {
            var faction = await _factionService.JoinFaction(playerId, factionId);
            return new MessageResult("faction_joined", connectionId, new { faction });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleLeave(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        try
        {
            await _factionService.LeaveFaction(playerId);
            return new MessageResult("faction_left", connectionId, new { message = "Left faction" });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleInfo(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        try
        {
            var faction = await _factionService.GetFaction(playerId);
            return new MessageResult("faction_info", connectionId, new { faction });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleList(string connectionId, JsonElement? payload, string? requestId)
    {
        var factions = await _factionService.ListFactions();
        return new MessageResult("faction_list", connectionId, new { factions });
    }

    public async Task<IActionResult> HandleDonate(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var cpu = payload?.Long("cpu") ?? 0L;
        var ram = payload?.Long("ram") ?? 0L;
        var bandwidth = payload?.Long("bandwidth") ?? 0L;
        var credits = payload?.Long("credits") ?? 0L;

        try
        {
            var mainframe = await _factionService.DonateToMainframe(playerId, cpu, ram, bandwidth, credits);
            return new MessageResult("faction_donated", connectionId, new { mainframe });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleUpgrade(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        var upgradeId = payload?.Str("upgrade_id");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(upgradeId))
            return new MessageResult("error", connectionId, new { message = "Upgrade ID required" });

        try
        {
            var mainframe = await _factionService.UpgradeMainframe(playerId, upgradeId);
            return new MessageResult("faction_upgraded", connectionId, new { mainframe });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }
}
