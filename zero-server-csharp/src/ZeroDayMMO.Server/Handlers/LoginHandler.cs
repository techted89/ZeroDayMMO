using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class LoginHandler : IHandler
{
    private readonly IPlayerService _playerService;

    public string MessageType => "login";

    public LoginHandler(IPlayerService playerService)
    {
        _playerService = playerService;
    }

    public Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
        {
            return Task.FromResult<IActionResult>(
                new MessageResult("login_failure", connectionId, new { message = "Missing payload" }));
        }

        var username = payload.Value.TryGetProperty("username", out var u) ? u.GetString() : null;
        var password = payload.Value.TryGetProperty("password", out var p) ? p.GetString() : null;

        if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password))
        {
            return Task.FromResult<IActionResult>(
                new MessageResult("login_failure", connectionId, new { message = "Username and password required" }));
        }

        var player = _playerService.Authenticate(username, password);
        if (player is null)
        {
            return Task.FromResult<IActionResult>(
                new MessageResult("login_failure", connectionId, new { message = "Invalid credentials" }));
        }

        return Task.FromResult<IActionResult>(
            new MessageResult("login_success", connectionId, new
            {
                playerId = player.Id,
                username = player.Username,
                displayName = player.DisplayName,
                level = player.Level
            }));
    }
}
