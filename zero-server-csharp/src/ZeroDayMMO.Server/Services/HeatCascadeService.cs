using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public class HeatCascadeService : IHostedService
{
    private readonly IPlayerService _playerService;
    private readonly CareerService _careerService;
    private readonly GameEventBus? _gameEventBus;

    private readonly Dictionary<string, HunterSession> _hunters = new();
    private readonly SemaphoreSlim _mutex = new(1, 1);
    private CancellationTokenSource? _cts;
    private Task? _loopTask;

    private const int HeatThreshold = 60;
    private const long HuntIntervalMs = 30_000;

    public class HunterSession
    {
        public string HunterId { get; set; } = "";
        public string TargetId { get; set; } = "";
        public string TargetName { get; set; } = "";
        public int TargetHeat { get; set; }
        public long StartedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        public long LastAttemptAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        public int AttemptCount { get; set; }
    }

    public record HunterInfo(
        string HunterId,
        string TargetId,
        string TargetName,
        int TargetHeat,
        long TimeSinceLastAttemptMs,
        int AttemptCount
    );

    public HeatCascadeService(IPlayerService playerService, CareerService careerService, GameEventBus? gameEventBus = null)
    {
        _playerService = playerService;
        _careerService = careerService;
        _gameEventBus = gameEventBus;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _loopTask = HeatCascadeLoopAsync(_cts.Token);
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

    private async Task HeatCascadeLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay((int)HuntIntervalMs, ct);
            await ProcessHeatCascade();
        }
    }

    private async Task ProcessHeatCascade()
    {
        foreach (var player in _playerService.GetAllPlayers())
        {
            if (player.CareerPath != "blackhat") continue;
            if (player.IsInJail) continue;

            if (player.HeatLevel >= HeatThreshold)
            {
                await EnsureHunter(player.Id, player.Username, player.HeatLevel);
            }
            else
            {
                await _mutex.WaitAsync();
                try { _hunters.Remove(player.Id); }
                finally { _mutex.Release(); }
            }
        }

        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        List<HunterSession> active;
        await _mutex.WaitAsync();
        try { active = _hunters.Values.ToList(); }
        finally { _mutex.Release(); }

        foreach (var hunter in active)
        {
            if (now - hunter.LastAttemptAt >= HuntIntervalMs)
            {
                await ExecuteHunterAttempt(hunter);
            }
        }
    }

    private async Task EnsureHunter(string targetId, string targetName, int targetHeat)
    {
        await _mutex.WaitAsync();
        try
        {
            if (!_hunters.ContainsKey(targetId))
            {
                var hunterId = $"hunter_{targetId}_{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}";
                _hunters[targetId] = new HunterSession
                {
                    HunterId = hunterId,
                    TargetId = targetId,
                    TargetName = targetName,
                    TargetHeat = targetHeat
                };
                _gameEventBus?.Publish(new GameEvent { PlayerId = targetId, Type = AchievementEvent.HEAT_CHANGED, Value = targetHeat });
            }
        }
        finally { _mutex.Release(); }
    }

    private async Task ExecuteHunterAttempt(HunterSession hunter)
    {
        var target = _playerService.GetPlayer(hunter.TargetId);
        if (target is null) return;

        if (target.CareerPath != "blackhat" || target.HeatLevel < HeatThreshold || target.IsInJail)
        {
            await _mutex.WaitAsync();
            try { _hunters.Remove(hunter.TargetId); }
            finally { _mutex.Release(); }
            return;
        }

        var successChance = 0.1 + (target.HeatLevel - HeatThreshold) * 0.005;
        var success = Random.Shared.NextDouble() < Math.Clamp(successChance, 0.05, 0.5);

        if (success)
        {
            target.IsInJail = true;
            target.JailTimeRemaining = Math.Clamp(target.HeatLevel * 0.2f, 60f, 1800f);
            target.HeatLevel = Math.Max(target.HeatLevel / 2, 0);
            target.BountyPrice = 0L;
            _playerService.UpdatePlayer(target);

            _gameEventBus?.Publish(new GameEvent { PlayerId = hunter.TargetId, Type = AchievementEvent.ARREST_EXECUTED, Value = 1 });

            await _mutex.WaitAsync();
            try { _hunters.Remove(hunter.TargetId); }
            finally { _mutex.Release(); }
        }
        else
        {
            await _mutex.WaitAsync();
            try
            {
                if (_hunters.TryGetValue(hunter.TargetId, out var updated))
                {
                    updated.LastAttemptAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                    updated.AttemptCount += 1;

                    if (updated.AttemptCount >= 5)
                    {
                        target.HeatLevel = Math.Min(target.HeatLevel + 5, target.MaxHeatLevel);
                        _playerService.UpdatePlayer(target);
                    }
                }
            }
            finally { _mutex.Release(); }
        }
    }

    public async Task<List<HunterInfo>> GetActiveHunters()
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        await _mutex.WaitAsync();
        try
        {
            return _hunters.Values.Select(h => new HunterInfo(
                h.HunterId, h.TargetId, h.TargetName,
                h.TargetHeat, now - h.LastAttemptAt, h.AttemptCount
            )).ToList();
        }
        finally { _mutex.Release(); }
    }

    public async Task<bool> IsPlayerHunted(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            return _hunters.ContainsKey(playerId);
        }
        finally { _mutex.Release(); }
    }
}
