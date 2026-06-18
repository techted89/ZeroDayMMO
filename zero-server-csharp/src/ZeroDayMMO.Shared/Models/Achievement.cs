using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum AchievementCategory
{
    [JsonPropertyName("combat")]
    COMBAT,
    [JsonPropertyName("progression")]
    PROGRESSION,
    [JsonPropertyName("economy")]
    ECONOMY,
    [JsonPropertyName("exploration")]
    EXPLORATION,
    [JsonPropertyName("social")]
    SOCIAL,
    [JsonPropertyName("scripting")]
    SCRIPTING,
    [JsonPropertyName("world_event")]
    WORLD_EVENT,
    [JsonPropertyName("meta")]
    META
}

public class AchievementDefinition
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("category")]
    public AchievementCategory Category { get; set; }

    [JsonPropertyName("target_value")]
    public long TargetValue { get; set; }

    [JsonPropertyName("reward_credits")]
    public long RewardCredits { get; set; } = 0;

    [JsonPropertyName("reward_reputation")]
    public int RewardReputation { get; set; } = 0;

    [JsonPropertyName("reward_xp")]
    public long RewardXp { get; set; } = 0;

    [JsonPropertyName("hidden")]
    public bool Hidden { get; set; } = false;
}

public class AchievementProgress
{
    [JsonPropertyName("achievement_id")]
    public string AchievementId { get; set; } = "";

    [JsonPropertyName("current_value")]
    public long CurrentValue { get; set; } = 0;

    [JsonPropertyName("completed")]
    public bool Completed { get; set; } = false;

    [JsonPropertyName("completed_at")]
    public long? CompletedAt { get; set; }

    public int Percent(AchievementDefinition definition)
    {
        if (definition.TargetValue == 0) return 100;
        return Math.Clamp((int)((double)CurrentValue / definition.TargetValue * 100), 0, 100);
    }
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum AchievementEvent
{
    [JsonPropertyName("node_discovered")]
    NODE_DISCOVERED,
    [JsonPropertyName("network_compromised")]
    NETWORK_COMPROMISED,
    [JsonPropertyName("command_executed")]
    COMMAND_EXECUTED,
    [JsonPropertyName("task_completed")]
    TASK_COMPLETED,
    [JsonPropertyName("storyline_completed")]
    STORYLINE_COMPLETED,
    [JsonPropertyName("level_reached")]
    LEVEL_REACHED,
    [JsonPropertyName("credits_earned")]
    CREDITS_EARNED,
    [JsonPropertyName("credits_spent")]
    CREDITS_SPENT,
    [JsonPropertyName("fragment_gathered")]
    FRAGMENT_GATHERED,
    [JsonPropertyName("script_saved")]
    SCRIPT_SAVED,
    [JsonPropertyName("faction_joined")]
    FACTION_JOINED,
    [JsonPropertyName("world_event_participated")]
    WORLD_EVENT_PARTICIPATED,
    [JsonPropertyName("command_unlocked")]
    COMMAND_UNLOCKED,
    [JsonPropertyName("career_chosen")]
    CAREER_CHOSEN,
    [JsonPropertyName("heat_changed")]
    HEAT_CHANGED,
    [JsonPropertyName("justice_changed")]
    JUSTICE_CHANGED,
    [JsonPropertyName("arrest_executed")]
    ARREST_EXECUTED,
    [JsonPropertyName("bounty_placed")]
    BOUNTY_PLACED,
    [JsonPropertyName("item_crafted")]
    ITEM_CRAFTED
}

public class AchievementUpdate
{
    [JsonPropertyName("achievement_id")]
    public string AchievementId { get; set; } = "";

    [JsonPropertyName("new_value")]
    public long NewValue { get; set; }

    [JsonPropertyName("completed")]
    public bool Completed { get; set; }

    [JsonPropertyName("rewards_awarded")]
    public AchievementRewards RewardsAwarded { get; set; } = new();
}

public class AchievementRewards
{
    [JsonPropertyName("credits")]
    public long Credits { get; set; }

    [JsonPropertyName("reputation")]
    public int Reputation { get; set; }

    [JsonPropertyName("xp")]
    public long Xp { get; set; }

    public static AchievementRewards NONE => new() { Credits = 0, Reputation = 0, Xp = 0 };
}
