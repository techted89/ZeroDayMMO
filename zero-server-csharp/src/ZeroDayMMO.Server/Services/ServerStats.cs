using System.Text.Json;

namespace ZeroDayMMO.Server.Services;

public class ServerStats
{
    public int OnlinePlayers { get; init; }
    public int TotalConnections { get; init; }
    public Dictionary<string, object?> Persistence { get; init; } = new();

    public string ToJsonString()
    {
        var obj = new Dictionary<string, object?>
        {
            ["status"] = "ok",
            ["online_players"] = OnlinePlayers,
            ["total_connections"] = TotalConnections,
            ["persistence"] = Persistence
        };

        return JsonSerializer.Serialize(obj, new JsonSerializerOptions
        {
            PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
            WriteIndented = false
        });
    }
}
