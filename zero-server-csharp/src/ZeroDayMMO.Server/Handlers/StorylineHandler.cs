using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class StorylineHandler : IHandler
{
    private readonly IPlayerService _playerService;

    public string MessageType => "storyline";

    public StorylineHandler(IPlayerService playerService)
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
            "get" => HandleGet(connectionId, payload.Value),
            "start" => HandleStart(connectionId, payload.Value),
            "advance" => HandleAdvance(connectionId, payload.Value),
            _ => Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = $"Unknown storyline action: {action}" }))
        };
    }

    private Task<IActionResult> HandleGet(string connectionId, JsonElement payload)
    {
        var playerId = payload.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId required" }));

        var player = _playerService.GetPlayer(playerId);
        if (player is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Player not found" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("storylines", connectionId, new
            {
                available = new[] { player.CurrentStoryline ?? "intro_welcome" },
                current = player.CurrentStoryline,
                current_stage = player.StorylineProgress
            }));
    }

    private Task<IActionResult> HandleStart(string connectionId, JsonElement payload)
    {
        var playerId = payload.Str("playerId");
        var storylineId = payload.Str("storyline_id");

        if (string.IsNullOrEmpty(playerId) || string.IsNullOrEmpty(storylineId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId and storyline_id required" }));

        var player = _playerService.GetPlayer(playerId);
        if (player is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Player not found" }));

        var updatedPlayer = _playerService.SetStoryline(playerId, storylineId).Result;
        var stage = updatedPlayer?.StorylineProgress ?? 0;

        return Task.FromResult<IActionResult>(
            new MessageResult("storyline_started", connectionId, new
            {
                storyline_id = storylineId,
                current_stage = stage
            }));
    }

    private Task<IActionResult> HandleAdvance(string connectionId, JsonElement payload)
    {
        var playerId = payload.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId required" }));

        var player = _playerService.GetPlayer(playerId);
        if (player is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Player not found" }));

        var advancedPlayer = _playerService.AdvanceStoryline(playerId).Result;
        var advStoryline = advancedPlayer?.CurrentStoryline ?? player.CurrentStoryline;
        var advStage = advancedPlayer?.StorylineProgress ?? player.StorylineProgress;

        return Task.FromResult<IActionResult>(
            new MessageResult("story_advanced", connectionId, new
            {
                storyline_id = advStoryline,
                current_stage = advStage
            }));
    }
}
