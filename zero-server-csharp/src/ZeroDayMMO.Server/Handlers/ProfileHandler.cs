using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;
using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Handlers;

public class ProfileHandler : IHandler
{
    private readonly IPlayerService _playerService;
    private readonly SkillService _skillService;
    private readonly AchievementService _achievementService;

    public string MessageType => "get_profile";

    public ProfileHandler(IPlayerService playerService, SkillService skillService, AchievementService achievementService)
    {
        _playerService = playerService;
        _skillService = skillService;
        _achievementService = achievementService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return new MessageResult("error", connectionId, new { message = "Missing payload" });

        var target = payload.Value.Str("username");
        Player? player;

        if (target is null)
            player = _playerService.GetPlayer(connectionId);
        else
            player = _playerService.GetPlayerByUsername(target);

        if (player is null)
            return new MessageResult("error", connectionId, new { message = "Player not found" });

        var multipliers = _skillService.GetEffectiveMultipliers(player);
        var achievementSummary = _achievementService.SummaryFor(player);
        var unlocked = _achievementService.VisibleTo(player)
            .Where(d => player.UnlockedAchievements.Contains(d.Id))
            .Take(10)
            .Select(d => new { id = d.Id, name = d.Name, category = d.Category.ToString() })
            .ToList();

        var skills = Enum.GetValues<SkillTree>().ToDictionary(
            tree => tree,
            tree => _skillService.ByTree(tree).Count(d => player.UnlockedSkills.Contains(d.Id)));

        var completionPercent = _achievementService.Definitions.Count > 0
            ? (int)((double)player.UnlockedAchievements.Count / _achievementService.Definitions.Count * 100)
            : 0;

        return new MessageResult("profile", connectionId, new
        {
            id = player.Id,
            username = player.Username,
            level = player.Level,
            experience = player.Experience,
            reputation = player.Reputation,
            prestige_level = player.PrestigeLevel,
            prestige_points = player.PrestigePoints,
            faction_id = player.FactionId,
            is_online = player.IsOnline,
            stats = new
            {
                nodes_discovered = player.DiscoveredNodes.Count,
                tasks_completed = player.CompletedTasks.Count,
                storylines_completed = player.CompletedStorylines.Count,
                commands_unlocked = player.UnlockedCommands.Count,
                world_events_joined = player.WorldEventParticipation.Count
            },
            achievements = new
            {
                summary = achievementSummary,
                total_unlocked = player.UnlockedAchievements.Count,
                completion_percent = completionPercent,
                recent = unlocked
            },
            skills = new
            {
                points = player.SkillPoints,
                unlocked_total = player.UnlockedSkills.Count,
                by_tree = skills.ToDictionary(kvp => kvp.Key.ToString(), kvp => kvp.Value)
            },
            multipliers = new
            {
                credits_earned = multipliers.CreditsEarned,
                xp_earned = multipliers.XpEarned,
                resource_cost = multipliers.ResourceCost,
                regen_rate = multipliers.RegenRate
            }
        });
    }
}
