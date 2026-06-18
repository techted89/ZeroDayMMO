using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class CareerHandler : IHandler
{
    private readonly CareerService _careerService;
    private readonly HeatCascadeService _heatCascadeService;

    public string MessageType => "career_choose";

    public CareerHandler(CareerService careerService, HeatCascadeService heatCascadeService)
    {
        _careerService = careerService;
        _heatCascadeService = heatCascadeService;
    }

    public Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId required" }));

        var career = payload?.Str("career");
        if (string.IsNullOrEmpty(career))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Career type required (whitehat/blackhat)" }));

        var result = _careerService.ChooseCareer(playerId, career.ToLowerInvariant());
        if (result.Success)
            return Task.FromResult<IActionResult>(new MessageResult("career_chosen", connectionId, new
            {
                career = career.ToLowerInvariant(),
                message = result.Message,
                justiceDelta = result.JusticeDelta,
                heatDelta = result.HeatDelta
            }));
        return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = result.Message }));
    }

    public Task<IActionResult> HandleStatus(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId required" }));

        var report = _careerService.GetCareerReport(playerId);
        return Task.FromResult<IActionResult>(new MessageResult("career_status", connectionId, report));
    }

    public Task<IActionResult> HandleAddHeat(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId required" }));

        var amount = payload?.Int("amount");
        if (amount is null || amount.Value <= 0)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Amount required" }));

        var reason = payload?.Str("reason") ?? "";
        var result = _careerService.AddHeat(playerId, amount.Value, reason);
        if (result.Success)
            return Task.FromResult<IActionResult>(new MessageResult("heat_updated", connectionId, new
            {
                heatDelta = amount.Value,
                message = result.Message
            }));
        return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = result.Message }));
    }

    public Task<IActionResult> HandleArrest(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "playerId required" }));

        var targetId = payload?.Str("targetId");
        if (string.IsNullOrEmpty(targetId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Target player ID required" }));

        var result = _careerService.AttemptArrest(playerId, targetId);
        if (result.Success)
            return Task.FromResult<IActionResult>(new MessageResult("arrest_success", connectionId, new
            {
                message = result.Message,
                justiceDelta = result.JusticeDelta
            }));
        return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = result.Message }));
    }

    public async Task<IActionResult> HandleHunterStatus(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        try
        {
            var hunters = await _heatCascadeService.GetActiveHunters();
            var isHunted = await _heatCascadeService.IsPlayerHunted(playerId);
            var relevantHunters = hunters.Where(h => h.TargetId == playerId).ToList();

            return new MessageResult("hunter_status", connectionId, new
            {
                hunters = relevantHunters.Select(h => new
                {
                    hunterId = h.HunterId,
                    targetName = h.TargetName,
                    targetHeat = h.TargetHeat,
                    attemptCount = h.AttemptCount
                }),
                isHunted
            });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }
}
