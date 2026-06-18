using System.Collections.Concurrent;
using System.Security.Cryptography;
using System.Text.Json;
using ZeroDayMMO.Server.Config;
using ZeroDayMMO.Server.Security;
using ZeroDayMMO.Shared.Models;
using TaskStatus = ZeroDayMMO.Shared.Models.TaskStatus;

namespace ZeroDayMMO.Server.Services;

public interface IPlayerService
{
    Player? GetPlayer(string playerId);
    Player? GetPlayerByUsername(string username);
    Player? Authenticate(string username, string password);
    Player CreatePlayer(string username, string passwordHash, string displayName);
    void UpdatePlayer(Player player);
    IEnumerable<Player> GetAllPlayers();
    void LoadFromJson(string json);
    string SaveToJson();
    Task<Player> Register(string username, string password);
    Task<Player?> Login(string username, string password);
    Task<Player?> AddExperience(string playerId, long amount);
    Task<Player?> AddResources(string playerId, TaskRewards rewards);
    Task<Player?> DeductCredits(string playerId, long amount);
    Task<Player?> AddCredits(string playerId, long amount);
    Task<Player?> AddReputation(string playerId, int amount);
    Task<bool?> ConsumeResources(string playerId, int cpu, int ram);
    Task RegenerateResources(string playerId);
    Task<int> RegenerateAllOnline();
    Task<Player?> UnlockCommand(string playerId, string commandName);
    Task<Player?> DiscoverNode(string playerId, string ip);
    Task<Player?> GrantNetworkAccess(string playerId, List<string> networks);
    Task<Player?> UpgradeResources(string playerId, int cpu = 0, int ram = 0, int bandwidth = 0);
    Task<Player?> SetStoryline(string playerId, string storylineId);
    Task<Player?> AdvanceStoryline(string playerId);
    Task<Player?> CompleteStoryline(string playerId, string storylineId);
    Task<Player?> AssignTask(string playerId, ZeroDayMMO.Shared.Models.TaskInstance task);
    Task<Player?> CompleteTask(string playerId, string taskInstanceId);
    Task<Player?> SetParty(string playerId, string? partyId);
    Task<Player?> SetFaction(string playerId, string? factionId);
    Task<Player?> AddWorldEventParticipation(string playerId, string eventId);
    Task<Player?> RemoveWorldEventParticipation(string playerId, string eventId);
    Task<Player?> AddToInventory(string playerId, InventoryItem item);
    Task<Player?> RemoveFromInventory(string playerId, string itemId);
    Task<Player?> AddZeroDayExploit(string playerId, InventoryItem item);
    Task<InventoryItem?> ConsumeZeroDayExploit(string playerId);
    PlayerSnapshot? GetSnapshot(string playerId);
    List<Player> GetOnlinePlayers();
    List<Player> GetOnlinePlayersPage(int offset, int limit);
    int? ConsumeLevelUp(string playerId);
    Task CreatePlayerDirect(Player player);
    Task SetOnline(string playerId, bool online);
    Task DeletePlayer(string playerId);
    Task<T?> WithPlayerAction<T>(string playerId, Func<Player, T?> action) where T : class;
    Task WithPlayerActionAsync(string playerId, Func<Player, Task> action);
    Task<Player?> AssignTaskAsync(string playerId, ZeroDayMMO.Shared.Models.TaskInstance task);
    Task<Player?> CompleteTaskAsync(string playerId, string taskInstanceId);
    Task<Player?> SetPartyAsync(string playerId, string? partyId);
    Task<Player?> AddExperienceAsync(string playerId, long amount);
    Task<Player?> AddCreditsAsync(string playerId, long amount);
    Task<Player?> AddReputationAsync(string playerId, int amount);
    Task<Player?> UpgradeResourcesAsync(string playerId, int cpu = 0, int ram = 0, int bandwidth = 0);
    Task<Player?> UnlockCommandAsync(string playerId, string commandName);
    Task<Player?> DiscoverNodeAsync(string playerId, string ip);
    (long Hits, long Misses) SnapshotCacheStats();
    void InvalidateSnapshot(string playerId);
}

public class PlayerService : IPlayerService
{
    private readonly IPasswordHasher _passwordHasher;
    private readonly ConcurrentDictionary<string, Player> _players = new();
    private readonly ConcurrentDictionary<string, string> _usernameIndex = new(StringComparer.OrdinalIgnoreCase);
    private readonly ConcurrentDictionary<string, string> _sessions = new();
    private readonly SemaphoreSlim _tableLock = new(1, 1);
    private readonly ConcurrentDictionary<string, SemaphoreSlim> _playerLocks = new();
    private readonly string _dataPath;

    private long _snapshotTtlMs = 250;
    private readonly ConcurrentDictionary<string, CachedSnapshot> _snapshotCache = new();
    private long _snapshotHits;
    private long _snapshotMisses;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        WriteIndented = true
    };

    public long SnapshotTtlMs
    {
        get => _snapshotTtlMs;
        set => _snapshotTtlMs = value;
    }

    public PlayerService(IPasswordHasher passwordHasher, string dataPath = "data/players.json")
    {
        _passwordHasher = passwordHasher;
        _dataPath = dataPath;
    }

    private SemaphoreSlim LockFor(string playerId) =>
        _playerLocks.GetOrAdd(playerId, _ => new SemaphoreSlim(1, 1));

    private async Task<T?> WithPlayerLock<T>(string playerId, Func<Player, T?> block) where T : class
    {
        if (!_players.TryGetValue(playerId, out var player))
            return null;

        var sem = LockFor(playerId);
        await sem.WaitAsync();
        try
        {
            if (!_players.TryGetValue(playerId, out var p))
                return null;
            var result = block(p);
            _snapshotCache.TryRemove(playerId, out _);
            return result;
        }
        finally
        {
            sem.Release();
        }
    }

    private void InvalidateCache(string playerId)
    {
        _snapshotCache.TryRemove(playerId, out _);
    }

    public async Task<T?> WithPlayerAction<T>(string playerId, Func<Player, T?> action) where T : class =>
        await WithPlayerLock(playerId, action);

    public async Task WithPlayerActionAsync(string playerId, Func<Player, Task> action)
    {
        if (!_players.TryGetValue(playerId, out var player))
            return;
        var sem = LockFor(playerId);
        await sem.WaitAsync();
        try
        {
            await action(player);
        }
        finally
        {
            sem.Release();
        }
    }

    public Task<Player?> AssignTaskAsync(string playerId, ZeroDayMMO.Shared.Models.TaskInstance task) =>
        AssignTask(playerId, task);
    public Task<Player?> CompleteTaskAsync(string playerId, string taskInstanceId) =>
        CompleteTask(playerId, taskInstanceId);
    public Task<Player?> SetPartyAsync(string playerId, string? partyId) =>
        SetParty(playerId, partyId);
    public Task<Player?> AddExperienceAsync(string playerId, long amount) =>
        AddExperience(playerId, amount);
    public Task<Player?> AddCreditsAsync(string playerId, long amount) =>
        AddCredits(playerId, amount);
    public Task<Player?> AddReputationAsync(string playerId, int amount) =>
        AddReputation(playerId, amount);
    public Task<Player?> UpgradeResourcesAsync(string playerId, int cpu = 0, int ram = 0, int bandwidth = 0) =>
        UpgradeResources(playerId, cpu, ram, bandwidth);
    public Task<Player?> UnlockCommandAsync(string playerId, string commandName) =>
        UnlockCommand(playerId, commandName);
    public Task<Player?> DiscoverNodeAsync(string playerId, string ip) =>
        DiscoverNode(playerId, ip);

    public async Task<Player> Register(string username, string password)
    {
        await _tableLock.WaitAsync();
        try
        {
            if (_usernameIndex.ContainsKey(username))
                throw new InvalidOperationException("Username already exists");

            var player = new Player
            {
                Username = username,
                PasswordHash = _passwordHasher.Hash(password),
                Cpu = 100,
                MaxCpu = 100,
                Ram = 256,
                MaxRam = 256,
                Bandwidth = 50,
                MaxBandwidth = 50
            };

            _players[player.Id] = player;
            _usernameIndex[player.Username] = player.Id;
            return player;
        }
        finally
        {
            _tableLock.Release();
        }
    }

    public async Task<Player?> Login(string username, string password)
    {
        await _tableLock.WaitAsync();
        try
        {
            if (!_usernameIndex.TryGetValue(username, out var playerId))
                return null;

            var player = _players.GetValueOrDefault(playerId);
            if (player == null) return null;

            if (!_passwordHasher.Verify(password, player.PasswordHash))
                return null;

            player.IsOnline = true;
            _sessions[player.Id] = NewSessionToken();
            return player;
        }
        finally
        {
            _tableLock.Release();
        }
    }

    public async Task<Player?> AddExperience(string playerId, long amount)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.Experience += amount;
            CheckForLevelUp(player);
            return player;
        });
    }

    private static void CheckForLevelUp(Player player)
    {
        var leveledUp = false;
        var reachedLevel = player.Level;

        while (player.Experience >= player.ExperienceToNext)
        {
            player.Experience -= player.ExperienceToNext;
            player.Level++;
            player.ExperienceToNext = (long)(player.ExperienceToNext * 1.5);
            player.MaxCpu += 5;
            player.MaxRam += 16;
            player.MaxBandwidth += 2;
            player.Cpu = player.MaxCpu;
            player.Ram = player.MaxRam;
            player.Bandwidth = player.MaxBandwidth;
            player.SkillPoints += player.Level % 5 == 0 ? 2 : 1;
            leveledUp = true;
            reachedLevel = player.Level;
        }

        if (leveledUp) player.LastLevelNotified = reachedLevel;
    }

    public int? ConsumeLevelUp(string playerId)
    {
        if (!_players.TryGetValue(playerId, out var p)) return null;
        var level = p.LastLevelNotified;
        if (level > 0) p.LastLevelNotified = 0;
        return level > 0 ? level : null;
    }

    public async Task<Player?> AddResources(string playerId, TaskRewards rewards)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.Credits += rewards.Credits;
            player.Cpu = Math.Min(player.Cpu + rewards.CpuUpgrade, player.MaxCpu);
            player.Ram = Math.Min(player.Ram + rewards.RamUpgrade, player.MaxRam);
            player.Bandwidth = Math.Min(player.Bandwidth + rewards.BandwidthUpgrade, player.MaxBandwidth);
            player.Reputation += rewards.Reputation;
            return player;
        });
    }

    public async Task<Player?> DeductCredits(string playerId, long amount)
    {
        return await WithPlayerLock(playerId, player =>
        {
            if (player.Credits < amount) return null;
            player.Credits -= amount;
            return player;
        });
    }

    public async Task<Player?> AddCredits(string playerId, long amount)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.Credits += amount;
            if (amount > 0) player.LifetimeCreditsEarned += amount;
            return player;
        });
    }

    public async Task<Player?> AddReputation(string playerId, int amount)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.Reputation += amount;
            return player;
        });
    }

    public async Task<bool?> ConsumeResources(string playerId, int cpu, int ram)
    {
        if (!_players.TryGetValue(playerId, out var player))
            return null;
        var sem = LockFor(playerId);
        await sem.WaitAsync();
        try
        {
            if (player.Cpu < cpu || player.Ram < ram) return false;
            player.Cpu -= cpu;
            player.Ram -= ram;
            return true;
        }
        finally
        {
            sem.Release();
        }
    }

    public async Task RegenerateResources(string playerId)
    {
        await WithPlayerLock(playerId, player =>
        {
            if (player.Cpu < player.MaxCpu)
                player.Cpu = Math.Min(player.Cpu + 2, player.MaxCpu);
            if (player.Ram < player.MaxRam)
                player.Ram = Math.Min(player.Ram + 4, player.MaxRam);
            return player;
        });
    }

    public async Task<int> RegenerateAllOnline()
    {
        var count = 0;
        foreach (var p in _players.Values)
        {
            if (!p.IsOnline) continue;
            if (p.Cpu < p.MaxCpu || p.Ram < p.MaxRam)
            {
                await WithPlayerLock(p.Id, player =>
                {
                    if (player.Cpu < player.MaxCpu)
                        player.Cpu = Math.Min(player.Cpu + 2, player.MaxCpu);
                    if (player.Ram < player.MaxRam)
                        player.Ram = Math.Min(player.Ram + 4, player.MaxRam);
                    return player;
                });
                count++;
            }
        }
        return count;
    }

    public async Task<Player?> UnlockCommand(string playerId, string commandName)
    {
        return await WithPlayerLock(playerId, player =>
        {
            if (CommandRegistry.CommandMap.ContainsKey(commandName))
                player.UnlockedCommands.Add(commandName);
            return player;
        });
    }

    public async Task<Player?> DiscoverNode(string playerId, string ip)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.DiscoveredNodes.Add(ip);
            return player;
        });
    }

    public async Task<Player?> GrantNetworkAccess(string playerId, List<string> networks)
    {
        return await WithPlayerLock(playerId, player =>
        {
            foreach (var net in networks)
                player.DiscoveredNodes.Add(net);
            return player;
        });
    }

    public async Task<Player?> UpgradeResources(string playerId, int cpu = 0, int ram = 0, int bandwidth = 0)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.MaxCpu += cpu;
            player.MaxRam += ram;
            player.MaxBandwidth += bandwidth;
            player.Cpu = player.MaxCpu;
            player.Ram = player.MaxRam;
            player.Bandwidth = player.MaxBandwidth;
            return player;
        });
    }

    public async Task<Player?> SetStoryline(string playerId, string storylineId)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.CurrentStoryline = storylineId;
            player.StorylineProgress = 0;
            return player;
        });
    }

    public async Task<Player?> AdvanceStoryline(string playerId)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.StorylineProgress++;
            return player;
        });
    }

    public async Task<Player?> CompleteStoryline(string playerId, string storylineId)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.CompletedStorylines.Add(storylineId);
            player.CurrentStoryline = null;
            player.StorylineProgress = 0;
            return player;
        });
    }

    public async Task<Player?> AssignTask(string playerId, ZeroDayMMO.Shared.Models.TaskInstance task)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.ActiveTasks.Add(task);
            return player;
        });
    }

    public async Task<Player?> CompleteTask(string playerId, string taskInstanceId)
    {
        return await WithPlayerLock(playerId, player =>
        {
            var task = player.ActiveTasks.FirstOrDefault(t => t.InstanceId == taskInstanceId);
            if (task == null) return null;
            task.Status = TaskStatus.COMPLETED;
            player.CompletedTasks.Add(task.TaskId);
            player.ActiveTasks.RemoveAll(t => t.InstanceId == taskInstanceId);
            return player;
        });
    }

    public async Task<Player?> SetParty(string playerId, string? partyId)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.PartyId = partyId;
            return player;
        });
    }

    public async Task<Player?> SetFaction(string playerId, string? factionId)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.FactionId = factionId;
            return player;
        });
    }

    public async Task<Player?> AddWorldEventParticipation(string playerId, string eventId)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.WorldEventParticipation.Add(eventId);
            return player;
        });
    }

    public async Task<Player?> RemoveWorldEventParticipation(string playerId, string eventId)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.WorldEventParticipation.Remove(eventId);
            return player;
        });
    }

    public async Task<Player?> AddToInventory(string playerId, InventoryItem item)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.Inventory.Add(item);
            return player;
        });
    }

    public async Task<Player?> RemoveFromInventory(string playerId, string itemId)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.Inventory.RemoveAll(i => i.Id == itemId);
            return player;
        });
    }

    public async Task<Player?> AddZeroDayExploit(string playerId, InventoryItem item)
    {
        return await WithPlayerLock(playerId, player =>
        {
            player.ActiveZeroDayExploits.Add(item);
            return player;
        });
    }

    public async Task<InventoryItem?> ConsumeZeroDayExploit(string playerId)
    {
        var result = await WithPlayerLock(playerId, player =>
        {
            var exploit = player.ActiveZeroDayExploits.FirstOrDefault();
            if (exploit != null) player.ActiveZeroDayExploits.RemoveAt(0);
            return exploit;
        });
        return result;
    }

    public PlayerSnapshot? GetSnapshot(string playerId)
    {
        if (!_players.TryGetValue(playerId, out var p)) return null;
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        if (_snapshotCache.TryGetValue(playerId, out var cached) && now - cached.At < _snapshotTtlMs)
        {
            Interlocked.Increment(ref _snapshotHits);
            return cached.Snapshot;
        }

        Interlocked.Increment(ref _snapshotMisses);
        var snap = p.ToSnapshot();
        _snapshotCache[playerId] = new CachedSnapshot(snap, now);
        return snap;
    }

    public void InvalidateSnapshot(string playerId)
    {
        _snapshotCache.TryRemove(playerId, out _);
    }

    public (long Hits, long Misses) SnapshotCacheStats() => (_snapshotHits, _snapshotMisses);

    public List<Player> GetOnlinePlayers() => _players.Values.Where(p => p.IsOnline).ToList();

    public List<Player> GetOnlinePlayersPage(int offset, int limit)
    {
        if (limit <= 0) return new List<Player>();
        var all = GetOnlinePlayers();
        var from = Math.Max(0, offset);
        if (from >= all.Count) return new List<Player>();
        var to = Math.Min(from + limit, all.Count);
        return all.GetRange(from, to - from);
    }

    public async Task CreatePlayerDirect(Player player)
    {
        await _tableLock.WaitAsync();
        try
        {
            _players[player.Id] = player;
            _usernameIndex[player.Username] = player.Id;
            _snapshotCache.TryRemove(player.Id, out _);
        }
        finally
        {
            _tableLock.Release();
        }
    }

    public async Task SetOnline(string playerId, bool online)
    {
        await WithPlayerLock(playerId, player =>
        {
            player.IsOnline = online;
            if (!online) _sessions.TryRemove(player.Id, out _);
            return player;
        });
    }

    public async Task DeletePlayer(string playerId)
    {
        await _tableLock.WaitAsync();
        try
        {
            if (!_players.TryRemove(playerId, out var p)) return;
            _usernameIndex.TryRemove(p.Username, out _);
            _sessions.TryRemove(playerId, out _);
            _snapshotCache.TryRemove(playerId, out _);
            _playerLocks.TryRemove(playerId, out _);
        }
        finally
        {
            _tableLock.Release();
        }
    }

    // --- Existing IPlayerService interface methods ---

    public Player? GetPlayer(string playerId) =>
        _players.TryGetValue(playerId, out var player) ? player : null;

    public Player? GetPlayerByUsername(string username)
    {
        if (_usernameIndex.TryGetValue(username, out var playerId))
            return GetPlayer(playerId);
        return null;
    }

    public Player? Authenticate(string username, string password)
    {
        var player = GetPlayerByUsername(username);
        if (player is null) return null;
        try
        {
            return _passwordHasher.Verify(password, player.PasswordHash) ? player : null;
        }
        catch
        {
            return null;
        }
    }

    public Player CreatePlayer(string username, string passwordHash, string displayName = "")
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var player = new Player
        {
            Username = username,
            PasswordHash = passwordHash,
            Cpu = 100,
            MaxCpu = 100,
            Ram = 256,
            MaxRam = 256,
            Bandwidth = 50,
            MaxBandwidth = 50,
            LastLoginAt = now
        };

        _players[player.Id] = player;
        _usernameIndex[username] = player.Id;
        return player;
    }

    public void UpdatePlayer(Player player)
    {
        _players[player.Id] = player;
        _usernameIndex[player.Username] = player.Id;
    }

    public IEnumerable<Player> GetAllPlayers() => _players.Values;

    public void LoadFromJson(string json)
    {
        var players = JsonSerializer.Deserialize<List<Player>>(json, JsonOptions);
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

    private static string NewSessionToken()
    {
        var bytes = new byte[24];
        RandomNumberGenerator.Fill(bytes);
        return "sess_" + Convert.ToBase64String(bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_');
    }

    private record CachedSnapshot(PlayerSnapshot Snapshot, long At);
}
