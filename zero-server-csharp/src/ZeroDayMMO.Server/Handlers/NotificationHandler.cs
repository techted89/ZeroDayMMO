using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class NotificationHandler : IHandler
{
    private readonly INotificationService _notificationService;
    private readonly IPlayerService _playerService;

    public string MessageType => "notification";

    public NotificationHandler(INotificationService notificationService, IPlayerService playerService)
    {
        _notificationService = notificationService;
        _playerService = playerService;
    }

    public Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Missing payload" }));

        var action = payload.Value.Str("action");
        return action switch
        {
            "list" => HandleList(connectionId, payload.Value),
            "mark_read" => HandleMarkRead(connectionId, payload.Value),
            "mark_all_read" => HandleMarkAllRead(connectionId, payload.Value),
            _ => Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = $"Unknown notification action: {action}" }))
        };
    }

    private Task<IActionResult> HandleList(string connectionId, JsonElement payload)
    {
        var playerId = payload.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId required" }));

        var player = _playerService.GetPlayer(playerId);
        if (player is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Player not found" }));

        var includeRead = payload.TryGetProperty("include_read", out var ir)
            ? ir.ValueKind != JsonValueKind.False
            : true;
        var limit = payload.Int("limit") ?? 50;

        var notifications = _notificationService.GetNotifications(playerId, includeRead, limit);
        var unreadCount = _notificationService.GetUnreadCount(playerId);

        return Task.FromResult<IActionResult>(
            new MessageResult("notifications", connectionId, new
            {
                notifications,
                unread_count = unreadCount
            }));
    }

    private Task<IActionResult> HandleMarkRead(string connectionId, JsonElement payload)
    {
        var playerId = payload.Str("playerId");
        var notificationId = payload.Str("notification_id");

        if (string.IsNullOrEmpty(playerId) || string.IsNullOrEmpty(notificationId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId and notification_id required" }));

        var player = _playerService.GetPlayer(playerId);
        if (player is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Player not found" }));

        var ok = _notificationService.MarkRead(playerId, notificationId);
        if (!ok)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = $"Unknown notification: {notificationId}" }));

        var unreadCount = _notificationService.GetUnreadCount(playerId);

        return Task.FromResult<IActionResult>(
            new MessageResult("notifications", connectionId, new
            {
                notification_id = notificationId,
                unread_count = unreadCount
            }));
    }

    private Task<IActionResult> HandleMarkAllRead(string connectionId, JsonElement payload)
    {
        var playerId = payload.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId required" }));

        var player = _playerService.GetPlayer(playerId);
        if (player is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Player not found" }));

        var notifications = _notificationService.GetNotifications(playerId, includeRead: false);
        var count = 0;
        foreach (var n in notifications)
        {
            if (_notificationService.MarkRead(playerId, n.Id))
                count++;
        }

        return Task.FromResult<IActionResult>(
            new MessageResult("notifications", connectionId, new
            {
                marked_read = count,
                unread_count = 0
            }));
    }
}
