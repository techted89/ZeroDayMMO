using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

public class Faction
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString();

    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("tag")]
    public string Tag { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("leader_id")]
    public string LeaderId { get; set; } = "";

    [JsonPropertyName("members")]
    public List<string> Members { get; set; } = new();

    [JsonPropertyName("join_requests")]
    public List<string> JoinRequests { get; set; } = new();

    [JsonPropertyName("level")]
    public int Level { get; set; } = 1;

    [JsonPropertyName("experience")]
    public long Experience { get; set; } = 0;

    [JsonPropertyName("mainframe")]
    public FactionMainframe Mainframe { get; set; } = new();

    [JsonPropertyName("created_at")]
    public long CreatedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [JsonPropertyName("is_open")]
    public bool IsOpen { get; set; } = true;
}

public class FactionMainframe
{
    [JsonPropertyName("level")]
    public int Level { get; set; } = 1;

    [JsonPropertyName("cpu_total")]
    public long CpuTotal { get; set; } = 1000;

    [JsonPropertyName("cpu_used")]
    public long CpuUsed { get; set; } = 0;

    [JsonPropertyName("ram_total")]
    public long RamTotal { get; set; } = 4096;

    [JsonPropertyName("ram_used")]
    public long RamUsed { get; set; } = 0;

    [JsonPropertyName("bandwidth_total")]
    public long BandwidthTotal { get; set; } = 500;

    [JsonPropertyName("bandwidth_used")]
    public long BandwidthUsed { get; set; } = 0;

    [JsonPropertyName("credits_pool")]
    public long CreditsPool { get; set; } = 0;

    [JsonPropertyName("security_level")]
    public int SecurityLevel { get; set; } = 1;

    [JsonPropertyName("passive_income_rate")]
    public double PassiveIncomeRate { get; set; } = 1.0;

    [JsonPropertyName("active_buffs")]
    public List<FactionBuff> ActiveBuffs { get; set; } = new();

    [JsonPropertyName("unlocked_features")]
    public List<string> UnlockedFeatures { get; set; } = new() { "faction_chat" };

    [JsonPropertyName("upgrade_progress")]
    public Dictionary<string, long> UpgradeProgress { get; set; } = new();
}

public class FactionBuff
{
    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("type")]
    public BuffType Type { get; set; }

    [JsonPropertyName("value")]
    public double Value { get; set; }

    [JsonPropertyName("duration_ms")]
    public long DurationMs { get; set; }

    [JsonPropertyName("expires_at")]
    public long ExpiresAt { get; set; }
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum BuffType
{
    [JsonPropertyName("experience_multiplier")]
    EXPERIENCE_MULTIPLIER,
    [JsonPropertyName("credit_multiplier")]
    CREDIT_MULTIPLIER,
    [JsonPropertyName("cpu_regen_boost")]
    CPU_REGEN_BOOST,
    [JsonPropertyName("ram_regen_boost")]
    RAM_REGEN_BOOST,
    [JsonPropertyName("scan_range_boost")]
    SCAN_RANGE_BOOST,
    [JsonPropertyName("firewall_boost")]
    FIREWALL_BOOST
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum UpgradeCategory
{
    [JsonPropertyName("cpu_capacity")]
    CPU_CAPACITY,
    [JsonPropertyName("ram_capacity")]
    RAM_CAPACITY,
    [JsonPropertyName("bandwidth_capacity")]
    BANDWIDTH_CAPACITY,
    [JsonPropertyName("security")]
    SECURITY,
    [JsonPropertyName("passive_income")]
    PASSIVE_INCOME,
    [JsonPropertyName("buff_duration")]
    BUFF_DURATION,
    [JsonPropertyName("member_limit")]
    MEMBER_LIMIT,
    [JsonPropertyName("feature_unlock")]
    FEATURE_UNLOCK
}

public class FactionUpgrade
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("category")]
    public UpgradeCategory Category { get; set; }

    [JsonPropertyName("levels")]
    public List<UpgradeLevel> Levels { get; set; } = new();
}

public class UpgradeLevel
{
    [JsonPropertyName("level")]
    public int Level { get; set; }

    [JsonPropertyName("cost_credits")]
    public long CostCredits { get; set; }

    [JsonPropertyName("cost_cpu")]
    public long CostCpu { get; set; }

    [JsonPropertyName("cost_ram")]
    public long CostRam { get; set; }

    [JsonPropertyName("benefit")]
    public string Benefit { get; set; } = "";

    [JsonPropertyName("value")]
    public double Value { get; set; }

    [JsonPropertyName("required_faction_level")]
    public int RequiredFactionLevel { get; set; } = 1;
}

public static class FactionUpgradesRegistry
{
    public static List<FactionUpgrade> AllUpgrades { get; } = new()
    {
        new FactionUpgrade
        {
            Id = "cpu_cap", Name = "CPU Core Cluster", Description = "Expand mainframe processing power",
            Category = UpgradeCategory.CPU_CAPACITY,
            Levels = new()
            {
                new UpgradeLevel { Level = 1, CostCredits = 5000, Benefit = "+5000 CPU capacity", Value = 5000.0 },
                new UpgradeLevel { Level = 2, CostCredits = 15000, Benefit = "+10000 CPU capacity", Value = 10000.0, RequiredFactionLevel = 2 },
                new UpgradeLevel { Level = 3, CostCredits = 50000, Benefit = "+25000 CPU capacity", Value = 25000.0, RequiredFactionLevel = 4 },
                new UpgradeLevel { Level = 4, CostCredits = 150000, Benefit = "+50000 CPU capacity", Value = 50000.0, RequiredFactionLevel = 6 }
            }
        },
        new FactionUpgrade
        {
            Id = "ram_cap", Name = "Memory Banks", Description = "Upgrade faction RAM pool",
            Category = UpgradeCategory.RAM_CAPACITY,
            Levels = new()
            {
                new UpgradeLevel { Level = 1, CostCredits = 5000, Benefit = "+8GB RAM", Value = 8192.0 },
                new UpgradeLevel { Level = 2, CostCredits = 15000, Benefit = "+16GB RAM", Value = 16384.0, RequiredFactionLevel = 2 },
                new UpgradeLevel { Level = 3, CostCredits = 50000, Benefit = "+32GB RAM", Value = 32768.0, RequiredFactionLevel = 4 },
                new UpgradeLevel { Level = 4, CostCredits = 150000, Benefit = "+64GB RAM", Value = 65536.0, RequiredFactionLevel = 6 }
            }
        },
        new FactionUpgrade
        {
            Id = "bandwidth_cap", Name = "Fiber Optic Link", Description = "Increase faction bandwidth",
            Category = UpgradeCategory.BANDWIDTH_CAPACITY,
            Levels = new()
            {
                new UpgradeLevel { Level = 1, CostCredits = 3000, Benefit = "+500 Mbps", Value = 500.0 },
                new UpgradeLevel { Level = 2, CostCredits = 10000, Benefit = "+1000 Mbps", Value = 1000.0, RequiredFactionLevel = 2 },
                new UpgradeLevel { Level = 3, CostCredits = 30000, Benefit = "+2500 Mbps", Value = 2500.0, RequiredFactionLevel = 4 },
                new UpgradeLevel { Level = 4, CostCredits = 100000, Benefit = "+5000 Mbps", Value = 5000.0, RequiredFactionLevel = 6 }
            }
        },
        new FactionUpgrade
        {
            Id = "security", Name = "ICE Security Suite", Description = "Strengthen mainframe defenses",
            Category = UpgradeCategory.SECURITY,
            Levels = new()
            {
                new UpgradeLevel { Level = 1, CostCredits = 2000, CostCpu = 1000, CostRam = 512, Benefit = "Security Level +1", Value = 1.0 },
                new UpgradeLevel { Level = 2, CostCredits = 8000, CostCpu = 3000, CostRam = 1024, Benefit = "Security Level +2", Value = 2.0, RequiredFactionLevel = 2 },
                new UpgradeLevel { Level = 3, CostCredits = 25000, CostCpu = 10000, CostRam = 4096, Benefit = "Security Level +3", Value = 3.0, RequiredFactionLevel = 4 },
                new UpgradeLevel { Level = 4, CostCredits = 80000, CostCpu = 30000, CostRam = 16384, Benefit = "Security Level +5", Value = 5.0, RequiredFactionLevel = 6 }
            }
        },
        new FactionUpgrade
        {
            Id = "passive_income", Name = "Automated Mining Rig", Description = "Generate passive credits",
            Category = UpgradeCategory.PASSIVE_INCOME,
            Levels = new()
            {
                new UpgradeLevel { Level = 1, CostCredits = 5000, CostCpu = 2000, CostRam = 1024, Benefit = "+2 credits/min per member", Value = 2.0 },
                new UpgradeLevel { Level = 2, CostCredits = 20000, CostCpu = 8000, CostRam = 4096, Benefit = "+5 credits/min per member", Value = 5.0, RequiredFactionLevel = 3 },
                new UpgradeLevel { Level = 3, CostCredits = 60000, CostCpu = 25000, CostRam = 16384, Benefit = "+10 credits/min per member", Value = 10.0, RequiredFactionLevel = 5 }
            }
        },
        new FactionUpgrade
        {
            Id = "member_limit", Name = "Member Expansion", Description = "Increase faction member cap",
            Category = UpgradeCategory.MEMBER_LIMIT,
            Levels = new()
            {
                new UpgradeLevel { Level = 1, CostCredits = 10000, Benefit = "Max members: 15", Value = 15.0 },
                new UpgradeLevel { Level = 2, CostCredits = 30000, Benefit = "Max members: 25", Value = 25.0, RequiredFactionLevel = 3 },
                new UpgradeLevel { Level = 3, CostCredits = 100000, Benefit = "Max members: 50", Value = 50.0, RequiredFactionLevel = 5 }
            }
        },
        new FactionUpgrade
        {
            Id = "feature_unlock", Name = "Advanced Modules", Description = "Unlock exclusive faction features",
            Category = UpgradeCategory.FEATURE_UNLOCK,
            Levels = new()
            {
                new UpgradeLevel { Level = 1, CostCredits = 20000, CostCpu = 10000, CostRam = 4096, Benefit = "Unlocks: faction_scan", Value = 1.0, RequiredFactionLevel = 2 },
                new UpgradeLevel { Level = 2, CostCredits = 50000, CostCpu = 25000, CostRam = 8192, Benefit = "Unlocks: mass_ddos", Value = 2.0, RequiredFactionLevel = 4 },
                new UpgradeLevel { Level = 3, CostCredits = 150000, CostCpu = 50000, CostRam = 32768, Benefit = "Unlocks: faction_shield", Value = 3.0, RequiredFactionLevel = 6 }
            }
        }
    };
}
