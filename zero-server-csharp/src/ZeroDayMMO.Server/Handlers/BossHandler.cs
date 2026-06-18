using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class BossHandler : IHandler
{
    private readonly CoopBossService _coopBossService;
    private readonly IPlayerService _playerService;

    public string MessageType => "boss_create";

    public BossHandler(CoopBossService coopBossService, IPlayerService playerService)
    {
        _coopBossService = coopBossService;
        _playerService = playerService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var zoneId = payload?.Str("zoneId");
        if (string.IsNullOrEmpty(zoneId))
            return new MessageResult("error", connectionId, new { message = "zoneId required" });

        try
        {
            var instance = await _coopBossService.CreateInstance(zoneId, playerId);
            return new MessageResult("boss_instance_created", connectionId, _coopBossService.SerializeInstance(instance));
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleJoin(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var instanceId = payload?.Str("instanceId");
        if (string.IsNullOrEmpty(instanceId))
            return new MessageResult("error", connectionId, new { message = "instanceId required" });

        try
        {
            var instance = await _coopBossService.JoinInstance(instanceId, playerId);
            return new MessageResult("boss_instance_joined", connectionId, _coopBossService.SerializeInstance(instance));
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleAction(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var instanceId = payload?.Str("instanceId");
        var action = payload?.Str("action");

        if (string.IsNullOrEmpty(instanceId))
            return new MessageResult("error", connectionId, new { message = "instanceId required" });
        if (string.IsNullOrEmpty(action))
            return new MessageResult("error", connectionId, new { message = "action required (attack/scan/defend/heal)" });

        try
        {
            var result = await _coopBossService.PerformAction(instanceId, playerId, action);
            return new MessageResult("boss_action_result", connectionId, new
            {
                action = result.Action.Action,
                message = result.Action.Message,
                damage = result.Action.Damage,
                healAmount = result.Action.HealAmount,
                isCrit = result.Action.IsCrit,
                isDodged = result.Action.IsDodged,
                bossPhase = result.BossPhase.ToString(),
                bossHp = result.BossHp,
                bossMaxHp = result.BossMaxHp,
                phaseChanged = result.PhaseChanged,
                abilityName = result.AbilityName,
                abilityDamage = result.AbilityDamage,
                knockedOut = result.KnockedOut ?? "",
                fightOver = result.FightOver,
                result = result.Result ?? ""
            });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleStatus(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var instanceId = payload?.Str("instanceId");

        try
        {
            CoopBossService.BossInstance? instance;
            if (!string.IsNullOrEmpty(instanceId))
            {
                instance = await _coopBossService.GetInstance(instanceId);
            }
            else
            {
                instance = await _coopBossService.GetPlayerActiveInstance(playerId);
            }

            if (instance is not null)
                return new MessageResult("boss_status", connectionId, _coopBossService.SerializeInstance(instance));
            return new MessageResult("error", connectionId, new { message = "No active boss instance found" });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleListAvailable(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        var zoneId = payload?.Str("zoneId");
        if (string.IsNullOrEmpty(zoneId))
            return new MessageResult("error", connectionId, new { message = "zoneId required" });

        try
        {
            var instances = await _coopBossService.GetAvailableInstances(zoneId);
            return new MessageResult("boss_available_instances", connectionId, new
            {
                instances = instances.Select(i => _coopBossService.SerializeInstance(i)),
                count = instances.Count
            });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }
}
