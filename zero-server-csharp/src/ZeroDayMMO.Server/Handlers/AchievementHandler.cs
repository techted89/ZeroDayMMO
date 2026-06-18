using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;
using ZeroDayMMO.Shared.Models;
using AchievementDefinition = ZeroDayMMO.Server.Services.AchievementDefinition;

namespace ZeroDayMMO.Server.Handlers;

public class AchievementHandler : IHandler
{
    private readonly IPlayerService _playerService;
    private readonly AchievementService _achievementService;
    private readonly INotificationService _notificationService;

    public string MessageType => "get_achievements";

    public AchievementHandler(
        IPlayerService playerService,
        AchievementService achievementService,
        INotificationService notificationService)
    {
        _playerService = playerService;
        _achievementService = achievementService;
        _notificationService = notificationService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return new MessageResult("error", connectionId, new { message = "Missing payload" });

        var type = payload.Value.Str("type") ?? "get_achievements";

        return type switch
        {
            "get_achievements" => await List(connectionId),
            "claim_achievement" => await Claim(connectionId, payload.Value),
            _ => new MessageResult("error", connectionId, new { message = $"Unsupported achievement request: {type}" })
        };
    }

    private async Task<IActionResult> List(string connectionId)
    {
        var player = _playerService.GetPlayer(connectionId);
        if (player is null)
            return new MessageResult("error", connectionId, new { message = "Player not found" });

        var visible = _achievementService.VisibleTo(player);
        var progressFor = player.AchievementProgress;

        var rows = visible.Select(def =>
        {
            var progress = progressFor.GetValueOrDefault(def.Id);
            var percent = progress is null ? 0 :
                def.TargetValue == 0 ? 100 :
                (int)Math.Clamp(progress.CurrentValue * 100 / def.TargetValue, 0, 100);

            return new
            {
                id = def.Id,
                name = def.Name,
                description = def.Description,
                category = def.Category.ToString(),
                target = def.TargetValue,
                current = progress?.CurrentValue ?? 0L,
                percent,
                completed = progress?.Completed == true,
                unlocked_at = progress?.CompletedAt,
                reward_credits = def.RewardCredits,
                reward_reputation = def.RewardReputation,
                reward_xp = def.RewardXp,
                claimed = player.UnlockedAchievements.Contains(def.Id)
            };
        }).ToList();

        var unread = player.AchievementProgress.Count(kvp => kvp.Value.Completed) - player.UnlockedAchievements.Count;

        return new MessageResult("achievements", connectionId, new
        {
            achievements = rows,
            summary = _achievementService.SummaryFor(player),
            unread_achievements = Math.Max(0, unread)
        });
    }

    private async Task<IActionResult> Claim(string connectionId, JsonElement payload)
    {
        string achievementId;
        try
        {
            achievementId = payload.ReqStr("achievement_id");
        }
        catch
        {
            return new MessageResult("error", connectionId, new { message = "Achievement ID required" });
        }

        var player = _playerService.GetPlayer(connectionId);
        if (player is null)
            return new MessageResult("error", connectionId, new { message = "Player not found" });

        var def = _achievementService.Get(achievementId);
        if (def is null)
            return new MessageResult("error", connectionId, new { message = $"Unknown achievement: {achievementId}" });

        var progress = player.AchievementProgress.GetValueOrDefault(achievementId);
        if (progress?.Completed != true)
            return new MessageResult("error", connectionId, new { message = "Achievement not yet completed" });

        if (player.UnlockedAchievements.Contains(achievementId))
        {
            return new MessageResult("achievement_unlocked", connectionId, new
            {
                achievement_id = achievementId,
                name = def.Name,
                rewards = new { credits = 0L, reputation = 0, xp = 0L },
                already_claimed = true
            });
        }

        player.UnlockedAchievements.Add(achievementId);

        await _playerService.AddCreditsAsync(connectionId, def.RewardCredits);
        await _playerService.AddReputationAsync(connectionId, def.RewardReputation);
        await _playerService.AddExperienceAsync(connectionId, def.RewardXp);

        _notificationService.AddNotification(connectionId, new Notification
        {
            Type = NotificationType.ACHIEVEMENT_UNLOCKED,
            Title = $"Achievement unlocked: {def.Name}",
            Message = def.Description,
            Data = new Dictionary<string, string> { { "achievement_id", def.Id } }
        });

        return new MessageResult("achievement_unlocked", connectionId, new
        {
            achievement_id = achievementId,
            name = def.Name,
            rewards = new
            {
                credits = def.RewardCredits,
                reputation = def.RewardReputation,
                xp = def.RewardXp
            }
        });
    }

    public async Task<List<ZeroDayMMO.Server.Services.AchievementUpdate>> Emit(
        string playerId,
        AchievementEvent eventType,
        long delta = 1)
    {
        var player = _playerService.GetPlayer(playerId);
        if (player is null)
            return new List<ZeroDayMMO.Server.Services.AchievementUpdate>();

        var updates = _achievementService.Record(player, eventType, delta);

        foreach (var update in updates.Where(u => u.Completed))
        {
                                var def = _achievementService.Get(update.AchievementId) as ZeroDayMMO.Server.Services.AchievementDefinition;
            if (def is null) continue;

            _notificationService.AddNotification(playerId, new Notification
            {
                Type = NotificationType.ACHIEVEMENT_UNLOCKED,
                Title = $"Achievement unlocked: {def.Name}",
                Message = def.Description,
                Data = new Dictionary<string, string>
                {
                    { "achievement_id", def.Id },
                    { "unclaimed", "true" }
                }
            });
        }

        return updates;
    }
}
