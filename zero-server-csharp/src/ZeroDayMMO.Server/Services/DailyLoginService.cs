using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public class DailyLoginService
{
    private readonly PlayerService _playerService;
    private readonly NotificationService? _notificationService;
    private readonly ChallengeService? _challengeService;

    public const long MaxOfflineMillis = 28_800_000L;
    public const long MinOfflineMillis = 3_600_000L;
    public const int OfflineCpuPerHour = 2;
    public const int OfflineRamPerHour = 4;
    public const long OfflineCreditsPerHourPerLevel = 15;
    public const long StreakWindowMillis = 57_600_000L;
    public const long DayMillis = 86_400_000L;

    public DailyLoginService(
        PlayerService playerService,
        NotificationService? notificationService = null,
        ChallengeService? challengeService = null)
    {
        _playerService = playerService;
        _notificationService = notificationService;
        _challengeService = challengeService;
    }

    public static StreakReward DailyStreakReward(int streakDay)
    {
        var day = ((streakDay - 1) % 7) + 1;
        return day switch
        {
            1 => new StreakReward(100, 50),
            2 => new StreakReward(200, 100),
            3 => new StreakReward(350, 175),
            4 => new StreakReward(500, 250),
            5 => new StreakReward(750, 400),
            6 => new StreakReward(1000, 600),
            7 => new StreakReward(2000, 1000, 5),
            _ => new StreakReward(100, 50)
        };
    }

    public async Task<LoginResult?> ProcessLogin(string playerId, string? ipAddress = null)
    {
        var result = await _playerService.WithPlayerAction<LoginResult>(playerId, player =>
        {
            var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            var offline = CalculateOffline(player, now);
            var streakResult = UpdateStreak(player, now);
            var reward = streakResult.Reward;

            if (offline != null)
            {
                player.Cpu = Math.Min(player.Cpu + offline.CpuGained, player.MaxCpu);
                player.Ram = Math.Min(player.Ram + offline.RamGained, player.MaxRam);
                player.Credits += offline.CreditsGained;
                player.LifetimeCreditsEarned += offline.CreditsGained;
            }

            if (reward != null)
            {
                player.Credits += reward.Credits;
                player.LifetimeCreditsEarned += reward.Credits;
                player.Experience += reward.Xp;
                player.Reputation += reward.Reputation;
                CheckForLevelUp(player);
            }

            player.LastLoginAt = now;
            player.TotalLogins++;
            if (streakResult.IsNewStreak) player.LoginStreak = streakResult.Streak;
            if (player.LoginStreak > player.LongestStreak) player.LongestStreak = player.LoginStreak;
            player.LastLoginIp = ipAddress ?? player.LastLoginIp;

            _challengeService?.RotateIfNeeded(player);

            var parts = new List<string>();
            if (offline != null && offline.CreditsGained > 0)
            {
                parts.Add($"While you were away (+{offline.OfflineHours:F1}h): +{offline.CreditsGained}cr, +{offline.CpuGained}CPU, +{offline.RamGained}RAM");
            }
            if (reward != null)
            {
                var repStr = reward.Reputation > 0 ? $", +{reward.Reputation} rep" : "";
                parts.Add($"Day {streakResult.Streak} streak reward: +{reward.Credits}cr, +{reward.Xp}xp{repStr}");
                parts.Add($"Next reward in: day {(streakResult.Streak % 7) + 1}");
            }

            var message = parts.Count > 0 ? string.Join("\n", parts) : "Welcome back!";

            if (reward != null)
            {
                _notificationService?.AddNotification(player.Id, new Notification
                {
                    Type = NotificationType.DAILY_LOGIN,
                    Title = $"Daily Login Streak: Day {streakResult.Streak}",
                    Message = $"You earned +{reward.Credits}cr, +{reward.Xp}xp{(reward.Reputation > 0 ? $" and +{reward.Reputation} rep" : "")} for logging in today!",
                    Data = new Dictionary<string, string>
                    {
                        ["streak"] = streakResult.Streak.ToString(),
                        ["credits"] = reward.Credits.ToString(),
                        ["xp"] = reward.Xp.ToString(),
                        ["reputation"] = reward.Reputation.ToString()
                    }
                });
            }

            return new LoginResult(
                streakResult.Streak,
                streakResult.IsNewStreak,
                reward,
                offline,
                message
            );
        });

        return result ?? new LoginResult(0, false, null, null, "Player not found");
    }

    public OfflineAccumulation? CalculateOffline(Player player, long? nowMs = null)
    {
        var now = nowMs ?? DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        if (player.LastLoginAt <= 0L) return null;

        var elapsed = now - player.LastLoginAt;
        if (elapsed < MinOfflineMillis) return null;

        var capped = Math.Min(elapsed, MaxOfflineMillis);
        var hours = capped / 3_600_000.0;

        return new OfflineAccumulation(
            Math.Max(0, (int)(hours * OfflineCpuPerHour)),
            Math.Max(0, (int)(hours * OfflineRamPerHour)),
            Math.Max(0, (long)(hours * player.Level * OfflineCreditsPerHourPerLevel)),
            hours
        );
    }

    private StreakResult UpdateStreak(Player player, long now)
    {
        var sinceLast = now - player.LastLoginAt;

        if (player.LastLoginAt <= 0L)
            return new StreakResult(1, true, DailyStreakReward(1));

        if (sinceLast < StreakWindowMillis)
            return new StreakResult(Math.Max(1, player.LoginStreak), false, null);

        var maxGap = StreakWindowMillis + DayMillis;
        if (sinceLast < maxGap)
        {
            var newStreak = player.LoginStreak + 1;
            return new StreakResult(newStreak, true, DailyStreakReward(newStreak));
        }

        return new StreakResult(1, true, DailyStreakReward(1));
    }

    private static void CheckForLevelUp(Player player)
    {
        var leveledUp = false;
        var reachedLevel = player.Level;

        while (player.Experience >= player.ExperienceToNext)
        {
            player.Experience -= player.ExperienceToNext;
            player.Level++;
            player.ExperienceToNext = (long)(player.ExperienceToNext * 1.5);
            player.MaxCpu += 5;
            player.MaxRam += 16;
            player.MaxBandwidth += 2;
            player.Cpu = player.MaxCpu;
            player.Ram = player.MaxRam;
            player.Bandwidth = player.MaxBandwidth;
            player.SkillPoints += player.Level % 5 == 0 ? 2 : 1;
            leveledUp = true;
            reachedLevel = player.Level;
        }

        if (leveledUp) player.LastLevelNotified = reachedLevel;
    }

    public record StreakReward(long Credits = 0, long Xp = 0, int Reputation = 0);

    public record LoginResult(
        int Streak,
        bool IsNewStreak,
        StreakReward? StreakReward,
        OfflineAccumulation? OfflineReward,
        string Message
    );

    public record OfflineAccumulation(
        int CpuGained,
        int RamGained,
        long CreditsGained,
        double OfflineHours
    );

    private record StreakResult(int Streak, bool IsNewStreak, StreakReward? Reward);
}
