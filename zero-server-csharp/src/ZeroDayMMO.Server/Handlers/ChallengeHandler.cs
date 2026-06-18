using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;
using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Handlers;

public class ChallengeHandler : IHandler
{
    private readonly IPlayerService _playerService;
    private readonly ChallengeService _challengeService;
    private readonly INotificationService _notificationService;

    public string MessageType => "get_challenges";

    public ChallengeHandler(
        IPlayerService playerService,
        ChallengeService challengeService,
        INotificationService notificationService)
    {
        _playerService = playerService;
        _challengeService = challengeService;
        _notificationService = notificationService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return new MessageResult("error", connectionId, new { message = "Missing payload" });

        var type = payload.Value.Str("type") ?? "get_challenges";

        return type switch
        {
            "get_challenges" => await List(connectionId),
            "claim_challenge" => await Claim(connectionId, payload.Value),
            "rotate_challenges" => await Rotate(connectionId),
            _ => new MessageResult("error", connectionId, new { message = $"Unsupported challenge request: {type}" })
        };
    }

    private async Task<IActionResult> List(string connectionId)
    {
        var player = _playerService.GetPlayer(connectionId);
        if (player is null)
            return new MessageResult("error", connectionId, new { message = "Player not found" });

        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        var rows = player.ActiveChallenges
            .Where(c => _challengeService.Get(c.ChallengeId) != null)
            .Select(c =>
        {
            var def = _challengeService.Get(c.ChallengeId)!;
            return new
            {
                id = c.ChallengeId,
                name = def.Name,
                description = def.Description,
                cadence = def.Cadence.ToString(),
                target = def.TargetValue,
                current = c.CurrentValue,
                percent = def.TargetValue == 0 ? 100 : (int)Math.Clamp(c.CurrentValue * 100 / def.TargetValue, 0, 100),
                completed = c.Completed,
                claimed = c.Claimed,
                expires_at = c.ExpiresAt,
                expires_in_ms = Math.Max(0, c.ExpiresAt - now),
                rewards = new
                {
                    credits = def.RewardCredits,
                    xp = def.RewardXp,
                    reputation = def.RewardReputation
                }
            };
        }).ToList();

        return new MessageResult("challenges", connectionId, new
        {
            challenges = rows,
            last_rotation = player.LastChallengeRotation
        });
    }

    private async Task<IActionResult> Claim(string connectionId, JsonElement payload)
    {
        string challengeId;
        try
        {
            challengeId = payload.ReqStr("challenge_id");
        }
        catch
        {
            return new MessageResult("error", connectionId, new { message = "Challenge ID required" });
        }

        var player = _playerService.GetPlayer(connectionId);
        if (player is null)
            return new MessageResult("error", connectionId, new { message = "Player not found" });

        var active = player.ActiveChallenges.FirstOrDefault(c => c.ChallengeId == challengeId);
        if (active is null)
            return new MessageResult("error", connectionId, new { message = $"Unknown active challenge: {challengeId}" });

        if (!active.Completed)
            return new MessageResult("error", connectionId, new { message = "Challenge not yet completed" });

        if (active.Claimed)
        {
            return new MessageResult("challenge_completed", connectionId, new
            {
                challenge_id = challengeId,
                already_claimed = true,
                rewards = new { }
            });
        }

        var def = _challengeService.Get(challengeId);
        if (def is null)
            return new MessageResult("error", connectionId, new { message = $"Unknown challenge definition: {challengeId}" });

        active.Claimed = true;

        await _playerService.AddCreditsAsync(connectionId, def.RewardCredits);
        await _playerService.AddReputationAsync(connectionId, def.RewardReputation);
        await _playerService.AddExperienceAsync(connectionId, def.RewardXp);

        _notificationService.AddNotification(connectionId, new Notification
        {
            Type = NotificationType.REWARD_GRANTED,
            Title = $"Challenge complete: {def.Name}",
            Message = $"You earned {def.RewardCredits} credits and {def.RewardXp} XP.",
            Data = new Dictionary<string, string>
            {
                { "challenge_id", def.Id },
                { "credits", def.RewardCredits.ToString() }
            }
        });

        return new MessageResult("challenge_completed", connectionId, new
        {
            challenge_id = challengeId,
            rewards = new
            {
                credits = def.RewardCredits,
                xp = def.RewardXp,
                reputation = def.RewardReputation
            }
        });
    }

    private async Task<IActionResult> Rotate(string connectionId)
    {
        var player = _playerService.GetPlayer(connectionId);
        if (player is null)
            return new MessageResult("error", connectionId, new { message = "Player not found" });

        await _challengeService.ForceRotate(player);

        return new MessageResult("challenge_rotated", connectionId, new
        {
            challenges = player.ActiveChallenges.Select(c => c.ChallengeId).ToList()
        });
    }
}
