using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum ChallengeCadence
{
    [JsonPropertyName("daily")]
    DAILY,
    [JsonPropertyName("weekly")]
    WEEKLY
}

public class ChallengeDefinition
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("cadence")]
    public ChallengeCadence Cadence { get; set; }

    [JsonPropertyName("event")]
    public AchievementEvent Event { get; set; }

    [JsonPropertyName("target_value")]
    public long TargetValue { get; set; }

    [JsonPropertyName("reward_credits")]
    public long RewardCredits { get; set; } = 0;

    [JsonPropertyName("reward_xp")]
    public long RewardXp { get; set; } = 0;

    [JsonPropertyName("reward_reputation")]
    public int RewardReputation { get; set; } = 0;
}

public class ActiveChallenge
{
    [JsonPropertyName("challenge_id")]
    public string ChallengeId { get; set; } = "";

    [JsonPropertyName("assigned_at")]
    public long AssignedAt { get; set; }

    [JsonPropertyName("expires_at")]
    public long ExpiresAt { get; set; }

    [JsonPropertyName("current_value")]
    public long CurrentValue { get; set; } = 0;

    [JsonPropertyName("completed")]
    public bool Completed { get; set; } = false;

    [JsonPropertyName("completed_at")]
    public long? CompletedAt { get; set; }

    [JsonPropertyName("claimed")]
    public bool Claimed { get; set; } = false;

    public int Percent(long target)
    {
        if (target == 0) return 100;
        return Math.Clamp((int)((double)CurrentValue / target * 100), 0, 100);
    }
}
