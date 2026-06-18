using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum TaskDifficulty
{
    [JsonPropertyName("trivial")]
    TRIVIAL,
    [JsonPropertyName("easy")]
    EASY,
    [JsonPropertyName("medium")]
    MEDIUM,
    [JsonPropertyName("hard")]
    HARD,
    [JsonPropertyName("expert")]
    EXPERT,
    [JsonPropertyName("legendary")]
    LEGENDARY
}

public static class TaskDifficultyExtensions
{
    public static string DisplayName(this TaskDifficulty difficulty) => difficulty switch
    {
        TaskDifficulty.TRIVIAL => "Trivial",
        TaskDifficulty.EASY => "Easy",
        TaskDifficulty.MEDIUM => "Medium",
        TaskDifficulty.HARD => "Hard",
        TaskDifficulty.EXPERT => "Expert",
        TaskDifficulty.LEGENDARY => "Legendary",
        _ => "Trivial"
    };

    public static string Color(this TaskDifficulty difficulty) => difficulty switch
    {
        TaskDifficulty.TRIVIAL => "#808080",
        TaskDifficulty.EASY => "#00FF00",
        TaskDifficulty.MEDIUM => "#FFFF00",
        TaskDifficulty.HARD => "#FF6600",
        TaskDifficulty.EXPERT => "#FF0000",
        TaskDifficulty.LEGENDARY => "#FF00FF",
        _ => "#808080"
    };

    public static double Multiplier(this TaskDifficulty difficulty) => difficulty switch
    {
        TaskDifficulty.TRIVIAL => 0.5,
        TaskDifficulty.EASY => 1.0,
        TaskDifficulty.MEDIUM => 1.5,
        TaskDifficulty.HARD => 2.5,
        TaskDifficulty.EXPERT => 4.0,
        TaskDifficulty.LEGENDARY => 8.0,
        _ => 1.0
    };
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum TaskTargetType
{
    [JsonPropertyName("penetration_test")]
    PENETRATION_TEST,
    [JsonPropertyName("data_theft")]
    DATA_THEFT,
    [JsonPropertyName("service_disruption")]
    SERVICE_DISRUPTION,
    [JsonPropertyName("defense_setup")]
    DEFENSE_SETUP,
    [JsonPropertyName("cryptography")]
    CRYPTOGRAPHY,
    [JsonPropertyName("social_engineering")]
    SOCIAL_ENGINEERING,
    [JsonPropertyName("botnet_operation")]
    BOTNET_OPERATION,
    [JsonPropertyName("network_recon")]
    NETWORK_RECON,
    [JsonPropertyName("forensics")]
    FORENSICS,
    [JsonPropertyName("bug_bounty")]
    BUG_BOUNTY
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum TaskStatus
{
    [JsonPropertyName("available")]
    AVAILABLE,
    [JsonPropertyName("in_progress")]
    IN_PROGRESS,
    [JsonPropertyName("completed")]
    COMPLETED,
    [JsonPropertyName("failed")]
    FAILED,
    [JsonPropertyName("expired")]
    EXPIRED
}

public class GameTask
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("title")]
    public string Title { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("difficulty")]
    public TaskDifficulty Difficulty { get; set; }

    [JsonPropertyName("required_level")]
    public int RequiredLevel { get; set; }

    [JsonPropertyName("required_commands")]
    public List<string> RequiredCommands { get; set; } = new();

    [JsonPropertyName("target_type")]
    public TaskTargetType TargetType { get; set; }

    [JsonPropertyName("target_ip")]
    public string? TargetIp { get; set; }

    [JsonPropertyName("objective")]
    public string Objective { get; set; } = "";

    [JsonPropertyName("time_limit_ms")]
    public long TimeLimitMs { get; set; } = 300000;

    [JsonPropertyName("rewards")]
    public TaskRewards Rewards { get; set; } = new();

    [JsonPropertyName("is_soloable")]
    public bool IsSoloable { get; set; } = true;

    [JsonPropertyName("max_party_size")]
    public int MaxPartySize { get; set; } = 4;

    [JsonPropertyName("tags")]
    public List<string> Tags { get; set; } = new();
}

public class TaskRewards
{
    [JsonPropertyName("experience")]
    public long Experience { get; set; } = 0;

    [JsonPropertyName("credits")]
    public long Credits { get; set; } = 0;

    [JsonPropertyName("reputation")]
    public int Reputation { get; set; } = 0;

    [JsonPropertyName("cpu_upgrade")]
    public int CpuUpgrade { get; set; } = 0;

    [JsonPropertyName("ram_upgrade")]
    public int RamUpgrade { get; set; } = 0;

    [JsonPropertyName("bandwidth_upgrade")]
    public int BandwidthUpgrade { get; set; } = 0;

    [JsonPropertyName("command_unlock")]
    public string? CommandUnlock { get; set; }
}

public class TaskInstance
{
    [JsonPropertyName("task_id")]
    public string TaskId { get; set; } = "";

    [JsonPropertyName("instance_id")]
    public string InstanceId { get; set; } = Guid.NewGuid().ToString();

    [JsonPropertyName("title")]
    public string Title { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("difficulty")]
    public TaskDifficulty Difficulty { get; set; }

    [JsonPropertyName("target_type")]
    public TaskTargetType TargetType { get; set; }

    [JsonPropertyName("target_ip")]
    public string? TargetIp { get; set; }

    [JsonPropertyName("objective")]
    public string Objective { get; set; } = "";

    [JsonPropertyName("time_limit_ms")]
    public long TimeLimitMs { get; set; }

    [JsonPropertyName("rewards")]
    public TaskRewards Rewards { get; set; } = new();

    [JsonPropertyName("created_at")]
    public long CreatedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [JsonPropertyName("claimed_by")]
    public List<string> ClaimedBy { get; set; } = new();

    [JsonPropertyName("status")]
    public TaskStatus Status { get; set; } = TaskStatus.AVAILABLE;

    [JsonPropertyName("completed_by")]
    public string? CompletedBy { get; set; }

    public Dictionary<string, object?> ToMap() => new()
    {
        ["taskId"] = TaskId,
        ["instanceId"] = InstanceId,
        ["title"] = Title,
        ["description"] = Description,
        ["difficulty"] = Difficulty.ToString(),
        ["targetType"] = TargetType.ToString(),
        ["targetIp"] = TargetIp,
        ["objective"] = Objective,
        ["timeLimitMs"] = TimeLimitMs,
        ["status"] = Status.ToString(),
        ["rewards"] = new Dictionary<string, object?>
        {
            ["experience"] = Rewards.Experience,
            ["credits"] = Rewards.Credits,
            ["reputation"] = Rewards.Reputation
        }
    };
}

public static class TaskTemplates
{
    public static List<GameTask> TaskPool { get; } = new()
    {
        new GameTask { Id = "t1", Title = "Scan Corporate Network", Description = "Perform a full reconnaissance scan of the target corporate network and identify all active hosts.", Difficulty = TaskDifficulty.EASY, RequiredLevel = 2, RequiredCommands = new() { "nmap" }, TargetType = TaskTargetType.NETWORK_RECON, TargetIp = "10.0.1.0/24", Objective = "Identify all hosts on the subnet", TimeLimitMs = 300000, Rewards = new TaskRewards { Experience = 80, Credits = 200, Reputation = 10, CpuUpgrade = 5, RamUpgrade = 10, BandwidthUpgrade = 5 } },
        new GameTask { Id = "t2", Title = "Extract Customer Database", Description = "Breach the target server and exfiltrate the customer database.", Difficulty = TaskDifficulty.MEDIUM, RequiredLevel = 5, RequiredCommands = new() { "exploit", "ssh" }, TargetType = TaskTargetType.DATA_THEFT, TargetIp = "10.0.2.50", Objective = "Locate and cat the database file", TimeLimitMs = 600000, Rewards = new TaskRewards { Experience = 300, Credits = 800, Reputation = 25, CpuUpgrade = 15, RamUpgrade = 25, BandwidthUpgrade = 10 } },
        new GameTask { Id = "t3", Title = "Deploy Ransomware Simulation", Description = "Simulate a ransomware attack by encrypting files on target and leaving a note.", Difficulty = TaskDifficulty.HARD, RequiredLevel = 8, RequiredCommands = new() { "exploit", "backdoor", "encrypt" }, TargetType = TaskTargetType.SERVICE_DISRUPTION, TargetIp = "10.0.3.100", Objective = "Encrypt 3 critical files", TimeLimitMs = 900000, Rewards = new TaskRewards { Experience = 800, Credits = 2000, Reputation = 50, CpuUpgrade = 25, RamUpgrade = 40, BandwidthUpgrade = 20 } },
        new GameTask { Id = "t4", Title = "Botnet Recruitment Drive", Description = "Infect 20 systems across the darknet with a backdoor and add them to your botnet.", Difficulty = TaskDifficulty.HARD, RequiredLevel = 12, RequiredCommands = new() { "backdoor", "botnet" }, TargetType = TaskTargetType.BOTNET_OPERATION, Objective = "Build a botnet of 20+ nodes", TimeLimitMs = 1200000, Rewards = new TaskRewards { Experience = 1500, Credits = 4000, Reputation = 100, CpuUpgrade = 40, RamUpgrade = 60, BandwidthUpgrade = 30 } },
        new GameTask { Id = "t5", Title = "Crack Government Cipher", Description = "Decrypt intercepted government communications. Multiple layers of encryption.", Difficulty = TaskDifficulty.EXPERT, RequiredLevel = 14, RequiredCommands = new() { "decrypt", "crack" }, TargetType = TaskTargetType.CRYPTOGRAPHY, Objective = "Decrypt all 5 message layers", TimeLimitMs = 1500000, Rewards = new TaskRewards { Experience = 3000, Credits = 8000, Reputation = 200, CpuUpgrade = 50, RamUpgrade = 80, BandwidthUpgrade = 40 } },
        new GameTask { Id = "t6", Title = "Zero-Day Auction Hijack", Description = "Intercept a zero-day exploit auction on the darknet and steal the payload.", Difficulty = TaskDifficulty.LEGENDARY, RequiredLevel = 18, RequiredCommands = new() { "sniff", "spoof", "zero-day" }, TargetType = TaskTargetType.DATA_THEFT, TargetIp = "203.0.113.50", Objective = "Steal the zero-day payload before the auction ends", TimeLimitMs = 1800000, Rewards = new TaskRewards { Experience = 8000, Credits = 25000, Reputation = 500, CpuUpgrade = 100, RamUpgrade = 150, BandwidthUpgrade = 60, CommandUnlock = "zero-day" } },
        new GameTask { Id = "t7", Title = "Defend Against APT", Description = "A sophisticated attacker is targeting your infrastructure. Set up defenses and trace them.", Difficulty = TaskDifficulty.MEDIUM, RequiredLevel = 9, RequiredCommands = new() { "firewall", "trace", "honeypot" }, TargetType = TaskTargetType.DEFENSE_SETUP, Objective = "Block 50+ attack attempts and trace the source", TimeLimitMs = 600000, Rewards = new TaskRewards { Experience = 500, Credits = 1500, Reputation = 40, CpuUpgrade = 20, RamUpgrade = 30, BandwidthUpgrade = 15 } },
        new GameTask { Id = "t8", Title = "Darknet Forum Heist", Description = "Extract user credentials from a darknet forum database.", Difficulty = TaskDifficulty.HARD, RequiredLevel = 10, RequiredCommands = new() { "sqlmap", "exploit" }, TargetType = TaskTargetType.DATA_THEFT, TargetIp = "172.16.0.50", Objective = "Dump the forum user table", TimeLimitMs = 900000, Rewards = new TaskRewards { Experience = 1200, Credits = 3500, Reputation = 80, CpuUpgrade = 30, RamUpgrade = 50, BandwidthUpgrade = 25 } },
        new GameTask { Id = "t9", Title = "Fireside Chat Intercept", Description = "Intercept VoIP communications between two corporate executives.", Difficulty = TaskDifficulty.EXPERT, RequiredLevel = 15, RequiredCommands = new() { "sniff", "decrypt", "spoof" }, TargetType = TaskTargetType.NETWORK_RECON, TargetIp = "10.0.5.100", Objective = "Capture and decrypt 5 VoIP packets", TimeLimitMs = 1200000, Rewards = new TaskRewards { Experience = 4000, Credits = 10000, Reputation = 250, CpuUpgrade = 60, RamUpgrade = 100, BandwidthUpgrade = 40 } },
        new GameTask { Id = "t10", Title = "Worm Containment", Description = "A worm is spreading through the network. Create a vaccine and contain it.", Difficulty = TaskDifficulty.EXPERT, RequiredLevel = 16, RequiredCommands = new() { "firewall", "trace", "worm" }, TargetType = TaskTargetType.FORENSICS, Objective = "Contain the worm and reverse-engineer its signature", TimeLimitMs = 1500000, Rewards = new TaskRewards { Experience = 6000, Credits = 15000, Reputation = 300, CpuUpgrade = 80, RamUpgrade = 120, BandwidthUpgrade = 50 } },
        new GameTask { Id = "t11", Title = "Simple Data Grab", Description = "Grab a file from an unsecured server. Practice target.", Difficulty = TaskDifficulty.TRIVIAL, RequiredLevel = 1, TargetType = TaskTargetType.DATA_THEFT, TargetIp = "10.0.0.5", Objective = "Connect and cat flag.txt", TimeLimitMs = 180000, Rewards = new TaskRewards { Experience = 30, Credits = 50, Reputation = 5, CpuUpgrade = 2, RamUpgrade = 5, BandwidthUpgrade = 2 } },
        new GameTask { Id = "t12", Title = "Brute Force Challenge", Description = "Brute force the admin panel of a local business server.", Difficulty = TaskDifficulty.EASY, RequiredLevel = 5, RequiredCommands = new() { "bruteforce" }, TargetType = TaskTargetType.PENETRATION_TEST, TargetIp = "10.0.1.200", Objective = "Find admin credentials via brute force", TimeLimitMs = 300000, Rewards = new TaskRewards { Experience = 150, Credits = 400, Reputation = 15, CpuUpgrade = 10, RamUpgrade = 15, BandwidthUpgrade = 5 } },
        new GameTask { Id = "t13", Title = "Phishing Campaign", Description = "Set up a phishing page and harvest credentials from corporate employees.", Difficulty = TaskDifficulty.MEDIUM, RequiredLevel = 7, RequiredCommands = new() { "proxy", "spoof" }, TargetType = TaskTargetType.SOCIAL_ENGINEERING, Objective = "Collect 10 sets of credentials", TimeLimitMs = 600000, Rewards = new TaskRewards { Experience = 400, Credits = 1000, Reputation = 30, CpuUpgrade = 15, RamUpgrade = 25, BandwidthUpgrade = 10 } },
        new GameTask { Id = "t14", Title = "DDoS Coordination", Description = "Coordinate a distributed denial of service attack using your botnet.", Difficulty = TaskDifficulty.HARD, RequiredLevel = 13, RequiredCommands = new() { "botnet", "overload" }, TargetType = TaskTargetType.SERVICE_DISRUPTION, TargetIp = "198.51.100.10", Objective = "Take the target offline for 60 seconds", TimeLimitMs = 1000000, Rewards = new TaskRewards { Experience = 2000, Credits = 5000, Reputation = 120, CpuUpgrade = 35, RamUpgrade = 50, BandwidthUpgrade = 25 } },
        new GameTask { Id = "t15", Title = "Bug Bounty: E-commerce", Description = "Find and exploit 3 vulnerabilities in an e-commerce platform.", Difficulty = TaskDifficulty.MEDIUM, RequiredLevel = 8, RequiredCommands = new() { "sqlmap", "exploit" }, TargetType = TaskTargetType.BUG_BOUNTY, TargetIp = "10.0.3.200", Objective = "Find SQLi, XSS, and LFI vulnerabilities", TimeLimitMs = 900000, Rewards = new TaskRewards { Experience = 600, Credits = 2000, Reputation = 60, CpuUpgrade = 20, RamUpgrade = 30, BandwidthUpgrade = 15 } }
    };
}
