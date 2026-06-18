using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum WorldEventType
{
    [JsonPropertyName("network_outage")]
    NETWORK_OUTAGE,
    [JsonPropertyName("security_alert")]
    SECURITY_ALERT,
    [JsonPropertyName("data_breach")]
    DATA_BREACH,
    [JsonPropertyName("malware_outbreak")]
    MALWARE_OUTBREAK,
    [JsonPropertyName("law_enforcement_crackdown")]
    LAW_ENFORCEMENT_CRACKDOWN,
    [JsonPropertyName("black_market_sale")]
    BLACK_MARKET_SALE,
    [JsonPropertyName("technology_breakthrough")]
    TECHNOLOGY_BREAKTHROUGH,
    [JsonPropertyName("international_conflict")]
    INTERNATIONAL_CONFLICT,
    [JsonPropertyName("solar_flare")]
    SOLAR_FLARE,
    [JsonPropertyName("quantum_supremacy")]
    QUANTUM_SUPREMACY
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum WorldEventSeverity
{
    [JsonPropertyName("minor")]
    MINOR,
    [JsonPropertyName("moderate")]
    MODERATE,
    [JsonPropertyName("major")]
    MAJOR,
    [JsonPropertyName("critical")]
    CRITICAL,
    [JsonPropertyName("catastrophic")]
    CATASTROPHIC
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum EffectType
{
    [JsonPropertyName("xp_multiplier")]
    XP_MULTIPLIER,
    [JsonPropertyName("credit_multiplier")]
    CREDIT_MULTIPLIER,
    [JsonPropertyName("network_speed_boost")]
    NETWORK_SPEED_BOOST,
    [JsonPropertyName("security_level_increase")]
    SECURITY_LEVEL_INCREASE,
    [JsonPropertyName("command_cooldown_reduction")]
    COMMAND_COOLDOWN_REDUCTION,
    [JsonPropertyName("resource_regen_boost")]
    RESOURCE_REGEN_BOOST,
    [JsonPropertyName("drop_rate_increase")]
    DROP_RATE_INCREASE,
    [JsonPropertyName("faction_rep_boost")]
    FACTION_REP_BOOST,
    [JsonPropertyName("network_visibility_boost")]
    NETWORK_VISIBILITY_BOOST,
    [JsonPropertyName("task_reward_boost")]
    TASK_REWARD_BOOST
}

public class WorldEventEffect
{
    [JsonPropertyName("effect_type")]
    public EffectType EffectType { get; set; }

    [JsonPropertyName("value")]
    public double Value { get; set; }

    [JsonPropertyName("duration_ms")]
    public long DurationMs { get; set; } = 0;
}

public class WorldEvent
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString();

    [JsonPropertyName("title")]
    public string Title { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("event_type")]
    public WorldEventType EventType { get; set; }

    [JsonPropertyName("severity")]
    public WorldEventSeverity Severity { get; set; }

    [JsonPropertyName("started_at")]
    public long StartedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [JsonPropertyName("ends_at")]
    public long EndsAt { get; set; } = 0;

    [JsonPropertyName("is_active")]
    public bool IsActive { get; set; } = true;

    [JsonPropertyName("affected_networks")]
    public List<string> AffectedNetworks { get; set; } = new();

    [JsonPropertyName("effects")]
    public List<WorldEventEffect> Effects { get; set; } = new();

    [JsonPropertyName("participating_factions")]
    public List<string> ParticipatingFactions { get; set; } = new();

    [JsonPropertyName("resolution_criteria")]
    public string ResolutionCriteria { get; set; } = "";

    [JsonPropertyName("reward_pool")]
    public long RewardPool { get; set; } = 0;

    [JsonPropertyName("reward_per_participant")]
    public long RewardPerParticipant { get; set; } = 0;
}

public class WorldEventParticipation
{
    [JsonPropertyName("player_id")]
    public string PlayerId { get; set; } = "";

    [JsonPropertyName("faction_id")]
    public string? FactionId { get; set; }

    [JsonPropertyName("contribution_score")]
    public int ContributionScore { get; set; } = 0;

    [JsonPropertyName("rewards_claimed")]
    public bool RewardsClaimed { get; set; } = false;
}

public class WorldEventUpdate
{
    [JsonPropertyName("event_id")]
    public string EventId { get; set; } = "";

    [JsonPropertyName("title")]
    public string Title { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("event_type")]
    public WorldEventType EventType { get; set; }

    [JsonPropertyName("severity")]
    public WorldEventSeverity Severity { get; set; }

    [JsonPropertyName("is_active")]
    public bool IsActive { get; set; }

    [JsonPropertyName("time_remaining_ms")]
    public long TimeRemainingMs { get; set; }

    [JsonPropertyName("effects")]
    public List<WorldEventEffect> Effects { get; set; } = new();

    [JsonPropertyName("participant_count")]
    public int ParticipantCount { get; set; }

    [JsonPropertyName("reward_pool")]
    public long RewardPool { get; set; }
}

public static class WorldEventTemplates
{
    private static readonly List<string> Networks = new()
    {
        "10.0.0.0/24", "10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24",
        "172.16.0.0/16", "192.168.0.0/16", "198.51.100.0/24", "203.0.113.0/24"
    };

    public static List<WorldEvent> EventTemplates { get; } = new()
    {
        new WorldEvent
        {
            Title = "Global Network Slowdown",
            Description = "Internet backbone experiencing unprecedented latency spikes. All network operations slowed.",
            EventType = WorldEventType.NETWORK_OUTAGE,
            Severity = WorldEventSeverity.MAJOR,
            AffectedNetworks = Networks,
            Effects = new()
            {
                new WorldEventEffect { EffectType = EffectType.NETWORK_SPEED_BOOST, Value = -0.5, DurationMs = 300000 },
                new WorldEventEffect { EffectType = EffectType.TASK_REWARD_BOOST, Value = 0.2, DurationMs = 300000 }
            },
            RewardPool = 50000,
            RewardPerParticipant = 500
        },
        new WorldEvent
        {
            Title = "Zero-Day Market Surge",
            Description = "Black market flooded with fresh zero-day exploits. Prices dropped, availability increased.",
            EventType = WorldEventType.BLACK_MARKET_SALE,
            Severity = WorldEventSeverity.MODERATE,
            AffectedNetworks = new() { "172.16.0.0/16" },
            Effects = new()
            {
                new WorldEventEffect { EffectType = EffectType.DROP_RATE_INCREASE, Value = 1.0, DurationMs = 180000 },
                new WorldEventEffect { EffectType = EffectType.XP_MULTIPLIER, Value = 0.3, DurationMs = 180000 }
            },
            RewardPool = 30000,
            RewardPerParticipant = 300
        },
        new WorldEvent
        {
            Title = "ISP Security Crackdown",
            Description = "Law enforcement has partnered with ISPs to increase monitoring and trace efforts.",
            EventType = WorldEventType.LAW_ENFORCEMENT_CRACKDOWN,
            Severity = WorldEventSeverity.MAJOR,
            AffectedNetworks = Networks,
            Effects = new()
            {
                new WorldEventEffect { EffectType = EffectType.SECURITY_LEVEL_INCREASE, Value = 1.0, DurationMs = 600000 },
                new WorldEventEffect { EffectType = EffectType.RESOURCE_REGEN_BOOST, Value = -0.4, DurationMs = 600000 }
            },
            RewardPool = 75000,
            RewardPerParticipant = 750,
            ResolutionCriteria = "Reduce network visibility below 20% for 10 minutes"
        },
        new WorldEvent
        {
            Title = "Quantum Computing Breakthrough",
            Description = "Researchers announce practical quantum computing. Encryption standards shaken.",
            EventType = WorldEventType.TECHNOLOGY_BREAKTHROUGH,
            Severity = WorldEventSeverity.CRITICAL,
            AffectedNetworks = Networks,
            Effects = new()
            {
                new WorldEventEffect { EffectType = EffectType.XP_MULTIPLIER, Value = 1.0, DurationMs = 900000 },
                new WorldEventEffect { EffectType = EffectType.CREDIT_MULTIPLIER, Value = 0.5, DurationMs = 900000 },
                new WorldEventEffect { EffectType = EffectType.DROP_RATE_INCREASE, Value = 1.5, DurationMs = 900000 }
            },
            RewardPool = 100000,
            RewardPerParticipant = 1000
        },
        new WorldEvent
        {
            Title = "Solar Flare Storm",
            Description = "Massive solar flare disrupting satellite communications and power grids.",
            EventType = WorldEventType.SOLAR_FLARE,
            Severity = WorldEventSeverity.MAJOR,
            AffectedNetworks = new() { "10.0.4.0/24", "10.0.5.0/24" },
            Effects = new()
            {
                new WorldEventEffect { EffectType = EffectType.NETWORK_SPEED_BOOST, Value = -0.7, DurationMs = 120000 },
                new WorldEventEffect { EffectType = EffectType.RESOURCE_REGEN_BOOST, Value = -0.6, DurationMs = 120000 }
            },
            RewardPool = 40000,
            RewardPerParticipant = 400
        }
    };
}
