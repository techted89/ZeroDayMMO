using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace ZeroDayMMO.Server.Services;

public class ServerScheduler : IHostedService, IDisposable
{
    private readonly PlayerService _playerService;
    private readonly ConnectionRegistry _connectionRegistry;
    private readonly PlayerPersistence? _persistence;
    private readonly ILogger<ServerScheduler> _logger;
    private Timer? _timer;

    public ServerScheduler(
        PlayerService playerService,
        ConnectionRegistry connectionRegistry,
        PlayerPersistence? persistence,
        ILogger<ServerScheduler> logger)
    {
        _playerService = playerService;
        _connectionRegistry = connectionRegistry;
        _persistence = persistence;
        _logger = logger;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _timer = new Timer(DoWork, null, TimeSpan.FromSeconds(60), TimeSpan.FromSeconds(60));
        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        _timer?.Change(Timeout.Infinite, 0);
        return Task.CompletedTask;
    }

    private void DoWork(object? state)
    {
        try
        {
            var online = _playerService.GetOnlinePlayers().Count;
            var total = _playerService.GetAllPlayers().Count();
            var conns = _connectionRegistry.ActiveConnections;

            if (online > 0 || conns > 0)
            {
                _logger.LogInformation("scheduler stats: online={Online} total={Total} connections={Conns}", online, total, conns);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "scheduler task failed");
        }
    }

    public void Dispose()
    {
        _timer?.Dispose();
    }
}
