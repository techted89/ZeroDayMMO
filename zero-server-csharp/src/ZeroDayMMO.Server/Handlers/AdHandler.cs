using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class AdHandler : IHandler
{
    private readonly IPlayerService _playerService;

    public string MessageType => "watch_ad";

    public AdHandler(IPlayerService playerService)
    {
        _playerService = playerService;
    }

    public Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Missing payload" }));

        var playerId = payload.Value.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId required" }));

        var player = _playerService.GetPlayer(playerId);
        if (player is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Player not found" }));

        var cpuReward = 5;
        var creditsReward = 100L;

        player.Cpu = Math.Min(player.Cpu + cpuReward, player.MaxCpu);
        player.Credits += creditsReward;
        _playerService.UpdatePlayer(player);

        return Task.FromResult<IActionResult>(
            new MessageResult("ad_reward", connectionId, new
            {
                rewards = new
                {
                    cpu = cpuReward,
                    credits = creditsReward
                },
                message = "Ad watched successfully! You earned rewards."
            }));
    }
}
