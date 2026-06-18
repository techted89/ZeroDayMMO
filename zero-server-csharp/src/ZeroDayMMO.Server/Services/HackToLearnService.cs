using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public enum KnowledgeCategory
{
    RECONNAISSANCE, EXPLOITATION, CRYPTOGRAPHY,
    DEFENSE, STEALTH, NETWORKING, GENERAL
}

public class KnowledgeFragment
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Description { get; set; } = "";
    public KnowledgeCategory Category { get; set; }
    public List<string> RequiredFragments { get; set; } = new();
    public string? UnlocksCommand { get; set; }
    public string Rarity { get; set; } = "common";
    public string SourceHint { get; set; } = "";
}

public record KnowledgeMapData(
    int TotalFragments,
    int DiscoveredFragments,
    int UnlockedCommands,
    int TotalCommands,
    List<FragmentStatus> Fragments
);

public record FragmentStatus(
    string Id,
    string Name,
    string Description,
    string Category,
    string Rarity,
    bool Unlocked,
    bool Available,
    List<string> RequiredFragments,
    string? UnlocksCommand,
    string SourceHint
);

public class HackToLearnService
{
    private readonly IPlayerService _playerService;
    private readonly GameEventBus? _gameEventBus;
    private readonly Dictionary<string, HashSet<string>> _playerKnowledge = new();
    private readonly Dictionary<string, HashSet<string>> _playerDiscoveredCommands = new();
    private readonly SemaphoreSlim _mutex = new(1, 1);

    public HackToLearnService(IPlayerService playerService, GameEventBus? gameEventBus = null)
    {
        _playerService = playerService;
        _gameEventBus = gameEventBus;
    }

    public async Task InitializePlayer(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            _playerKnowledge.TryAdd(playerId, new HashSet<string>());
            _playerDiscoveredCommands.TryAdd(playerId, new HashSet<string>());
        }
        finally { _mutex.Release(); }
    }

    public async Task<bool> LearnKnowledge(string playerId, string fragmentId)
    {
        await _mutex.WaitAsync();
        try
        {
            var knowledge = _playerKnowledge.GetValueOrDefault(playerId);
            if (knowledge is null)
            {
                knowledge = new HashSet<string>();
                _playerKnowledge[playerId] = knowledge;
            }
            return knowledge.Add(fragmentId);
        }
        finally { _mutex.Release(); }
    }

    public async Task<HashSet<string>> GetPlayerKnowledge(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            return _playerKnowledge.GetValueOrDefault(playerId)?.ToHashSet() ?? new HashSet<string>();
        }
        finally { _mutex.Release(); }
    }

    public async Task<KnowledgeFragment> DiscoverFragment(string playerId, string fragmentId)
    {
        await _mutex.WaitAsync();
        try
        {
            var fragment = KnowledgeFragments.Find(f => f.Id == fragmentId)
                ?? throw new InvalidOperationException("Knowledge fragment not found");

            var known = _playerKnowledge.GetValueOrDefault(playerId);
            if (known is null)
            {
                known = new HashSet<string>();
                _playerKnowledge[playerId] = known;
            }

            if (known.Contains(fragmentId))
                throw new InvalidOperationException("Already discovered this fragment");

            if (!fragment.RequiredFragments.All(r => known.Contains(r)))
                throw new InvalidOperationException($"Required fragments: {string.Join(", ", fragment.RequiredFragments)}");

            known.Add(fragmentId);

            if (fragment.UnlocksCommand is not null)
            {
                var discoveredCmds = _playerDiscoveredCommands.GetValueOrDefault(playerId);
                if (discoveredCmds is null)
                {
                    discoveredCmds = new HashSet<string>();
                    _playerDiscoveredCommands[playerId] = discoveredCmds;
                }
                discoveredCmds.Add(fragment.UnlocksCommand);

                var player = _playerService.GetPlayer(playerId);
                if (player is not null)
                {
                    player.UnlockedCommands.Add(fragment.UnlocksCommand);
                    _playerService.UpdatePlayer(player);
                }
            }

            _gameEventBus?.Publish(new GameEvent { PlayerId = playerId, Type = AchievementEvent.FRAGMENT_GATHERED, Value = 1 });
            return fragment;
        }
        finally { _mutex.Release(); }
    }

    public async Task<List<KnowledgeFragment>> GetAvailableFragments(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var known = _playerKnowledge.GetValueOrDefault(playerId) ?? new HashSet<string>();
            return KnowledgeFragments
                .Where(f => !known.Contains(f.Id) && f.RequiredFragments.All(r => known.Contains(r)))
                .ToList();
        }
        finally { _mutex.Release(); }
    }

    public async Task<KnowledgeMapData> GetKnowledgeMap(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var known = _playerKnowledge.GetValueOrDefault(playerId) ?? new HashSet<string>();
            var discoveredCmds = _playerDiscoveredCommands.GetValueOrDefault(playerId) ?? new HashSet<string>();

            return new KnowledgeMapData(
                KnowledgeFragments.Count,
                known.Count,
                discoveredCmds.Count,
                CommandRegistrySize,
                KnowledgeFragments.Select(f => new FragmentStatus(
                    f.Id, f.Name, f.Description,
                    f.Category.ToString(), f.Rarity,
                    Unlocked: known.Contains(f.Id),
                    Available: !known.Contains(f.Id) && f.RequiredFragments.All(r => known.Contains(r)),
                    f.RequiredFragments,
                    f.UnlocksCommand,
                    known.Contains(f.Id) ? "" : f.SourceHint
                )).ToList()
            );
        }
        finally { _mutex.Release(); }
    }

    private static int CommandRegistrySize { get; } = 100;

    private static readonly List<KnowledgeFragment> KnowledgeFragments = new()
    {
        new KnowledgeFragment { Id = "recon_basics", Name = "Recon Basics", Description = "Foundational reconnaissance techniques", Category = KnowledgeCategory.RECONNAISSANCE, RequiredFragments = new(), UnlocksCommand = "ping", Rarity = "common", SourceHint = "Run 'ping' or 'scan' to discover this" },
        new KnowledgeFragment { Id = "recon_advanced", Name = "Advanced Recon", Description = "Deep network probing and enumeration", Category = KnowledgeCategory.RECONNAISSANCE, RequiredFragments = new() { "recon_basics" }, UnlocksCommand = "nmap", Rarity = "uncommon", SourceHint = "Run 'nmap' on a target" },
        new KnowledgeFragment { Id = "exploit_basics", Name = "Exploit Fundamentals", Description = "Basic exploitation techniques", Category = KnowledgeCategory.EXPLOITATION, RequiredFragments = new() { "recon_advanced" }, UnlocksCommand = "exploit", Rarity = "rare", SourceHint = "Run 'exploit' on a target" },
        new KnowledgeFragment { Id = "crypto_basics", Name = "Crypto Foundations", Description = "Understanding encryption and hashing", Category = KnowledgeCategory.CRYPTOGRAPHY, RequiredFragments = new(), UnlocksCommand = "decrypt", Rarity = "common", SourceHint = "Run 'decrypt' on a file" },
        new KnowledgeFragment { Id = "crypto_advanced", Name = "Crypto Mastery", Description = "Breaking strong encryption", Category = KnowledgeCategory.CRYPTOGRAPHY, RequiredFragments = new() { "crypto_basics" }, UnlocksCommand = "crack", Rarity = "epic", SourceHint = "Run 'crack' on a hash" },
        new KnowledgeFragment { Id = "defense_basics", Name = "Defense Foundations", Description = "Hardening systems against attack", Category = KnowledgeCategory.DEFENSE, RequiredFragments = new(), UnlocksCommand = "firewall", Rarity = "uncommon", SourceHint = "Run 'firewall' command" },
        new KnowledgeFragment { Id = "stealth_basics", Name = "Stealth Tactics", Description = "Avoiding detection", Category = KnowledgeCategory.STEALTH, RequiredFragments = new(), UnlocksCommand = "spoof", Rarity = "uncommon", SourceHint = "Run 'spoof' command" },
        new KnowledgeFragment { Id = "stealth_advanced", Name = "Ghost Protocol", Description = "Become invisible on the wire", Category = KnowledgeCategory.STEALTH, RequiredFragments = new() { "stealth_basics" }, UnlocksCommand = "proxy", Rarity = "rare", SourceHint = "Chain 'proxy' for anonymity" },
        new KnowledgeFragment { Id = "network_basics", Name = "Network Fundamentals", Description = "Understanding the network layer", Category = KnowledgeCategory.NETWORKING, RequiredFragments = new(), UnlocksCommand = "ifconfig", Rarity = "common", SourceHint = "Run 'ifconfig'" },
        new KnowledgeFragment { Id = "persistence", Name = "Persistence Techniques", Description = "Maintaining access to a system", Category = KnowledgeCategory.EXPLOITATION, RequiredFragments = new() { "exploit_basics" }, UnlocksCommand = "backdoor", Rarity = "epic", SourceHint = "Run 'backdoor' on a target" },
        new KnowledgeFragment { Id = "propagation", Name = "Worm Propagation", Description = "Self-replicating payloads", Category = KnowledgeCategory.EXPLOITATION, RequiredFragments = new() { "persistence" }, UnlocksCommand = "worm", Rarity = "legendary", SourceHint = "Run 'worm' with --spread" },
        new KnowledgeFragment { Id = "ai_basics", Name = "AI Assistance", Description = "Leveraging AI for hacking", Category = KnowledgeCategory.GENERAL, RequiredFragments = new() { "crypto_advanced", "stealth_advanced" }, UnlocksCommand = "ai-assist", Rarity = "legendary", SourceHint = "Run 'ai-assist'" }
    };
}
