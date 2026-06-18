using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public interface IWorldZoneService
{
    Task<Zone?> GetZone(string zoneId);
}

public class CoopBossService : IHostedService
{
    private readonly IPlayerService _playerService;
    private readonly IWorldZoneService _worldZoneService;
    private readonly GameEventBus? _gameEventBus;

    private readonly Dictionary<string, BossInstance> _instances = new();
    private readonly SemaphoreSlim _mutex = new(1, 1);
    private CancellationTokenSource? _cts;
    private Task? _cleanupTask;
    private Task? _timerCheckTask;

    private const int MinPlayers = 2;
    private const int MaxPlayers = 3;
    private const long InstanceTimeoutMs = 600_000;
    private const long TurnTimeoutMs = 30_000;
    private const int TimerCheckIntervalMs = 5_000;

    public enum BossPhase { Phase1, Phase2, Phase3 }
    public enum BossState { Waiting, InProgress, Completed, Expired }
    public enum BossResult { Victory, Defeat, NoShow }
    public enum StatusEffect { None, Burn, Stun, Shield }

    public class BossInstance
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string ZoneId { get; set; } = "";
        public string BossName { get; set; } = "";
        public int BossLevel { get; set; }
        public int BossPower { get; set; }
        public int ThreatLevel { get; set; }
        public int CurrentHp { get; set; }
        public List<BossParticipant> Participants { get; set; } = new();
        public BossState State { get; set; } = BossState.Waiting;
        public BossPhase Phase { get; set; } = BossPhase.Phase1;
        public long CreatedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        public long ResolvedAt { get; set; }
        public BossResult? Result { get; set; }
        public int CurrentTurnPlayerIndex { get; set; }
        public long LastActionTime { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        public int TurnNumber { get; set; }
    }

    public class BossParticipant
    {
        public string PlayerId { get; set; } = "";
        public string Username { get; set; } = "";
        public int PlayerLevel { get; set; }
        public int PlayerPower { get; set; }
        public int CurrentHp { get; set; }
        public int MaxHp { get; set; }
        public int ContributionScore { get; set; }
        public bool Survived { get; set; } = true;
        public StatusEffect StatusEffect { get; set; } = StatusEffect.None;
        public int StatusDuration { get; set; }
        public bool ShieldActive { get; set; }
        public float DodgeBuff { get; set; }
    }

    public record PlayerBossAction(
        string Action,
        int Damage = 0,
        int HealAmount = 0,
        bool IsCrit = false,
        bool IsDodged = false,
        string Message = ""
    );

    public record BossActionResult(
        PlayerBossAction Action,
        BossPhase BossPhase,
        int BossHp,
        int BossMaxHp,
        bool PhaseChanged = false,
        string AbilityName = "",
        int AbilityDamage = 0,
        string? KnockedOut = null,
        bool FightOver = false,
        string? Result = null
    );

    public CoopBossService(IPlayerService playerService, IWorldZoneService worldZoneService, GameEventBus? gameEventBus = null)
    {
        _playerService = playerService;
        _worldZoneService = worldZoneService;
        _gameEventBus = gameEventBus;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _cleanupTask = CleanupLoopAsync(_cts.Token);
        _timerCheckTask = TimerCheckLoopAsync(_cts.Token);
        return Task.CompletedTask;
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        _cts?.Cancel();
        var tasks = new[] { _cleanupTask, _timerCheckTask }.Where(t => t is not null).ToArray();
        try { await Task.WhenAll(tasks); } catch (OperationCanceledException) { }
    }

    private async Task CleanupLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay(30_000, ct);
            await CleanupStaleInstances();
        }
    }

    private async Task TimerCheckLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay(TimerCheckIntervalMs, ct);
            await CheckTurnTimeouts();
        }
    }

    public async Task<BossInstance> CreateInstance(string zoneId, string creatorPlayerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var zone = await _worldZoneService.GetZone(zoneId);
            if (zone is null) throw new InvalidOperationException("Zone not found");
            if (!zone.HasBoss) throw new InvalidOperationException("No boss in this zone");

            var creator = _playerService.GetPlayer(creatorPlayerId);
            if (creator is null) throw new InvalidOperationException("Player not found");

            var existingInstance = _instances.Values.FirstOrDefault(i =>
                i.ZoneId == zoneId && i.State == BossState.Waiting &&
                i.Participants.Any(p => p.PlayerId == creatorPlayerId));
            if (existingInstance is not null)
                throw new InvalidOperationException("You already have a pending instance in this zone");

            var playerPower = creator.Level * 10 + (int)(creator.BreakthroughMultiplier * 100);
            var bossPower = zone.BossLevel * 15 + zone.ThreatLevel;
            var playerMaxHp = Math.Max(playerPower, 50);

            var instance = new BossInstance
            {
                ZoneId = zoneId,
                BossName = zone.BossName,
                BossLevel = zone.BossLevel,
                BossPower = bossPower,
                ThreatLevel = zone.ThreatLevel,
                CurrentHp = bossPower,
                Participants = new List<BossParticipant>
                {
                    new()
                    {
                        PlayerId = creatorPlayerId,
                        Username = creator.Username,
                        PlayerLevel = creator.Level,
                        PlayerPower = playerPower,
                        CurrentHp = playerMaxHp,
                        MaxHp = playerMaxHp
                    }
                }
            };
            _instances[instance.Id] = instance;
            return instance;
        }
        finally { _mutex.Release(); }
    }

    public async Task<BossInstance> JoinInstance(string instanceId, string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            if (!_instances.TryGetValue(instanceId, out var instance))
                throw new InvalidOperationException("Instance not found");
            if (instance.State != BossState.Waiting)
                throw new InvalidOperationException("Boss fight already started or ended");
            if (instance.Participants.Count >= MaxPlayers)
                throw new InvalidOperationException("Instance is full");
            if (instance.Participants.Any(p => p.PlayerId == playerId))
                throw new InvalidOperationException("Already in this instance");

            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");

            var playerPower = player.Level * 10 + (int)(player.BreakthroughMultiplier * 100);
            var zone = await _worldZoneService.GetZone(instance.ZoneId);
            if (zone is not null && player.Level < zone.RequiredLevel)
                throw new InvalidOperationException($"Level {zone.RequiredLevel} required for this zone");

            var playerMaxHp = Math.Max(playerPower, 50);
            instance.Participants.Add(new BossParticipant
            {
                PlayerId = playerId,
                Username = player.Username,
                PlayerLevel = player.Level,
                PlayerPower = playerPower,
                CurrentHp = playerMaxHp,
                MaxHp = playerMaxHp
            });

            if (instance.Participants.Count >= MinPlayers)
            {
                instance.State = BossState.InProgress;
                instance.LastActionTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            }

            return instance;
        }
        finally { _mutex.Release(); }
    }

    public async Task<BossActionResult> PerformAction(string instanceId, string playerId, string action)
    {
        await _mutex.WaitAsync();
        try
        {
            if (!_instances.TryGetValue(instanceId, out var instance))
                throw new InvalidOperationException("Instance not found");
            if (instance.State != BossState.InProgress)
                throw new InvalidOperationException("Fight is not in progress");

            var participant = instance.Participants.FirstOrDefault(p => p.PlayerId == playerId)
                ?? throw new InvalidOperationException("Not a participant");
            if (!participant.Survived)
                throw new InvalidOperationException("You are knocked out");

            instance.LastActionTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            instance.TurnNumber++;

            var playerAction = ResolvePlayerAction(participant, action, instance);
            if (playerAction.Action == "invalid")
                throw new InvalidOperationException("Invalid action: use attack/scan/defend/heal");

            if (participant.StatusEffect == StatusEffect.Burn && participant.Survived)
            {
                var burnDmg = Math.Max((int)(participant.MaxHp * 0.08), 5);
                participant.CurrentHp = Math.Max(participant.CurrentHp - burnDmg, 0);
                playerAction = playerAction with { Message = playerAction.Message + $" | Burn: -{burnDmg} HP" };
                if (participant.CurrentHp <= 0)
                    participant.Survived = false;
            }

            if (participant.StatusDuration > 0)
            {
                participant.StatusDuration--;
                if (participant.StatusDuration <= 0)
                    participant.StatusEffect = StatusEffect.None;
            }

            if (instance.CurrentHp <= 0)
            {
                await ResolveFight(instance, BossResult.Victory);
                return new BossActionResult(
                    playerAction, instance.Phase, 0, instance.BossPower,
                    FightOver: true, Result: "victory"
                );
            }

            var newPhase = GetPhaseForHp(instance.CurrentHp, instance.BossPower);
            var phaseChanged = newPhase != instance.Phase;
            instance.Phase = newPhase;

            var (bossDmg, abilityName, knockedOut) = ExecuteBossTurn(instance);

            if (instance.Participants.All(p => !p.Survived))
            {
                await ResolveFight(instance, BossResult.Defeat);
                return new BossActionResult(
                    playerAction, instance.Phase, Math.Max(instance.CurrentHp, 0),
                    instance.BossPower, PhaseChanged: phaseChanged,
                    AbilityName: abilityName, AbilityDamage: bossDmg,
                    FightOver: true, Result: "defeat"
                );
            }

            AdvanceTurn(instance);

            return new BossActionResult(
                playerAction, instance.Phase, Math.Max(instance.CurrentHp, 0),
                instance.BossPower, PhaseChanged: phaseChanged,
                AbilityName: abilityName, AbilityDamage: bossDmg,
                KnockedOut: knockedOut
            );
        }
        finally { _mutex.Release(); }
    }

    private static PlayerBossAction ResolvePlayerAction(BossParticipant participant, string action, BossInstance instance)
    {
        return action.ToLowerInvariant() switch
        {
            "attack" => ResolveAttack(participant, instance),
            "scan" => ResolveScan(participant, instance),
            "defend" => ResolveDefend(participant, instance),
            "heal" => ResolveHeal(participant, instance),
            _ => new PlayerBossAction("invalid", Message: "Unknown action")
        };
    }

    private static PlayerBossAction ResolveAttack(BossParticipant participant, BossInstance instance)
    {
        var dmg = (int)(participant.PlayerPower * (0.4 + Random.Shared.NextDouble() * 0.6));
        var isCrit = Random.Shared.NextDouble() < 0.15;
        if (isCrit) dmg = (int)(dmg * 1.5);
        if (participant.StatusEffect == StatusEffect.Stun) dmg = (int)(dmg * 0.5);

        participant.ContributionScore += dmg;
        instance.CurrentHp -= dmg;

        if (isCrit && Random.Shared.NextDouble() < 0.3)
            ApplyStatusToBoss(instance, StatusEffect.Burn, 2);

        var msg = isCrit ? $"CRIT! Dealt {dmg} damage to {instance.BossName}"
            : $"Dealt {dmg} damage to {instance.BossName}";
        return new PlayerBossAction("attack", dmg, IsCrit: isCrit, Message: msg);
    }

    private static PlayerBossAction ResolveScan(BossParticipant participant, BossInstance instance)
    {
        var bonus = (int)(participant.PlayerPower * 0.2) + 1;
        participant.ContributionScore += bonus;
        participant.DodgeBuff = 0.15f;
        var hpPct = instance.BossPower > 0 ? instance.CurrentHp * 100 / instance.BossPower : 0;
        return new PlayerBossAction("scan", 0, Message: $"Scanned {instance.BossName} — HP at {hpPct}%, Phase: {instance.Phase}");
    }

    private static PlayerBossAction ResolveDefend(BossParticipant participant, BossInstance instance)
    {
        participant.ShieldActive = true;
        participant.DodgeBuff = 0.25f;
        return new PlayerBossAction("defend", 0, Message: $"Shields raised against {instance.BossName}'s next attack");
    }

    private static PlayerBossAction ResolveHeal(BossParticipant participant, BossInstance instance)
    {
        var restored = Math.Max((int)(participant.PlayerPower * 0.2), 10);
        var actualHeal = Math.Min(participant.CurrentHp + restored, participant.MaxHp) - participant.CurrentHp;
        participant.CurrentHp += actualHeal;
        participant.ContributionScore += actualHeal / 2;

        if (participant.StatusEffect == StatusEffect.Burn)
        {
            participant.StatusEffect = StatusEffect.None;
            participant.StatusDuration = 0;
            return new PlayerBossAction("heal", HealAmount: actualHeal, Message: $"Restored {actualHeal} HP and cleansed burn");
        }
        return new PlayerBossAction("heal", HealAmount: actualHeal, Message: $"Restored {actualHeal} HP to self");
    }

    private static (int Damage, string AbilityName, string? KnockedOut) ExecuteBossTurn(BossInstance instance)
    {
        var (baseDmgPct, abilityName) = instance.Phase switch
        {
            BossPhase.Phase1 => (0.08, "Data Spike"),
            BossPhase.Phase2 => (0.14, "Firewall Surge"),
            BossPhase.Phase3 => (0.22, "System Purge"),
            _ => (0.08, "Data Spike")
        };

        var baseDmg = (int)(instance.BossPower * baseDmgPct);
        var alive = instance.Participants.Where(p => p.Survived).ToList();
        var totalDmg = 0;
        string? knockedOut = null;

        switch (instance.Phase)
        {
            case BossPhase.Phase1:
            {
                var target = alive.Count > 0 ? alive[Random.Shared.Next(alive.Count)] : null;
                if (target is null) return (0, abilityName, null);
                var dmg = ResolveBossDamage(target, baseDmg, abilityName);
                totalDmg = dmg;
                if (!target.Survived) knockedOut = target.Username;
                break;
            }
            case BossPhase.Phase2:
            {
                if (Random.Shared.NextDouble() < 0.4 && alive.Count >= 2)
                {
                    var hits = Random.Shared.Next(2, 4);
                    for (var i = 0; i < hits; i++)
                    {
                        var remainingAlive = instance.Participants.Where(p => p.Survived).ToList();
                        if (remainingAlive.Count == 0) break;
                        var t = remainingAlive[Random.Shared.Next(remainingAlive.Count)];
                        var dmg = ResolveBossDamage(t, Math.Max((int)(baseDmg * 0.5), 5), abilityName);
                        totalDmg += dmg;
                        if (!t.Survived) knockedOut = t.Username;
                    }
                }
                else
                {
                    var target = alive.Count > 0 ? alive[Random.Shared.Next(alive.Count)] : null;
                    if (target is null) return (0, abilityName, null);
                    var dmg = ResolveBossDamage(target, (int)(baseDmg * 1.2), $"{abilityName}+");
                    totalDmg = dmg;
                    if (!target.Survived) knockedOut = target.Username;
                }
                break;
            }
            case BossPhase.Phase3:
            {
                if (Random.Shared.NextDouble() < 0.35)
                    return (0, $"{abilityName} (miss)", null);
                foreach (var target in alive)
                {
                    var dmg = ResolveBossDamage(target, (int)(baseDmg * 0.8), abilityName);
                    totalDmg += dmg;
                    if (!target.Survived) knockedOut = target.Username;
                }
                break;
            }
        }

        return (totalDmg, abilityName, knockedOut);
    }

    private static int ResolveBossDamage(BossParticipant participant, int damage, string abilityName)
    {
        var dodgeChance = 0.1f + participant.DodgeBuff;
        if (Random.Shared.NextDouble() < dodgeChance)
        {
            participant.DodgeBuff = 0f;
            return 0;
        }

        var dmg = damage;
        if (participant.ShieldActive)
        {
            dmg = (int)(dmg * 0.5);
            participant.ShieldActive = false;
        }

        participant.CurrentHp = Math.Max(participant.CurrentHp - dmg, 0);
        participant.DodgeBuff = 0f;

        if (participant.CurrentHp <= 0)
            participant.Survived = false;

        return dmg;
    }

    private static void ApplyStatusToBoss(BossInstance instance, StatusEffect effect, int duration)
    {
    }

    private static BossPhase GetPhaseForHp(int currentHp, int maxHp)
    {
        var pct = maxHp > 0 ? (float)currentHp / maxHp : 0f;
        if (pct <= 0.33f) return BossPhase.Phase3;
        if (pct <= 0.66f) return BossPhase.Phase2;
        return BossPhase.Phase1;
    }

    private static void AdvanceTurn(BossInstance instance)
    {
        var alive = instance.Participants.Where(p => p.Survived).ToList();
        if (alive.Count == 0) return;

        var currentIdx = alive.FindIndex(p => p.PlayerId ==
            instance.Participants.ElementAtOrDefault(instance.CurrentTurnPlayerIndex)?.PlayerId);
        var nextIdx = currentIdx >= 0 ? (currentIdx + 1) % alive.Count : 0;
        var nextPlayer = alive[nextIdx];
        instance.CurrentTurnPlayerIndex = instance.Participants.FindIndex(p => p.PlayerId == nextPlayer.PlayerId);
    }

    private async Task CheckTurnTimeouts()
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        await _mutex.WaitAsync();
        try
        {
            foreach (var instance in _instances.Values)
            {
                if (instance.State == BossState.InProgress && now - instance.LastActionTime > TurnTimeoutMs)
                {
                    var currentPlayer = instance.Participants.ElementAtOrDefault(instance.CurrentTurnPlayerIndex);
                    if (currentPlayer is not null && currentPlayer.Survived)
                    {
                        currentPlayer.ShieldActive = true;
                        AdvanceTurn(instance);
                        instance.LastActionTime = now;
                    }
                }
            }
        }
        finally { _mutex.Release(); }
    }

    private async Task ResolveFight(BossInstance instance, BossResult result)
    {
        instance.State = BossState.Completed;
        instance.Result = result;
        instance.ResolvedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        if (result == BossResult.Victory)
        {
            var baseCredits = instance.BossLevel * 200 + instance.ThreatLevel * 10;
            var baseXp = instance.BossLevel * 50 + instance.ThreatLevel * 2;
            var perPlayerCredits = (long)Math.Max((baseCredits * 1.5 / instance.Participants.Count), 50);
            var perPlayerXp = Math.Max((int)(baseXp * 1.5 / instance.Participants.Count), 20);

            var topContributor = instance.Participants.MaxBy(p => p.ContributionScore);
            var bonusCredits = (long)(perPlayerCredits * 0.2);

            foreach (var p in instance.Participants)
            {
                var player = _playerService.GetPlayer(p.PlayerId);
                if (player is not null)
                {
                    player.Credits += perPlayerCredits;
                    player.Experience += perPlayerXp;
                    if (p == topContributor)
                        player.Credits += bonusCredits;

                    while (player.Experience >= player.ExperienceToNext)
                    {
                        player.Level += 1;
                        player.Experience -= player.ExperienceToNext;
                        player.ExperienceToNext = (long)(player.ExperienceToNext * 1.2);
                    }
                    _playerService.UpdatePlayer(player);
                }
                _gameEventBus?.Publish(new GameEvent { PlayerId = p.PlayerId, Type = AchievementEvent.WORLD_EVENT_PARTICIPATED, Value = instance.BossLevel });
            }
        }
    }

    private async Task CleanupStaleInstances()
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        await _mutex.WaitAsync();
        try
        {
            var staleIds = _instances.Values
                .Where(i => now - i.CreatedAt > InstanceTimeoutMs && i.State == BossState.Waiting)
                .Select(i => i.Id)
                .ToList();
            foreach (var id in staleIds)
            {
                if (_instances.TryGetValue(id, out var inst))
                    inst.State = BossState.Expired;
            }

            var expiredIds = _instances.Values
                .Where(i => i.State == BossState.Completed && now - i.ResolvedAt > 300_000)
                .Select(i => i.Id)
                .ToList();
            foreach (var id in expiredIds)
                _instances.Remove(id);
        }
        finally { _mutex.Release(); }
    }

    public async Task<BossInstance?> GetInstance(string instanceId)
    {
        await _mutex.WaitAsync();
        try
        {
            return _instances.TryGetValue(instanceId, out var instance) ? instance : null;
        }
        finally { _mutex.Release(); }
    }

    public async Task<BossInstance?> GetPlayerActiveInstance(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            return _instances.Values.FirstOrDefault(i =>
                i.State != BossState.Completed && i.State != BossState.Expired &&
                i.Participants.Any(p => p.PlayerId == playerId));
        }
        finally { _mutex.Release(); }
    }

    public async Task<List<BossInstance>> GetAvailableInstances(string zoneId)
    {
        await _mutex.WaitAsync();
        try
        {
            return _instances.Values
                .Where(i => i.ZoneId == zoneId && i.State == BossState.Waiting && i.Participants.Count < MaxPlayers)
                .ToList();
        }
        finally { _mutex.Release(); }
    }

    public Dictionary<string, object?> SerializeInstance(BossInstance instance) => new()
    {
        ["id"] = instance.Id,
        ["zoneId"] = instance.ZoneId,
        ["bossName"] = instance.BossName,
        ["bossLevel"] = instance.BossLevel,
        ["bossPower"] = instance.BossPower,
        ["bossCurrentHp"] = Math.Max(instance.CurrentHp, 0),
        ["phase"] = instance.Phase.ToString(),
        ["state"] = instance.State.ToString(),
        ["turnNumber"] = instance.TurnNumber,
        ["currentTurnPlayerId"] = instance.Participants.ElementAtOrDefault(instance.CurrentTurnPlayerIndex)?.PlayerId,
        ["participants"] = instance.Participants.Select(p => (object)new Dictionary<string, object?>
        {
            ["playerId"] = p.PlayerId,
            ["username"] = p.Username,
            ["playerLevel"] = p.PlayerLevel,
            ["playerPower"] = p.PlayerPower,
            ["currentHp"] = p.CurrentHp,
            ["maxHp"] = p.MaxHp,
            ["survived"] = p.Survived,
            ["contributionScore"] = p.ContributionScore,
            ["statusEffect"] = p.StatusEffect.ToString(),
            ["shieldActive"] = p.ShieldActive
        }).ToList(),
        ["result"] = instance.Result?.ToString()
    };
}
