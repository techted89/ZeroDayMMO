namespace ZeroDayMMO.Server.Services;

public class PlayerPersistence : IHostedService
{
    private readonly IPlayerService _playerService;
    private readonly Config.ServerConfig _config;
    private readonly string _filePath;
    private readonly string _tempPath;
    private CancellationTokenSource? _cts;
    private Task? _loopTask;

    public PlayerPersistence(IPlayerService playerService, Config.ServerConfig config)
    {
        _playerService = playerService;
        _config = config;
        _filePath = Path.GetFullPath("data/players.json");
        _tempPath = _filePath + ".tmp";
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        LoadPlayers();
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _loopTask = SaveLoopAsync(_cts.Token);
        return Task.CompletedTask;
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        _cts?.Cancel();
        if (_loopTask is not null)
        {
            try { await _loopTask; } catch (OperationCanceledException) { }
        }
        await SavePlayersAsync();
    }

    private void LoadPlayers()
    {
        try
        {
            if (File.Exists(_filePath))
            {
                var json = File.ReadAllText(_filePath);
                _playerService.LoadFromJson(json);
                Console.WriteLine($"Loaded players from {_filePath}");
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Failed to load players: {ex.Message}");
        }
    }

    private async Task SaveLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay(TimeSpan.FromSeconds(_config.PlayerSaveIntervalSeconds), ct);
            await SavePlayersAsync();
        }
    }

    private async Task SavePlayersAsync()
    {
        try
        {
            var json = _playerService.SaveToJson();
            var dir = Path.GetDirectoryName(_filePath);
            if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                Directory.CreateDirectory(dir);

            await File.WriteAllTextAsync(_tempPath, json);
            File.Move(_tempPath, _filePath, overwrite: true);
            Console.WriteLine($"Saved players to {_filePath}");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Failed to save players: {ex.Message}");
        }
    }
}
