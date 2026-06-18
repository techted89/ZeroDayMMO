using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public enum ChallengeCadence
{
    Daily,
    Weekly
}

public record ChallengeDefinition(
    string Id,
    string Name,
    string Description,
    ChallengeCadence Cadence,
    AchievementEvent Event,
    long TargetValue,
    long RewardCredits = 0,
    long RewardXp = 0,
    int RewardReputation = 0
);

public class ChallengeService
{
    private readonly SemaphoreSlim _mutex = new(1, 1);

    public List<ChallengeDefinition> Catalog { get; } = new()
    {
        new("daily_discover_3", "Recon Day", "Discover 3 nodes.",
            ChallengeCadence.Daily, AchievementEvent.NODE_DISCOVERED, 3, 200, 100),
        new("daily_tasks_2", "Contractor", "Complete 2 tasks.",
            ChallengeCadence.Daily, AchievementEvent.TASK_COMPLETED, 2, 500, 250),
        new("daily_credits_1000", "Money Maker", "Earn 1,000 credits.",
            ChallengeCadence.Daily, AchievementEvent.CREDITS_EARNED, 1000, 100, 200),
        new("daily_fragments_2", "Knowledge Seeker", "Gather 2 knowledge fragments.",
            ChallengeCadence.Daily, AchievementEvent.FRAGMENT_GATHERED, 2, 300, 150),
        new("weekly_tasks_15", "Pro Contractor", "Complete 15 tasks this week.",
            ChallengeCadence.Weekly, AchievementEvent.TASK_COMPLETED, 15, 5000, 2000, 10),
        new("weekly_discover_25", "Cartographer", "Discover 25 nodes this week.",
            ChallengeCadence.Weekly, AchievementEvent.NODE_DISCOVERED, 25, 3000, 1500),
        new("weekly_storyline_1", "Plot Twist", "Complete a storyline this week.",
            ChallengeCadence.Weekly, AchievementEvent.STORYLINE_COMPLETED, 1, 2000, 1000, 5)
    };

    private readonly Dictionary<string, ChallengeDefinition> _byId;

    public ChallengeService()
    {
        _byId = Catalog.ToDictionary(c => c.Id);
    }

    public ChallengeDefinition? Get(string id) =>
        _byId.GetValueOrDefault(id);

    public async Task<List<ActiveChallenge>> Record(Player player, AchievementEvent eventType, long delta = 1)
    {
        await _mutex.WaitAsync();
        try
        {
            RotateIfNeeded(player);
            var completed = new List<ActiveChallenge>();
            foreach (var challenge in player.ActiveChallenges)
            {
                if (challenge.Completed) continue;
                if (!_byId.TryGetValue(challenge.ChallengeId, out var def)) continue;
                if (def.Event != eventType) continue;

                challenge.CurrentValue = Math.Min(challenge.CurrentValue + delta, def.TargetValue);
                if (challenge.CurrentValue >= def.TargetValue)
                {
                    challenge.Completed = true;
                    challenge.CompletedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                    completed.Add(challenge);
                }
            }
            return completed;
        }
        finally
        {
            _mutex.Release();
        }
    }

    public void RotateIfNeeded(Player player)
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        if (player.LastChallengeRotation == 0)
        {
            player.ActiveChallenges.Clear();
            player.ActiveChallenges.AddRange(PickInitial());
            player.LastChallengeRotation = now;
            return;
        }

        var (dailies, weeklies) = Partition(player);
        var dailyExpired = dailies.Count > 0 && dailies.All(c => c.ExpiresAt <= now);
        var weeklyExpired = weeklies.Count > 0 && weeklies.All(c => c.ExpiresAt <= now);

        if (dailyExpired)
        {
            player.ActiveChallenges.RemoveAll(c =>
                _byId.TryGetValue(c.ChallengeId, out var def) && def.Cadence == ChallengeCadence.Daily);
            player.ActiveChallenges.AddRange(PickDailySet(now));
        }
        if (weeklyExpired)
        {
            player.ActiveChallenges.RemoveAll(c =>
                _byId.TryGetValue(c.ChallengeId, out var def) && def.Cadence == ChallengeCadence.Weekly);
            player.ActiveChallenges.AddRange(PickWeeklySet(now));
        }
        player.LastChallengeRotation = now;
    }

    private (List<ActiveChallenge> Daily, List<ActiveChallenge> Weekly) Partition(Player player)
    {
        var daily = new List<ActiveChallenge>();
        var weekly = new List<ActiveChallenge>();
        foreach (var c in player.ActiveChallenges)
        {
            if (!_byId.TryGetValue(c.ChallengeId, out var def)) continue;
            switch (def.Cadence)
            {
                case ChallengeCadence.Daily: daily.Add(c); break;
                case ChallengeCadence.Weekly: weekly.Add(c); break;
            }
        }
        return (daily, weekly);
    }

    private List<ActiveChallenge> PickInitial()
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var result = PickDailySet(now);
        result.AddRange(PickWeeklySet(now));
        return result;
    }

    private List<ActiveChallenge> PickDailySet(long now)
    {
        var dailies = Catalog.Where(c => c.Cadence == ChallengeCadence.Daily).ToList();
        var picks = dailies.OrderBy(_ => Random.Shared.Next()).Take(3).ToList();
        var expires = now + TimeSpan.FromDays(1).TotalMilliseconds;
        return picks.Select(c => new ActiveChallenge { ChallengeId = c.Id, AssignedAt = now, ExpiresAt = (long)expires }).ToList();
    }

    private List<ActiveChallenge> PickWeeklySet(long now)
    {
        var weeklies = Catalog.Where(c => c.Cadence == ChallengeCadence.Weekly).ToList();
        var picks = weeklies.OrderBy(_ => Random.Shared.Next()).Take(2).ToList();
        var expires = now + TimeSpan.FromDays(7).TotalMilliseconds;
        return picks.Select(c => new ActiveChallenge { ChallengeId = c.Id, AssignedAt = now, ExpiresAt = (long)expires }).ToList();
    }

    public async Task ForceRotate(Player player)
    {
        await _mutex.WaitAsync();
        try
        {
            player.ActiveChallenges.Clear();
            player.LastChallengeRotation = 0;
            RotateIfNeeded(player);
        }
        finally
        {
            _mutex.Release();
        }
    }
}
