using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class GameHandler : IHandler
{
    private readonly IPlayerService _playerService;

    public string MessageType => "game_action";

    public GameHandler(IPlayerService playerService)
    {
        _playerService = playerService;
    }

    public Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
        {
            return Task.FromResult<IActionResult>(
                new MessageResult("error", connectionId, new { message = "Missing payload" }));
        }

        var playerId = payload.Value.TryGetProperty("playerId", out var pid) ? pid.GetString() : null;
        var action = payload.Value.TryGetProperty("action", out var act) ? act.GetString() : null;

        if (string.IsNullOrEmpty(playerId))
        {
            return Task.FromResult<IActionResult>(
                new MessageResult("error", connectionId, new { message = "playerId required" }));
        }

        var player = _playerService.GetPlayer(playerId);
        if (player is null)
        {
            return Task.FromResult<IActionResult>(
                new MessageResult("error", connectionId, new { message = "Player not found" }));
        }

        return Task.FromResult<IActionResult>(
            new MessageResult("game_result", connectionId, new
            {
                playerId = player.Id,
                action,
                status = "ok",
                cpu = player.Cpu,
                ram = player.Ram,
                network = player.Network,
                level = player.Level,
                experience = player.Experience
            }));
    }
}
