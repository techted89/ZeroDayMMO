using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class PlayerHandler : IHandler
{
    private readonly IPlayerService _playerService;

    public string MessageType => "get_status";

    public PlayerHandler(IPlayerService playerService)
    {
        _playerService = playerService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return new MessageResult("error", connectionId, new { message = "Missing payload" });

        var type = payload.Value.Str("type") ?? "get_status";

        return type switch
        {
            "get_status" => await GetStatus(connectionId),
            "get_online_players" => await GetOnlinePlayers(connectionId),
            "get_leaderboard" => await GetLeaderboard(connectionId),
            "create_party" => CreateParty(connectionId),
            "join_party" => await JoinParty(connectionId, payload.Value),
            _ => new MessageResult("error", connectionId, new { message = $"Unsupported player request: {type}" })
        };
    }

    private async Task<IActionResult> GetStatus(string connectionId)
    {
        var snapshot = _playerService.GetSnapshot(connectionId);
        if (snapshot is null)
            return new MessageResult("error", connectionId, new { message = "Player not found" });

        return new MessageResult("status", connectionId, snapshot.SnapshotToMap());
    }

    private async Task<IActionResult> GetOnlinePlayers(string connectionId)
    {
        var players = _playerService.GetOnlinePlayers()
            .Select(p => p.ToSnapshot().SnapshotToMap())
            .ToList();

        return new MessageResult("online_players", connectionId, new { players });
    }

    private async Task<IActionResult> GetLeaderboard(string connectionId)
    {
        var rows = _playerService.GetAllPlayers()
            .OrderByDescending(p => p.Level)
            .Take(20)
            .Select((p, i) => new
            {
                rank = i + 1,
                username = p.Username,
                level = p.Level,
                reputation = p.Reputation,
                credits = p.Credits
            })
            .ToList();

        return new MessageResult("leaderboard", connectionId, new { leaderboard = rows });
    }

    private IActionResult CreateParty(string connectionId)
    {
        var partyId = $"party_{connectionId[..Math.Min(8, connectionId.Length)]}_{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}";
        return new MessageResult("party_created", connectionId, new { party_id = partyId });
    }

    private async Task<IActionResult> JoinParty(string connectionId, JsonElement payload)
    {
        var partyId = payload.Str("party_id");
        if (string.IsNullOrEmpty(partyId))
            return new MessageResult("error", connectionId, new { message = "Party ID required" });

        await _playerService.SetPartyAsync(connectionId, partyId);
        return new MessageResult("party_joined", connectionId, new { party_id = partyId });
    }
}
