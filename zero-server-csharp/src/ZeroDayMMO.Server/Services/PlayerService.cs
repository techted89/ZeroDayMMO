using System.Collections.Concurrent;
using System.Text.Json;
using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public interface IPlayerService
{
    PlayerData? GetPlayer(string playerId);
    PlayerData? GetPlayerByUsername(string username);
    PlayerData? Authenticate(string username, string password);
    PlayerData CreatePlayer(string username, string passwordHash, string displayName);
    void UpdatePlayer(PlayerData player);
    IEnumerable<PlayerData> GetAllPlayers();
    void LoadFromJson(string json);
    string SaveToJson();
}

public class PlayerService : IPlayerService
{
    private readonly ConcurrentDictionary<string, PlayerData> _players = new();
    private readonly ConcurrentDictionary<string, string> _usernameIndex = new(StringComparer.OrdinalIgnoreCase);
    private readonly string _dataPath;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        WriteIndented = true
    };

    public PlayerService(string dataPath = "data/players.json")
    {
        _dataPath = dataPath;
    }

    public PlayerData? GetPlayer(string playerId)
    {
        return _players.TryGetValue(playerId, out var player) ? player : null;
    }

    public PlayerData? GetPlayerByUsername(string username)
    {
        if (_usernameIndex.TryGetValue(username, out var playerId))
            return GetPlayer(playerId);
        return null;
    }

    public PlayerData? Authenticate(string username, string password)
    {
        var player = GetPlayerByUsername(username);
        if (player is null)
            return null;

        try
        {
            return BCrypt.Net.BCrypt.Verify(password, player.PasswordHash) ? player : null;
        }
        catch
        {
            return null;
        }
    }

    public PlayerData CreatePlayer(string username, string passwordHash, string displayName)
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var player = new PlayerData
        {
            Id = Guid.NewGuid().ToString(),
            Username = username,
            PasswordHash = passwordHash,
            DisplayName = displayName,
            Cpu = 10.0,
            MaxCpu = 10.0,
            Ram = 100.0,
            MaxRam = 100.0,
            Network = 50.0,
            MaxNetwork = 50.0,
            LastLogin = now,
            CreatedAt = now
        };

        _players[player.Id] = player;
        _usernameIndex[username] = player.Id;
        return player;
    }

    public void UpdatePlayer(PlayerData player)
    {
        _players[player.Id] = player;
        _usernameIndex[player.Username] = player.Id;
    }

    public IEnumerable<PlayerData> GetAllPlayers()
    {
        return _players.Values;
    }

    public void LoadFromJson(string json)
    {
        var players = JsonSerializer.Deserialize<List<PlayerData>>(json, JsonOptions);
        if (players is null) return;

        foreach (var player in players)
        {
            _players[player.Id] = player;
            _usernameIndex[player.Username] = player.Id;
        }
    }

    public string SaveToJson()
    {
        return JsonSerializer.Serialize(_players.Values.ToList(), JsonOptions);
    }
}
