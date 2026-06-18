using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class GameEventHandler : IHandler
{
    private readonly IPlayerService _playerService;

    public string MessageType => "event";

    public GameEventHandler(IPlayerService playerService)
    {
        _playerService = playerService;
    }

    public Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Missing payload" }));

        var action = payload.Value.Str("action");
        return action switch
        {
            "list" => HandleList(connectionId),
            "join" => HandleJoin(connectionId, payload.Value),
            "leave" => HandleLeave(connectionId, payload.Value),
            "leaderboard" => HandleLeaderboard(connectionId, payload.Value),
            "score" => HandleScore(connectionId, payload.Value),
            "my_events" => HandleMyEvents(connectionId, payload.Value),
            _ => Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = $"Unknown event action: {action}" }))
        };
    }

    private static Task<IActionResult> HandleList(string connectionId)
    {
        return Task.FromResult<IActionResult>(
            new MessageResult("event_list", connectionId, new
            {
                events = Array.Empty<object>(),
                count = 0
            }));
    }

    private Task<IActionResult> HandleJoin(string connectionId, JsonElement payload)
    {
        var eventId = payload.Str("eventId");
        if (string.IsNullOrEmpty(eventId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "eventId required" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("event_joined", connectionId, new { eventId }));
    }

    private Task<IActionResult> HandleLeave(string connectionId, JsonElement payload)
    {
        var eventId = payload.Str("eventId");
        if (string.IsNullOrEmpty(eventId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "eventId required" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("event_left", connectionId, new { eventId }));
    }

    private static Task<IActionResult> HandleLeaderboard(string connectionId, JsonElement payload)
    {
        var eventId = payload.Str("eventId");
        if (string.IsNullOrEmpty(eventId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "eventId required" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("event_leaderboard", connectionId, new
            {
                eventId,
                participants = Array.Empty<object>()
            }));
    }

    private static Task<IActionResult> HandleScore(string connectionId, JsonElement payload)
    {
        var eventId = payload.Str("eventId");
        var delta = payload.Long("delta");

        if (string.IsNullOrEmpty(eventId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "eventId required" }));
        if (delta is null || delta <= 0)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "delta must be positive" }));

        var playerId = payload.Str("playerId");

        return Task.FromResult<IActionResult>(
            new MessageResult("event_score_updated", connectionId, new
            {
                eventId,
                playerId,
                delta
            }));
    }

    private static Task<IActionResult> HandleMyEvents(string connectionId, JsonElement payload)
    {
        return Task.FromResult<IActionResult>(
            new MessageResult("event_my_events", connectionId, new
            {
                events = Array.Empty<object>(),
                count = 0
            }));
    }
}
