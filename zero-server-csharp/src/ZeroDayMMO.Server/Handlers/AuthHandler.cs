using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class AuthHandler : IHandler
{
    private readonly IPlayerService _playerService;

    public string MessageType => "auth";

    public AuthHandler(IPlayerService playerService)
    {
        _playerService = playerService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return new MessageResult("error", connectionId, new { message = "Missing payload" });

        var action = payload.Value.Str("action");
        return action switch
        {
            "register" => await HandleRegister(connectionId, payload.Value),
            "login" => await HandleLogin(connectionId, payload.Value),
            "logout" => await HandleLogout(connectionId),
            "ping" => await HandlePing(connectionId),
            _ => new MessageResult("error", connectionId, new { message = $"Unknown auth action: {action}" })
        };
    }

    private async Task<IActionResult> HandleRegister(string connectionId, JsonElement payload)
    {
        var username = payload.Str("username");
        var password = payload.Str("password");

        if (string.IsNullOrEmpty(username))
            return new MessageResult("error", connectionId, new { message = "Username required" });
        if (string.IsNullOrEmpty(password))
            return new MessageResult("error", connectionId, new { message = "Password required" });

        try
        {
            var player = await _playerService.Register(username, password);

            return new MessageResult("register_success", connectionId, new
            {
                player = new
                {
                    id = player.Id,
                    username = player.Username,
                    level = player.Level
                },
                message = $"Account created. Welcome, {player.Username}!"
            });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    private Task<IActionResult> HandleLogin(string connectionId, JsonElement payload)
    {
        var username = payload.Str("username");
        var password = payload.Str("password");

        if (string.IsNullOrEmpty(username))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Username required" }));
        if (string.IsNullOrEmpty(password))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Password required" }));

        var player = _playerService.Authenticate(username, password);
        if (player is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Invalid credentials" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("login_success", connectionId, new
            {
                player = new
                {
                    id = player.Id,
                    username = player.Username,
                    level = player.Level,
                    cpu = player.Cpu,
                    ram = player.Ram,
                    credits = player.Credits,
                    experience = player.Experience,
                    experience_to_next = player.ExperienceToNext
                },
                message = $"Welcome back, {player.Username}! You are level {player.Level}.",
                login_streak = 0
            }));
    }

    private static Task<IActionResult> HandleLogout(string connectionId)
    {
        return Task.FromResult<IActionResult>(
            new MessageResult("logout_success", connectionId, new { message = "Logged out" }));
    }

    private static Task<IActionResult> HandlePing(string connectionId)
    {
        return Task.FromResult<IActionResult>(
            new MessageResult("pong", connectionId, new { timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() }));
    }
}
