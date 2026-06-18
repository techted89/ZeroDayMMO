namespace ZeroDayMMO.Server.Services;

public class ResourceRegenTicker : IHostedService
{
    private readonly IPlayerService _playerService;
    private readonly Config.ServerConfig _config;
    private CancellationTokenSource? _cts;
    private Task? _loopTask;

    public ResourceRegenTicker(IPlayerService playerService, Config.ServerConfig config)
    {
        _playerService = playerService;
        _config = config;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _loopTask = RegenLoopAsync(_cts.Token);
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

    private async Task RegenLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay(TimeSpan.FromSeconds(_config.ResourceRegenIntervalSeconds), ct);
            RegenAllPlayers();
        }
    }

    private void RegenAllPlayers()
    {
        foreach (var player in _playerService.GetAllPlayers())
        {
            player.Cpu = Math.Min(player.MaxCpu, player.Cpu + 5);
            player.Ram = Math.Min(player.MaxRam, player.Ram + 5);
            player.Bandwidth = Math.Min(player.MaxBandwidth, player.Bandwidth + 5);
            player.LastLoginAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            _playerService.UpdatePlayer(player);
        }
    }
}
