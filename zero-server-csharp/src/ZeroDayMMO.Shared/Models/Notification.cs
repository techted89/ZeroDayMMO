using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum NotificationType
{
    [JsonPropertyName("achievement_unlocked")]
    ACHIEVEMENT_UNLOCKED,
    [JsonPropertyName("challenge_completed")]
    CHALLENGE_COMPLETED,
    [JsonPropertyName("challenge_expired")]
    CHALLENGE_EXPIRED,
    [JsonPropertyName("storyline_available")]
    STORYLINE_AVAILABLE,
    [JsonPropertyName("faction_invited")]
    FACTION_INVITED,
    [JsonPropertyName("level_up")]
    LEVEL_UP,
    [JsonPropertyName("world_event_started")]
    WORLD_EVENT_STARTED,
    [JsonPropertyName("world_event_joinable")]
    WORLD_EVENT_JOINABLE,
    [JsonPropertyName("system_message")]
    SYSTEM_MESSAGE,
    [JsonPropertyName("reward_granted")]
    REWARD_GRANTED,
    [JsonPropertyName("daily_login")]
    DAILY_LOGIN
}

public class Notification
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString();

    [JsonPropertyName("type")]
    public NotificationType Type { get; set; }

    [JsonPropertyName("title")]
    public string Title { get; set; } = "";

    [JsonPropertyName("message")]
    public string Message { get; set; } = "";

    [JsonPropertyName("created_at")]
    public long CreatedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [JsonPropertyName("read")]
    public bool Read { get; set; } = false;

    [JsonPropertyName("data")]
    public Dictionary<string, string> Data { get; set; } = new();
}
