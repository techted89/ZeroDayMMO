using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum ZoneFaction
{
    [JsonPropertyName("neutral")]
    Neutral,
    [JsonPropertyName("syndicate")]
    Syndicate,
    [JsonPropertyName("corpnet")]
    CorpNet,
    [JsonPropertyName("ghostnet")]
    GhostNet,
    [JsonPropertyName("freeworld")]
    FreeWorld,
    [JsonPropertyName("zeroday")]
    ZeroDay,
    [JsonPropertyName("law_enforcement")]
    LawEnforcement
}

public static class ZoneFactionExtensions
{
    public static string DisplayName(this ZoneFaction faction) => faction switch
    {
        ZoneFaction.Neutral => "Neutral",
        ZoneFaction.Syndicate => "Syndicate",
        ZoneFaction.CorpNet => "CorpNet",
        ZoneFaction.GhostNet => "GhostNet",
        ZoneFaction.FreeWorld => "FreeWorld",
        ZoneFaction.ZeroDay => "ZeroDay",
        ZoneFaction.LawEnforcement => "Law Enforcement",
        _ => "Neutral"
    };
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum ZoneState
{
    [JsonPropertyName("safe")]
    Safe,
    [JsonPropertyName("contested")]
    Contested,
    [JsonPropertyName("controlled")]
    Controlled,
    [JsonPropertyName("warzone")]
    Warzone,
    [JsonPropertyName("locked")]
    Locked
}

public class Zone
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString();

    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("controlling_faction")]
    public ZoneFaction ControllingFaction { get; set; } = ZoneFaction.Neutral;

    [JsonPropertyName("state")]
    public ZoneState State { get; set; } = ZoneState.Safe;

    [JsonPropertyName("control_level")]
    public int ControlLevel { get; set; } = 50;

    [JsonPropertyName("max_control_level")]
    public int MaxControlLevel { get; set; } = 100;

    [JsonPropertyName("security_level")]
    public int SecurityLevel { get; set; } = 1;

    [JsonPropertyName("required_level")]
    public int RequiredLevel { get; set; } = 1;

    [JsonPropertyName("connected_zone_ids")]
    public List<string> ConnectedZoneIds { get; set; } = new();

    [JsonPropertyName("has_boss")]
    public bool HasBoss { get; set; } = false;

    [JsonPropertyName("boss_name")]
    public string BossName { get; set; } = "";

    [JsonPropertyName("boss_level")]
    public int BossLevel { get; set; } = 1;

    [JsonPropertyName("threat_level")]
    public int ThreatLevel { get; set; } = 0;
}

public class ZoneSnapshot
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("controlling_faction")]
    public string ControllingFaction { get; set; } = "";

    [JsonPropertyName("state")]
    public string State { get; set; } = "";

    [JsonPropertyName("control_level")]
    public int ControlLevel { get; set; }

    [JsonPropertyName("max_control_level")]
    public int MaxControlLevel { get; set; }

    [JsonPropertyName("security_level")]
    public int SecurityLevel { get; set; }

    [JsonPropertyName("required_level")]
    public int RequiredLevel { get; set; }

    [JsonPropertyName("connected_zone_ids")]
    public List<string> ConnectedZoneIds { get; set; } = new();

    [JsonPropertyName("has_boss")]
    public bool HasBoss { get; set; }

    [JsonPropertyName("boss_name")]
    public string BossName { get; set; } = "";

    [JsonPropertyName("boss_level")]
    public int BossLevel { get; set; }

    [JsonPropertyName("threat_level")]
    public int ThreatLevel { get; set; }

    public static ZoneSnapshot From(Zone zone) => new()
    {
        Id = zone.Id,
        Name = zone.Name,
        Description = zone.Description,
        ControllingFaction = zone.ControllingFaction.ToString(),
        State = zone.State.ToString(),
        ControlLevel = zone.ControlLevel,
        MaxControlLevel = zone.MaxControlLevel,
        SecurityLevel = zone.SecurityLevel,
        RequiredLevel = zone.RequiredLevel,
        ConnectedZoneIds = new List<string>(zone.ConnectedZoneIds),
        HasBoss = zone.HasBoss,
        BossName = zone.BossName,
        BossLevel = zone.BossLevel,
        ThreatLevel = zone.ThreatLevel
    };
}
