using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class KMapHandler : IHandler
{
    private readonly HackToLearnService _hackToLearnService;

    public string MessageType => "kmap";

    public KMapHandler(HackToLearnService hackToLearnService)
    {
        _hackToLearnService = hackToLearnService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return new MessageResult("error", connectionId, new { message = "Missing payload" });

        var action = payload.Value.Str("action");
        return action switch
        {
            "get" => await HandleGet(connectionId, payload.Value),
            "discover" => await HandleDiscover(connectionId, payload.Value),
            "fragments" => await HandleFragments(connectionId, payload.Value),
            _ => new MessageResult("error", connectionId, new { message = $"Unknown kmap action: {action}" })
        };
    }

    private async Task<IActionResult> HandleGet(string connectionId, JsonElement payload)
    {
        var playerId = payload.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var kmap = await _hackToLearnService.GetKnowledgeMap(playerId);

        return new MessageResult("knowledge_map", connectionId, new { kmap });
    }

    private async Task<IActionResult> HandleDiscover(string connectionId, JsonElement payload)
    {
        var playerId = payload.Str("playerId");
        var fragmentId = payload.Str("fragment_id");

        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(fragmentId))
            return new MessageResult("error", connectionId, new { message = "Fragment ID required" });

        try
        {
            var fragment = await _hackToLearnService.DiscoverFragment(playerId, fragmentId);
            var msg = $"Discovered: {fragment.Name}";
            if (fragment.UnlocksCommand is not null)
                msg += $" - Unlocked command: {fragment.UnlocksCommand}";

            return new MessageResult("fragment_discovered", connectionId, new
            {
                fragment,
                message = msg
            });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    private async Task<IActionResult> HandleFragments(string connectionId, JsonElement payload)
    {
        var playerId = payload.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var fragments = await _hackToLearnService.GetAvailableFragments(playerId);

        return new MessageResult("available_fragments", connectionId, new { fragments });
    }
}
