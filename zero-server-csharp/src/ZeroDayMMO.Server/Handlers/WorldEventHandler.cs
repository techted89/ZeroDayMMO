using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class WorldEventHandler : IHandler
{
    private readonly IPlayerService _playerService;
    private readonly INotificationService _notificationService;

    public string MessageType => "world_event";

    public WorldEventHandler(IPlayerService playerService, INotificationService notificationService)
    {
        _playerService = playerService;
        _notificationService = notificationService;
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
            _ => Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = $"Unknown world_event action: {action}" }))
        };
    }

    private Task<IActionResult> HandleList(string connectionId)
    {
        var events = _playerService.GetAllPlayers()
            .SelectMany(p => p.WorldEventParticipation)
            .Distinct()
            .ToList();

        return Task.FromResult<IActionResult>(
            new MessageResult("world_events", connectionId, new { events }));
    }

    private Task<IActionResult> HandleJoin(string connectionId, JsonElement payload)
    {
        var eventId = payload.Str("event_id");
        if (string.IsNullOrEmpty(eventId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "event_id required" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("world_event_joined", connectionId, new { event_id = eventId }));
    }

    private Task<IActionResult> HandleLeave(string connectionId, JsonElement payload)
    {
        var eventId = payload.Str("event_id");
        if (string.IsNullOrEmpty(eventId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "event_id required" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("world_event_left", connectionId, new { message = "Left event" }));
    }
}
