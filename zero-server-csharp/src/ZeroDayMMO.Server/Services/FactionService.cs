using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public record FactionSummary(
    string Id,
    string Name,
    string Tag,
    int MemberCount,
    int Level,
    string LeaderName,
    bool IsOpen
);

public class FactionService : IHostedService
{
    private readonly IPlayerService _playerService;
    private readonly GameEventBus? _gameEventBus;
    private readonly Dictionary<string, Faction> _factions = new();
    private readonly SemaphoreSlim _mutex = new(1, 1);
    private CancellationTokenSource? _cts;
    private Task? _loopTask;

    public FactionService(IPlayerService playerService, GameEventBus? gameEventBus = null)
    {
        _playerService = playerService;
        _gameEventBus = gameEventBus;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _loopTask = PassiveIncomeLoopAsync(_cts.Token);
        return Task.CompletedTask;
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        _cts?.Cancel();
        if (_loopTask is not null)
        {
            try { await _loopTask; } catch (OperationCanceledException) { }
        }
    }

    private async Task PassiveIncomeLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay(60_000, ct);
            await ProcessPassiveIncome();
        }
    }

    public async Task<Faction> CreateFaction(string playerId, string name, string tag, string description)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");
            if (player.Level < 5) throw new InvalidOperationException("Level 5 required to create a faction");
            if (player.FactionId is not null) throw new InvalidOperationException("Already in a faction");
            if (_factions.Values.Any(f => string.Equals(f.Name, name, StringComparison.OrdinalIgnoreCase)))
                throw new InvalidOperationException("Faction name already taken");
            if (_factions.Values.Any(f => string.Equals(f.Tag, tag, StringComparison.OrdinalIgnoreCase)))
                throw new InvalidOperationException("Faction tag already taken");

            var faction = new Faction
            {
                Name = name,
                Tag = tag[..Math.Min(5, tag.Length)].ToUpperInvariant(),
                Description = description,
                LeaderId = playerId,
                Members = new List<string> { playerId }
            };
            _factions[faction.Id] = faction;
            player.FactionId = faction.Id;
            player.Credits -= 1000;
            _playerService.UpdatePlayer(player);
            return faction;
        }
        finally { _mutex.Release(); }
    }

    public async Task<Faction> JoinFaction(string playerId, string factionId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");
            if (!_factions.TryGetValue(factionId, out var faction))
                throw new InvalidOperationException("Faction not found");
            if (player.FactionId is not null) throw new InvalidOperationException("Already in a faction");
            if (!faction.IsOpen && faction.LeaderId != playerId)
                throw new InvalidOperationException("Faction is invite-only");
            if (faction.Members.Count >= GetMemberLimit(faction))
                throw new InvalidOperationException("Faction is full");

            faction.Members.Add(playerId);
            player.FactionId = factionId;
            _playerService.UpdatePlayer(player);
            _gameEventBus?.Publish(new GameEvent { PlayerId = playerId, Type = AchievementEvent.FACTION_JOINED, Value = 1 });
            return faction;
        }
        finally { _mutex.Release(); }
    }

    public async Task LeaveFaction(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");
            var factionId = player.FactionId ?? throw new InvalidOperationException("Not in a faction");
            if (!_factions.TryGetValue(factionId, out var faction))
                throw new InvalidOperationException("Faction not found");

            if (faction.LeaderId == playerId)
            {
                if (faction.Members.Count > 1)
                    throw new InvalidOperationException("Transfer leadership before leaving");
                _factions.Remove(factionId);
            }
            else
            {
                faction.Members.Remove(playerId);
            }
            player.FactionId = null;
            _playerService.UpdatePlayer(player);
        }
        finally { _mutex.Release(); }
    }

    public async Task<FactionMainframe> DonateToMainframe(string playerId, long cpu = 0, long ram = 0, long bandwidth = 0, long credits = 0)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");
            var factionId = player.FactionId ?? throw new InvalidOperationException("Not in a faction");
            if (!_factions.TryGetValue(factionId, out var faction))
                throw new InvalidOperationException("Faction not found");

            var mf = faction.Mainframe;
            if (cpu > 0)
            {
                if (player.Cpu < (int)cpu) throw new InvalidOperationException("Not enough CPU");
                player.Cpu -= (int)cpu;
                mf.CpuUsed = Math.Max(0, mf.CpuUsed - cpu);
                mf.CpuTotal += cpu;
            }
            if (ram > 0)
            {
                if (player.Ram < (int)ram) throw new InvalidOperationException("Not enough RAM");
                player.Ram -= (int)ram;
                mf.RamUsed = Math.Max(0, mf.RamUsed - ram);
                mf.RamTotal += ram;
            }
            if (bandwidth > 0)
            {
                if (player.Bandwidth < (int)bandwidth) throw new InvalidOperationException("Not enough bandwidth");
                mf.BandwidthTotal += bandwidth;
            }
            if (credits > 0)
            {
                if (player.Credits < credits) throw new InvalidOperationException("Not enough credits");
                player.Credits -= credits;
                mf.CreditsPool += credits;
            }
            _playerService.UpdatePlayer(player);
            return mf;
        }
        finally { _mutex.Release(); }
    }

    public async Task<FactionMainframe> UpgradeMainframe(string playerId, string upgradeId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");
            var factionId = player.FactionId ?? throw new InvalidOperationException("Not in a faction");
            if (!_factions.TryGetValue(factionId, out var faction))
                throw new InvalidOperationException("Faction not found");
            if (faction.LeaderId != playerId) throw new InvalidOperationException("Only the leader can upgrade");

            var upgrade = FactionUpgradesRegistry.AllUpgrades.Find(u => u.Id == upgradeId)
                ?? throw new InvalidOperationException("Unknown upgrade");

            var currentLevel = faction.Mainframe.UpgradeProgress.GetValueOrDefault(upgradeId, 0);
            var nextLevel = upgrade.Levels.Find(l => l.Level == (int)currentLevel + 1)
                ?? throw new InvalidOperationException("Max level reached for this upgrade");

            if (faction.Level < nextLevel.RequiredFactionLevel)
                throw new InvalidOperationException($"Faction level {nextLevel.RequiredFactionLevel} required");

            if (faction.Mainframe.CreditsPool < nextLevel.CostCredits)
                throw new InvalidOperationException("Not enough credits in pool");
            if (faction.Mainframe.CpuTotal - faction.Mainframe.CpuUsed < nextLevel.CostCpu)
                throw new InvalidOperationException("Not enough CPU capacity");
            if (faction.Mainframe.RamTotal - faction.Mainframe.RamUsed < nextLevel.CostRam)
                throw new InvalidOperationException("Not enough RAM capacity");

            faction.Mainframe.CreditsPool -= nextLevel.CostCredits;
            faction.Mainframe.CpuUsed += nextLevel.CostCpu;
            faction.Mainframe.RamUsed += nextLevel.CostRam;
            faction.Mainframe.UpgradeProgress[upgradeId] = (int)currentLevel + 1;

            ApplyUpgradeBenefit(faction, upgrade.Category, nextLevel);
            return faction.Mainframe;
        }
        finally { _mutex.Release(); }
    }

    private static void ApplyUpgradeBenefit(Faction faction, UpgradeCategory category, UpgradeLevel level)
    {
        switch (category)
        {
            case UpgradeCategory.CPU_CAPACITY:
            case UpgradeCategory.RAM_CAPACITY:
            case UpgradeCategory.BANDWIDTH_CAPACITY:
            case UpgradeCategory.MEMBER_LIMIT:
            case UpgradeCategory.BUFF_DURATION:
                break;
            case UpgradeCategory.SECURITY:
                faction.Mainframe.SecurityLevel += (int)level.Value;
                break;
            case UpgradeCategory.PASSIVE_INCOME:
                faction.Mainframe.PassiveIncomeRate = level.Value;
                break;
            case UpgradeCategory.FEATURE_UNLOCK:
                var features = new Dictionary<int, string>
                {
                    { 1, "faction_scan" },
                    { 2, "mass_ddos" },
                    { 3, "faction_shield" }
                };
                if (features.TryGetValue(level.Level, out var feature))
                    faction.Mainframe.UnlockedFeatures.Add(feature);
                break;
        }
    }

    public async Task<Faction> GetFaction(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");
            var factionId = player.FactionId ?? throw new InvalidOperationException("Not in a faction");
            if (!_factions.TryGetValue(factionId, out var faction))
                throw new InvalidOperationException("Faction not found");
            return faction;
        }
        finally { _mutex.Release(); }
    }

    public async Task<List<FactionSummary>> ListFactions()
    {
        await _mutex.WaitAsync();
        try
        {
            return _factions.Values.Select(f =>
            {
                var leader = _playerService.GetPlayer(f.LeaderId);
                return new FactionSummary(
                    f.Id, f.Name, f.Tag,
                    f.Members.Count, f.Level,
                    leader?.Username ?? "Unknown",
                    f.IsOpen
                );
            }).ToList();
        }
        finally { _mutex.Release(); }
    }

    public async Task<Faction?> GetFactionById(string factionId)
    {
        await _mutex.WaitAsync();
        try
        {
            return _factions.TryGetValue(factionId, out var faction) ? faction : null;
        }
        finally { _mutex.Release(); }
    }

    private async Task ProcessPassiveIncome()
    {
        await _mutex.WaitAsync();
        try
        {
            foreach (var (_, faction) in _factions)
            {
                if (faction.Mainframe.PassiveIncomeRate > 0 && faction.Members.Count > 0)
                {
                    var incomePerMember = (long)(faction.Mainframe.PassiveIncomeRate * faction.Mainframe.Level);
                    foreach (var memberId in faction.Members)
                    {
                        var member = _playerService.GetPlayer(memberId);
                        if (member is not null)
                        {
                            member.Credits += incomePerMember;
                            _playerService.UpdatePlayer(member);
                        }
                    }
                    faction.Mainframe.CreditsPool += incomePerMember * faction.Members.Count / 10;
                }
            }
        }
        finally { _mutex.Release(); }
    }

    private static int GetMemberLimit(Faction faction)
    {
        var level = faction.Mainframe.UpgradeProgress.GetValueOrDefault("member_limit");
        return level switch
        {
            0 => 10,
            1 => 15,
            2 => 25,
            3 => 50,
            _ => 10
        };
    }

    public async Task<List<string>> GetOnlineMembers(string factionId)
    {
        await _mutex.WaitAsync();
        try
        {
            if (!_factions.TryGetValue(factionId, out var faction))
                return new List<string>();
            return faction.Members.Where(memberId =>
            {
                var p = _playerService.GetPlayer(memberId);
                return p?.IsOnline == true;
            }).ToList();
        }
        finally { _mutex.Release(); }
    }
}
