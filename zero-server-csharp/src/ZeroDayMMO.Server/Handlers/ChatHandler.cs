using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class ChatHandler : IHandler
{
    private readonly WebSocketConnectionManager _connectionManager;
    private readonly IPlayerService _playerService;

    public string MessageType => "chat_send";

    public ChatHandler(WebSocketConnectionManager connectionManager, IPlayerService playerService)
    {
        _connectionManager = connectionManager;
        _playerService = playerService;
    }

    public Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
        {
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Missing payload" }));
        }

        var playerId = payload.Value.TryGetProperty("playerId", out var pid) ? pid.GetString() : null;
        var message = payload.Value.TryGetProperty("message", out var msg) ? msg.GetString() : null;

        if (string.IsNullOrEmpty(playerId) || string.IsNullOrEmpty(message))
        {
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId and message required" }));
        }

        var player = _playerService.GetPlayer(playerId);
        var displayName = player?.DisplayName ?? "Unknown";

        var broadcastPayload = new
        {
            from = displayName,
            fromPlayerId = playerId,
            message
        };

        return Task.FromResult<IActionResult>(new BroadcastResult("chat_receive", broadcastPayload, excludeConnectionId: connectionId));
    }
}
