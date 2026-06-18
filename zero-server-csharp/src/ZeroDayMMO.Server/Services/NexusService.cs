using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public class NexusService
{
    private readonly Dictionary<string, List<NexusScript>> _playerScripts = new();
    private readonly NexusScriptEngine _engine;
    private readonly IPlayerService _playerService;
    private readonly GameEventBus? _gameEventBus;
    private readonly object _lock = new();

    public NexusService(
        IPlayerService playerService,
        GameEventBus? gameEventBus = null,
        IReadOnlyDictionary<string, GameCommand>? builtinCommands = null)
    {
        _playerService = playerService;
        _gameEventBus = gameEventBus;
        _engine = new NexusScriptEngine(builtinCommands ?? CommandRegistry.CommandMap);
    }

    public void InitializePlayer(string playerId)
    {
        lock (_lock)
        {
            if (!_playerScripts.ContainsKey(playerId))
                _playerScripts[playerId] = new List<NexusScript>();
        }
    }

    public NexusScriptEngine.ValidationResult ValidateCode(string code)
    {
        return _engine.Validate(code);
    }

    public NexusScript SaveScript(string playerId, string name, string code, string description)
    {
        var validation = _engine.Validate(code);
        if (!validation.Valid)
            throw new InvalidOperationException(
                $"Script validation failed: {string.Join("; ", validation.Errors ?? new List<string>())}");

        var script = new NexusScript(
            Name: name,
            AuthorId: playerId,
            Code: code,
            Description: description,
            Validated: true
        );

        lock (_lock)
        {
            if (!_playerScripts.ContainsKey(playerId))
                _playerScripts[playerId] = new List<NexusScript>();
            _playerScripts[playerId].Add(script);
        }

        _gameEventBus?.Publish(new GameEvent
        {
            PlayerId = playerId,
            Type = AchievementEvent.SCRIPT_SAVED,
            Value = 1
        });

        return script;
    }

    public List<NexusScript> GetPlayerScripts(string playerId)
    {
        lock (_lock)
        {
            return _playerScripts.TryGetValue(playerId, out var scripts)
                ? new List<NexusScript>(scripts)
                : new List<NexusScript>();
        }
    }

    public NexusScript? GetScript(string playerId, string scriptId)
    {
        lock (_lock)
        {
            return _playerScripts.TryGetValue(playerId, out var scripts)
                ? scripts.Find(s => s.Id == scriptId)
                : null;
        }
    }

    public bool DeleteScript(string playerId, string scriptId)
    {
        lock (_lock)
        {
            if (!_playerScripts.TryGetValue(playerId, out var scripts))
                return false;
            var removed = scripts.RemoveAll(s => s.Id == scriptId);
            return removed > 0;
        }
    }

    public NexusScriptEngine.ScriptResult RunCode(string playerId, string code)
    {
        var validation = _engine.Validate(code);
        if (!validation.Valid)
            throw new InvalidOperationException(
                string.Join("; ", validation.Errors ?? new List<string>()));

        var player = _playerService.GetPlayer(playerId);
        if (player is null)
            throw new InvalidOperationException("Player not found");

        var context = new ScriptContext(playerId, player.Level);
        var script = new NexusScript(
            Name: "inline",
            AuthorId: playerId,
            Code: code,
            Validated: true
        );
        var result = _engine.Execute(script, context);

        if (!result.Success)
            throw new InvalidOperationException(result.Error ?? "Unknown error");

        return result;
    }

    public NexusScriptEngine.ScriptResult ExecuteScript(
        string playerId,
        string scriptId,
        Dictionary<string, string>? scriptArgs = null)
    {
        scriptArgs ??= new Dictionary<string, string>();

        NexusScript script;
        lock (_lock)
        {
            script = _playerScripts.TryGetValue(playerId, out var scripts)
                ? scripts.Find(s => s.Id == scriptId)!
                : throw new InvalidOperationException("Script not found");
        }

        var player = _playerService.GetPlayer(playerId);
        if (player is null)
            throw new InvalidOperationException("Player not found");

        var context = new ScriptContext(playerId, player.Level);
        foreach (var (k, v) in scriptArgs)
            context.Variables[k] = NexusValue.StrVal(v);

        var result = _engine.Execute(script, context);

        if (!result.Success)
            throw new InvalidOperationException(result.Error ?? "Unknown error");

        return result;
    }
}
