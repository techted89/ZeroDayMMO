using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

public class BanRecord
{
    [JsonPropertyName("player_id")]
    public string PlayerId { get; set; } = "";

    [JsonPropertyName("username")]
    public string Username { get; set; } = "";

    [JsonPropertyName("banned_by")]
    public string BannedBy { get; set; } = "";

    [JsonPropertyName("reason")]
    public string Reason { get; set; } = "";

    [JsonPropertyName("banned_at")]
    public long BannedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [JsonPropertyName("expires_at")]
    public long? ExpiresAt { get; set; }

    [JsonPropertyName("is_active")]
    public bool IsActive { get; set; } = true;
}

public class AdminAction
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString();

    [JsonPropertyName("action")]
    public string Action { get; set; } = "";

    [JsonPropertyName("admin_username")]
    public string AdminUsername { get; set; } = "";

    [JsonPropertyName("target_player_id")]
    public string? TargetPlayerId { get; set; }

    [JsonPropertyName("target_username")]
    public string? TargetUsername { get; set; }

    [JsonPropertyName("details")]
    public string Details { get; set; } = "";

    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [JsonPropertyName("ip_address")]
    public string? IpAddress { get; set; }
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum CheatAlertType
{
    [JsonPropertyName("speed_hack")]
    SPEED_HACK,
    [JsonPropertyName("resource_tamper")]
    RESOURCE_TAMPER,
    [JsonPropertyName("xp_tamper")]
    XP_TAMPER,
    [JsonPropertyName("credit_tamper")]
    CREDIT_TAMPER,
    [JsonPropertyName("command_spam")]
    COMMAND_SPAM,
    [JsonPropertyName("rapid_travel")]
    RAPID_TRAVEL,
    [JsonPropertyName("impossible_action")]
    IMPOSSIBLE_ACTION,
    [JsonPropertyName("packet_tampering")]
    PACKET_TAMPERING,
    [JsonPropertyName("suspicious_login")]
    SUSPICIOUS_LOGIN,
    [JsonPropertyName("level_jump")]
    LEVEL_JUMP,
    [JsonPropertyName("faction_exploit")]
    FACTION_EXPLOIT,
    [JsonPropertyName("economy_abuse")]
    ECONOMY_ABUSE
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum CheatSeverity
{
    [JsonPropertyName("low")]
    LOW,
    [JsonPropertyName("medium")]
    MEDIUM,
    [JsonPropertyName("high")]
    HIGH,
    [JsonPropertyName("critical")]
    CRITICAL
}

public class CheatAlert
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString();

    [JsonPropertyName("player_id")]
    public string PlayerId { get; set; } = "";

    [JsonPropertyName("username")]
    public string Username { get; set; } = "";

    [JsonPropertyName("alert_type")]
    public CheatAlertType AlertType { get; set; }

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("severity")]
    public CheatSeverity Severity { get; set; }

    [JsonPropertyName("detected_at")]
    public long DetectedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [JsonPropertyName("resolved")]
    public bool Resolved { get; set; } = false;

    [JsonPropertyName("metadata")]
    public Dictionary<string, object?> Metadata { get; set; } = new();
}

public class AdminPlayerView
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

    [JsonPropertyName("is_online")]
    public bool IsOnline { get; set; }

    [JsonPropertyName("is_banned")]
    public bool IsBanned { get; set; }

    [JsonPropertyName("ban_reason")]
    public string? BanReason { get; set; }

    [JsonPropertyName("career_path")]
    public string CareerPath { get; set; } = "";

    [JsonPropertyName("heat_level")]
    public int HeatLevel { get; set; }

    [JsonPropertyName("justice_points")]
    public int JusticePoints { get; set; }

    [JsonPropertyName("current_zone_id")]
    public string CurrentZoneId { get; set; } = "";

    [JsonPropertyName("login_streak")]
    public int LoginStreak { get; set; }

    [JsonPropertyName("total_logins")]
    public int TotalLogins { get; set; }

    [JsonPropertyName("last_login_ip")]
    public string LastLoginIp { get; set; } = "";

    [JsonPropertyName("last_login_at")]
    public long LastLoginAt { get; set; }

    [JsonPropertyName("faction_id")]
    public string? FactionId { get; set; }

    [JsonPropertyName("party_id")]
    public string? PartyId { get; set; }

    [JsonPropertyName("unlocked_commands_count")]
    public int UnlockedCommandsCount { get; set; }

    [JsonPropertyName("discovered_nodes_count")]
    public int DiscoveredNodesCount { get; set; }

    [JsonPropertyName("completed_tasks_count")]
    public int CompletedTasksCount { get; set; }

    [JsonPropertyName("total_play_time_ms")]
    public long TotalPlayTimeMs { get; set; } = 0;

    [JsonPropertyName("skill_points")]
    public int SkillPoints { get; set; }

    [JsonPropertyName("unlocked_skill_count")]
    public int UnlockedSkillCount { get; set; }

    [JsonPropertyName("times_arrested")]
    public int TimesArrested { get; set; }

    public static AdminPlayerView From(Player player) => new()
    {
        Id = player.Id,
        Username = player.Username,
        Level = player.Level,
        Experience = player.Experience,
        ExperienceToNext = player.ExperienceToNext,
        Cpu = player.Cpu,
        MaxCpu = player.MaxCpu,
        Ram = player.Ram,
        MaxRam = player.MaxRam,
        Bandwidth = player.Bandwidth,
        MaxBandwidth = player.MaxBandwidth,
        Credits = player.Credits,
        Reputation = player.Reputation,
        PrestigeLevel = player.PrestigeLevel,
        IsOnline = player.IsOnline,
        IsBanned = player.IsBanned,
        BanReason = player.BanReason,
        CareerPath = player.CareerPath,
        HeatLevel = player.HeatLevel,
        JusticePoints = player.JusticePoints,
        CurrentZoneId = player.CurrentZoneId,
        LoginStreak = player.LoginStreak,
        TotalLogins = player.TotalLogins,
        LastLoginIp = player.LastLoginIp,
        LastLoginAt = player.LastLoginAt,
        FactionId = player.FactionId,
        PartyId = player.PartyId,
        UnlockedCommandsCount = player.UnlockedCommands.Count,
        DiscoveredNodesCount = player.DiscoveredNodes.Count,
        CompletedTasksCount = player.CompletedTasks.Count,
        TotalPlayTimeMs = 0,
        SkillPoints = player.SkillPoints,
        UnlockedSkillCount = player.UnlockedSkills.Count,
        TimesArrested = player.TimesArrested
    };
}

public class ModifyPlayerRequest
{
    [JsonPropertyName("field")]
    public string Field { get; set; } = "";

    [JsonPropertyName("value")]
    public string Value { get; set; } = "";
}

public class BroadcastRequest
{
    [JsonPropertyName("title")]
    public string Title { get; set; } = "";

    [JsonPropertyName("message")]
    public string Message { get; set; } = "";

    [JsonPropertyName("message_type")]
    public string MessageType { get; set; } = "announcement";

    [JsonPropertyName("priority")]
    public string Priority { get; set; } = "normal";
}

public class ServerLogEntry
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    [JsonPropertyName("level")]
    public string Level { get; set; } = "";

    [JsonPropertyName("logger")]
    public string Logger { get; set; } = "";

    [JsonPropertyName("message")]
    public string Message { get; set; } = "";
}
