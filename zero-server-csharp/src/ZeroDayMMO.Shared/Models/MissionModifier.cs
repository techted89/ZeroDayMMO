using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum ModifierType
{
    [JsonPropertyName("time_limit")]
    TIME_LIMIT,
    [JsonPropertyName("stealth_required")]
    STEALTH_REQUIRED,
    [JsonPropertyName("no_detection")]
    NO_DETECTION,
    [JsonPropertyName("speed_run")]
    SPEED_RUN,
    [JsonPropertyName("ghost_mode")]
    GHOST_MODE,
    [JsonPropertyName("limited_resources")]
    LIMITED_RESOURCES,
    [JsonPropertyName("boosted_rewards")]
    BOOSTED_REWARDS,
    [JsonPropertyName("reduced_rewards")]
    REDUCED_REWARDS,
    [JsonPropertyName("increased_difficulty")]
    INCREASED_DIFFICULTY,
    [JsonPropertyName("decreased_difficulty")]
    DECREASED_DIFFICULTY,
    [JsonPropertyName("special_objective")]
    SPECIAL_OBJECTIVE,
    [JsonPropertyName("team_required")]
    TEAM_REQUIRED,
    [JsonPropertyName("solo_only")]
    SOLO_ONLY
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum ModifierRarity
{
    [JsonPropertyName("common")]
    COMMON,
    [JsonPropertyName("uncommon")]
    UNCOMMON,
    [JsonPropertyName("rare")]
    RARE,
    [JsonPropertyName("epic")]
    EPIC,
    [JsonPropertyName("legendary")]
    LEGENDARY
}

public class MissionModifier
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("modifier_type")]
    public ModifierType ModifierType { get; set; }

    [JsonPropertyName("value")]
    public double Value { get; set; }

    [JsonPropertyName("rarity")]
    public ModifierRarity Rarity { get; set; }

    [JsonPropertyName("duration_ms")]
    public long DurationMs { get; set; } = 0;

    [JsonPropertyName("stacking")]
    public bool Stacking { get; set; } = false;
}

public class EnhancedContract
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString();

    [JsonPropertyName("title")]
    public string Title { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("target_type")]
    public TaskTargetType TargetType { get; set; }

    [JsonPropertyName("target_ip")]
    public string TargetIp { get; set; } = "";

    [JsonPropertyName("required_commands")]
    public List<string> RequiredCommands { get; set; } = new();

    [JsonPropertyName("difficulty")]
    public TaskDifficulty Difficulty { get; set; }

    [JsonPropertyName("suggested_level")]
    public int SuggestedLevel { get; set; } = 1;

    [JsonPropertyName("base_xp")]
    public long BaseXp { get; set; } = 0;

    [JsonPropertyName("base_credits")]
    public long BaseCredits { get; set; } = 0;

    [JsonPropertyName("modifiers")]
    public List<MissionModifier> Modifiers { get; set; } = new();

    [JsonPropertyName("final_reward_multiplier")]
    public double FinalRewardMultiplier { get; set; } = 1.0;

    [JsonPropertyName("final_xp")]
    public long FinalXp { get; set; } = 0;

    [JsonPropertyName("final_credits")]
    public long FinalCredits { get; set; } = 0;

    [JsonPropertyName("created_at")]
    public long CreatedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [JsonPropertyName("time_limit_ms")]
    public long TimeLimitMs { get; set; } = 600000;
}

public static class MissionModifiers
{
    public static MissionModifier TimeLimited => new()
    {
        Id = "time_limited", Name = "Time Limited", Description = "Complete within reduced time",
        ModifierType = ModifierType.TIME_LIMIT, Value = 0.7, Rarity = ModifierRarity.UNCOMMON
    };

    public static MissionModifier StealthRequired => new()
    {
        Id = "stealth_required", Name = "Stealth Required", Description = "Avoid detection systems",
        ModifierType = ModifierType.STEALTH_REQUIRED, Value = 1.0, Rarity = ModifierRarity.RARE
    };

    public static MissionModifier NoDetection => new()
    {
        Id = "no_detection", Name = "Ghost Mode", Description = "Leave no traces behind",
        ModifierType = ModifierType.NO_DETECTION, Value = 1.0, Rarity = ModifierRarity.EPIC
    };

    public static MissionModifier SpeedRun => new()
    {
        Id = "speed_run", Name = "Speed Run", Description = "2x rewards for completing under half time",
        ModifierType = ModifierType.SPEED_RUN, Value = 2.0, Rarity = ModifierRarity.RARE
    };

    public static MissionModifier LimitedResources => new()
    {
        Id = "limited_resources", Name = "Limited Resources", Description = "Reduced starting resources",
        ModifierType = ModifierType.LIMITED_RESOURCES, Value = 0.5, Rarity = ModifierRarity.UNCOMMON
    };

    public static MissionModifier BoostedRewards => new()
    {
        Id = "boosted_rewards", Name = "Boosted Rewards", Description = "Increased payout for risk",
        ModifierType = ModifierType.BOOSTED_REWARDS, Value = 1.5, Rarity = ModifierRarity.UNCOMMON
    };

    public static MissionModifier ReducedRewards => new()
    {
        Id = "reduced_rewards", Name = "Reduced Rewards", Description = "Lower payout for ease",
        ModifierType = ModifierType.REDUCED_REWARDS, Value = 0.7, Rarity = ModifierRarity.COMMON
    };

    public static MissionModifier IncreasedDifficulty => new()
    {
        Id = "increased_difficulty", Name = "Increased Difficulty", Description = "Enhanced security measures",
        ModifierType = ModifierType.INCREASED_DIFFICULTY, Value = 1.0, Rarity = ModifierRarity.RARE
    };

    public static MissionModifier DecreasedDifficulty => new()
    {
        Id = "decreased_difficulty", Name = "Decreased Difficulty", Description = "Weakened defenses",
        ModifierType = ModifierType.DECREASED_DIFFICULTY, Value = 1.0, Rarity = ModifierRarity.UNCOMMON
    };

    public static MissionModifier SpecialObjective => new()
    {
        Id = "special_objective", Name = "Special Objective", Description = "Additional objective required",
        ModifierType = ModifierType.SPECIAL_OBJECTIVE, Value = 1.0, Rarity = ModifierRarity.RARE
    };

    public static MissionModifier TeamRequired => new()
    {
        Id = "team_required", Name = "Team Required", Description = "Must complete with 2+ players",
        ModifierType = ModifierType.TEAM_REQUIRED, Value = 1.0, Rarity = ModifierRarity.RARE
    };

    public static MissionModifier SoloOnly => new()
    {
        Id = "solo_only", Name = "Solo Only", Description = "Must be completed alone",
        ModifierType = ModifierType.SOLO_ONLY, Value = 1.0, Rarity = ModifierRarity.UNCOMMON
    };

    public static List<MissionModifier> AllModifiers { get; } = new()
    {
        TimeLimited, StealthRequired, NoDetection, SpeedRun,
        LimitedResources, BoostedRewards, ReducedRewards,
        IncreasedDifficulty, DecreasedDifficulty, SpecialObjective,
        TeamRequired, SoloOnly
    };

    public static List<MissionModifier> GetRandomModifier(int count = 1)
    {
        var rng = new Random();
        var available = AllModifiers;
        var takeCount = Math.Min(count, available.Count);
        return Enumerable.Range(0, takeCount)
            .Select(_ => available[rng.Next(available.Count)])
            .Distinct()
            .ToList();
    }
}
