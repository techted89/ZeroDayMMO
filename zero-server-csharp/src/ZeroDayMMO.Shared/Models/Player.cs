using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

public class Player
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString();

    [JsonPropertyName("username")]
    public string Username { get; set; } = "";

    [JsonPropertyName("password_hash")]
    public string PasswordHash { get; set; } = "";

    [JsonPropertyName("level")]
    public int Level { get; set; } = 1;

    [JsonPropertyName("experience")]
    public long Experience { get; set; } = 0;

    [JsonPropertyName("experience_to_next")]
    public long ExperienceToNext { get; set; } = 100;

    [JsonPropertyName("cpu")]
    public int Cpu { get; set; } = 100;

    [JsonPropertyName("max_cpu")]
    public int MaxCpu { get; set; } = 100;

    [JsonPropertyName("ram")]
    public int Ram { get; set; } = 256;

    [JsonPropertyName("max_ram")]
    public int MaxRam { get; set; } = 256;

    [JsonPropertyName("bandwidth")]
    public int Bandwidth { get; set; } = 50;

    [JsonPropertyName("max_bandwidth")]
    public int MaxBandwidth { get; set; } = 50;

    [JsonPropertyName("credits")]
    public long Credits { get; set; } = 0;

    [JsonPropertyName("reputation")]
    public int Reputation { get; set; } = 0;

    [JsonPropertyName("prestige_level")]
    public int PrestigeLevel { get; set; } = 0;

    [JsonPropertyName("prestige_points")]
    public long PrestigePoints { get; set; } = 0;

    [JsonPropertyName("unlocked_commands")]
    public HashSet<string> UnlockedCommands { get; set; } = new() { "help", "scan", "connect", "whoami", "status" };

    [JsonPropertyName("discovered_nodes")]
    public HashSet<string> DiscoveredNodes { get; set; } = new() { "localhost" };

    [JsonPropertyName("active_tasks")]
    public List<TaskInstance> ActiveTasks { get; set; } = new();

    [JsonPropertyName("completed_tasks")]
    public List<string> CompletedTasks { get; set; } = new();

    [JsonPropertyName("completed_storylines")]
    public List<string> CompletedStorylines { get; set; } = new();

    [JsonPropertyName("is_banned")]
    public bool IsBanned { get; set; } = false;

    [JsonPropertyName("ban_reason")]
    public string? BanReason { get; set; }

    [JsonPropertyName("current_storyline")]
    public string? CurrentStoryline { get; set; }

    [JsonPropertyName("storyline_progress")]
    public int StorylineProgress { get; set; } = 0;

    [JsonPropertyName("last_login_ip")]
    public string LastLoginIp { get; set; } = "127.0.0.1";

    [JsonPropertyName("is_online")]
    public bool IsOnline { get; set; } = false;

    [JsonPropertyName("party_id")]
    public string? PartyId { get; set; }

    [JsonPropertyName("faction_id")]
    public string? FactionId { get; set; }

    [JsonPropertyName("inventory")]
    public List<InventoryItem> Inventory { get; set; } = new();

    [JsonPropertyName("active_zero_day_exploits")]
    public List<InventoryItem> ActiveZeroDayExploits { get; set; } = new();

    [JsonPropertyName("firewall_boost")]
    public int FirewallBoost { get; set; } = 0;

    [JsonPropertyName("world_event_participation")]
    public HashSet<string> WorldEventParticipation { get; set; } = new();

    [JsonPropertyName("achievement_progress")]
    public Dictionary<string, AchievementProgress> AchievementProgress { get; set; } = new();

    [JsonPropertyName("unlocked_achievements")]
    public HashSet<string> UnlockedAchievements { get; set; } = new();

    [JsonPropertyName("skill_points")]
    public int SkillPoints { get; set; } = 0;

    [JsonPropertyName("unlocked_skills")]
    public HashSet<string> UnlockedSkills { get; set; } = new();

    [JsonPropertyName("notifications")]
    public List<Notification> Notifications { get; set; } = new();

    [JsonPropertyName("last_challenge_rotation")]
    public long LastChallengeRotation { get; set; } = 0;

    [JsonPropertyName("active_challenges")]
    public List<ActiveChallenge> ActiveChallenges { get; set; } = new();

    [JsonPropertyName("last_level_notified")]
    public int LastLevelNotified { get; set; } = 0;

    [JsonPropertyName("lifetime_credits_earned")]
    public long LifetimeCreditsEarned { get; set; } = 0;

    [JsonPropertyName("career_path")]
    public string CareerPath { get; set; } = "undecided";

    [JsonPropertyName("heat_level")]
    public int HeatLevel { get; set; } = 0;

    [JsonPropertyName("max_heat_level")]
    public int MaxHeatLevel { get; set; } = 100;

    [JsonPropertyName("notoriety")]
    public int Notoriety { get; set; } = 0;

    [JsonPropertyName("justice_points")]
    public int JusticePoints { get; set; } = 0;

    [JsonPropertyName("bounty_price")]
    public long BountyPrice { get; set; } = 0;

    [JsonPropertyName("times_arrested")]
    public int TimesArrested { get; set; } = 0;

    [JsonPropertyName("is_in_jail")]
    public bool IsInJail { get; set; } = false;

    [JsonPropertyName("jail_time_remaining")]
    public float JailTimeRemaining { get; set; } = 0;

    [JsonPropertyName("white_hat_rank")]
    public int WhiteHatRank { get; set; } = 0;

    [JsonPropertyName("black_hat_rank")]
    public int BlackHatRank { get; set; } = 0;

    [JsonPropertyName("successful_heists")]
    public int SuccessfulHeists { get; set; } = 0;

    [JsonPropertyName("traces_covered")]
    public int TracesCovered { get; set; } = 0;

    [JsonPropertyName("current_zone_id")]
    public string CurrentZoneId { get; set; } = "safehouse";

    [JsonPropertyName("zone_control_contributions")]
    public Dictionary<string, int> ZoneControlContributions { get; set; } = new();

    [JsonPropertyName("faction_reputations")]
    public Dictionary<string, int> FactionReputations { get; set; } = new();

    [JsonPropertyName("last_login_at")]
    public long LastLoginAt { get; set; } = 0;

    [JsonPropertyName("login_streak")]
    public int LoginStreak { get; set; } = 0;

    [JsonPropertyName("longest_streak")]
    public int LongestStreak { get; set; } = 0;

    [JsonPropertyName("total_logins")]
    public int TotalLogins { get; set; } = 0;

    [JsonPropertyName("equipment_durability")]
    public Dictionary<string, int> EquipmentDurability { get; set; } = new();

    [JsonPropertyName("mastery_gems")]
    public List<string> MasteryGems { get; set; } = new();

    [JsonPropertyName("breakthrough_levels")]
    public List<int> BreakthroughLevels { get; set; } = new();

    [JsonPropertyName("breakthrough_multiplier")]
    public double BreakthroughMultiplier { get; set; } = 1.0;

    public PlayerSnapshot ToSnapshot() => new()
    {
        Id = Id,
        Username = Username,
        Level = Level,
        Experience = Experience,
        ExperienceToNext = ExperienceToNext,
        Cpu = Cpu,
        MaxCpu = MaxCpu,
        Ram = Ram,
        MaxRam = MaxRam,
        Bandwidth = Bandwidth,
        MaxBandwidth = MaxBandwidth,
        Credits = Credits,
        Reputation = Reputation,
        PrestigeLevel = PrestigeLevel,
        PrestigePoints = PrestigePoints,
        UnlockedCommands = new HashSet<string>(UnlockedCommands),
        DiscoveredNodesCount = DiscoveredNodes.Count,
        ActiveTasks = new List<TaskInstance>(ActiveTasks),
        CompletedTasksCount = CompletedTasks.Count,
        CurrentStoryline = CurrentStoryline,
        StorylineProgress = StorylineProgress,
        FactionId = FactionId,
        Inventory = new List<InventoryItem>(Inventory),
        ActiveZeroDayExploits = ActiveZeroDayExploits.Count,
        FirewallBoost = FirewallBoost,
        WorldEventParticipation = WorldEventParticipation.Count,
        UnlockedAchievementCount = UnlockedAchievements.Count,
        SkillPoints = SkillPoints,
        UnlockedSkillCount = UnlockedSkills.Count,
        UnreadNotifications = Notifications.Count(n => !n.Read),
        ActiveChallengeCount = ActiveChallenges.Count,
        CareerPath = CareerPath,
        HeatLevel = HeatLevel,
        Notoriety = Notoriety,
        JusticePoints = JusticePoints,
        BountyPrice = BountyPrice,
        CurrentZoneId = CurrentZoneId,
        BreakthroughMultiplier = BreakthroughMultiplier,
        LoginStreak = LoginStreak,
        TotalLogins = TotalLogins
    };
}

public class PlayerSnapshot
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("username")]
    public string Username { get; set; } = "";

    [JsonPropertyName("level")]
    public int Level { get; set; }

    [JsonPropertyName("experience")]
    public long Experience { get; set; }

    [JsonPropertyName("experience_to_next")]
    public long ExperienceToNext { get; set; }

    [JsonPropertyName("cpu")]
    public int Cpu { get; set; }

    [JsonPropertyName("max_cpu")]
    public int MaxCpu { get; set; }

    [JsonPropertyName("ram")]
    public int Ram { get; set; }

    [JsonPropertyName("max_ram")]
    public int MaxRam { get; set; }

    [JsonPropertyName("bandwidth")]
    public int Bandwidth { get; set; }

    [JsonPropertyName("max_bandwidth")]
    public int MaxBandwidth { get; set; }

    [JsonPropertyName("credits")]
    public long Credits { get; set; }

    [JsonPropertyName("reputation")]
    public int Reputation { get; set; }

    [JsonPropertyName("prestige_level")]
    public int PrestigeLevel { get; set; }

    [JsonPropertyName("prestige_points")]
    public long PrestigePoints { get; set; }

    [JsonPropertyName("unlocked_commands")]
    public HashSet<string> UnlockedCommands { get; set; } = new();

    [JsonPropertyName("discovered_nodes_count")]
    public int DiscoveredNodesCount { get; set; }

    [JsonPropertyName("active_tasks")]
    public List<TaskInstance> ActiveTasks { get; set; } = new();

    [JsonPropertyName("completed_tasks_count")]
    public int CompletedTasksCount { get; set; }

    [JsonPropertyName("current_storyline")]
    public string? CurrentStoryline { get; set; }

    [JsonPropertyName("storyline_progress")]
    public int StorylineProgress { get; set; }

    [JsonPropertyName("faction_id")]
    public string? FactionId { get; set; }

    [JsonPropertyName("inventory")]
    public List<InventoryItem> Inventory { get; set; } = new();

    [JsonPropertyName("active_zero_day_exploits")]
    public int ActiveZeroDayExploits { get; set; }

    [JsonPropertyName("firewall_boost")]
    public int FirewallBoost { get; set; }

    [JsonPropertyName("world_event_participation")]
    public int WorldEventParticipation { get; set; }

    [JsonPropertyName("unlocked_achievement_count")]
    public int UnlockedAchievementCount { get; set; }

    [JsonPropertyName("skill_points")]
    public int SkillPoints { get; set; }

    [JsonPropertyName("unlocked_skill_count")]
    public int UnlockedSkillCount { get; set; }

    [JsonPropertyName("unread_notifications")]
    public int UnreadNotifications { get; set; }

    [JsonPropertyName("active_challenge_count")]
    public int ActiveChallengeCount { get; set; }

    [JsonPropertyName("career_path")]
    public string CareerPath { get; set; } = "";

    [JsonPropertyName("heat_level")]
    public int HeatLevel { get; set; }

    [JsonPropertyName("notoriety")]
    public int Notoriety { get; set; }

    [JsonPropertyName("justice_points")]
    public int JusticePoints { get; set; }

    [JsonPropertyName("bounty_price")]
    public long BountyPrice { get; set; }

    [JsonPropertyName("current_zone_id")]
    public string CurrentZoneId { get; set; } = "";

    [JsonPropertyName("breakthrough_multiplier")]
    public double BreakthroughMultiplier { get; set; }

    [JsonPropertyName("login_streak")]
    public int LoginStreak { get; set; }

    [JsonPropertyName("total_logins")]
    public int TotalLogins { get; set; }

    public Dictionary<string, object?> SnapshotToMap() => new()
    {
        ["id"] = Id,
        ["username"] = Username,
        ["level"] = Level,
        ["experience"] = Experience,
        ["experienceToNext"] = ExperienceToNext,
        ["cpu"] = Cpu,
        ["maxCpu"] = MaxCpu,
        ["ram"] = Ram,
        ["maxRam"] = MaxRam,
        ["bandwidth"] = Bandwidth,
        ["maxBandwidth"] = MaxBandwidth,
        ["credits"] = Credits,
        ["reputation"] = Reputation,
        ["prestigeLevel"] = PrestigeLevel,
        ["prestigePoints"] = PrestigePoints,
        ["unlockedCommands"] = UnlockedCommands.ToList(),
        ["discoveredNodesCount"] = DiscoveredNodesCount,
        ["activeTasks"] = ActiveTasks.Select(t => t.ToMap()).ToList(),
        ["completedTasksCount"] = CompletedTasksCount,
        ["currentStoryline"] = CurrentStoryline,
        ["storylineProgress"] = StorylineProgress,
        ["factionId"] = FactionId,
        ["inventory"] = Inventory.Select(i => i.ToMap()).ToList(),
        ["activeZeroDayExploits"] = ActiveZeroDayExploits,
        ["firewallBoost"] = FirewallBoost,
        ["worldEventParticipation"] = WorldEventParticipation,
        ["unlockedAchievementCount"] = UnlockedAchievementCount,
        ["skillPoints"] = SkillPoints,
        ["unlockedSkillCount"] = UnlockedSkillCount,
        ["unreadNotifications"] = UnreadNotifications,
        ["activeChallengeCount"] = ActiveChallengeCount,
        ["careerPath"] = CareerPath,
        ["heatLevel"] = HeatLevel,
        ["notoriety"] = Notoriety,
        ["justicePoints"] = JusticePoints,
        ["bountyPrice"] = BountyPrice,
        ["currentZoneId"] = CurrentZoneId,
        ["breakthroughMultiplier"] = BreakthroughMultiplier,
        ["loginStreak"] = LoginStreak,
        ["totalLogins"] = TotalLogins
    };
}
