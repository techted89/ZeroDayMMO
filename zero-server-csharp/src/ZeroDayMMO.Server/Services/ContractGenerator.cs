namespace ZeroDayMMO.Server.Services;

public enum ModifierType
{
    TimeLimit,
    StealthRequired,
    NoDetection,
    SpeedRun,
    GhostMode,
    LimitedResources,
    BoostedRewards,
    ReducedRewards,
    IncreasedDifficulty,
    DecreasedDifficulty,
    SpecialObjective,
    TeamRequired,
    SoloOnly
}

public enum ModifierRarity
{
    Common,
    Uncommon,
    Rare,
    Epic,
    Legendary
}

public record MissionModifier(
    string Id,
    string Name,
    string Description,
    ModifierType ModifierType,
    double Value,
    ModifierRarity Rarity,
    long DurationMs = 0,
    bool Stacking = false
);

public record EnhancedContract(
    string Id,
    string Title,
    string Description,
    TaskTargetType TargetType,
    string TargetIp,
    List<string> RequiredCommands,
    TaskDifficulty Difficulty,
    int SuggestedLevel,
    long BaseXp,
    long BaseCredits,
    List<MissionModifier> Modifiers,
    double FinalRewardMultiplier,
    long FinalXp,
    long FinalCredits,
    long CreatedAt,
    long TimeLimitMs
);

public static class MissionModifiers
{
    public static readonly MissionModifier TimeLimited = new(
        "time_limited", "Time Limited", "Complete within reduced time",
        ModifierType.TimeLimit, 0.7, ModifierRarity.Uncommon);

    public static readonly MissionModifier StealthRequired = new(
        "stealth_required", "Stealth Required", "Avoid detection systems",
        ModifierType.StealthRequired, 1.0, ModifierRarity.Rare);

    public static readonly MissionModifier NoDetection = new(
        "no_detection", "Ghost Mode", "Leave no traces behind",
        ModifierType.NoDetection, 1.0, ModifierRarity.Epic);

    public static readonly MissionModifier SpeedRun = new(
        "speed_run", "Speed Run", "2x rewards for completing under half time",
        ModifierType.SpeedRun, 2.0, ModifierRarity.Rare);

    public static readonly MissionModifier LimitedResources = new(
        "limited_resources", "Limited Resources", "Reduced starting resources",
        ModifierType.LimitedResources, 0.5, ModifierRarity.Uncommon);

    public static readonly MissionModifier BoostedRewards = new(
        "boosted_rewards", "Boosted Rewards", "Increased payout for risk",
        ModifierType.BoostedRewards, 1.5, ModifierRarity.Uncommon);

    public static readonly MissionModifier ReducedRewards = new(
        "reduced_rewards", "Reduced Rewards", "Lower payout for ease",
        ModifierType.ReducedRewards, 0.7, ModifierRarity.Common);

    public static readonly MissionModifier IncreasedDifficulty = new(
        "increased_difficulty", "Increased Difficulty", "Enhanced security measures",
        ModifierType.IncreasedDifficulty, 1.0, ModifierRarity.Rare);

    public static readonly MissionModifier DecreasedDifficulty = new(
        "decreased_difficulty", "Decreased Difficulty", "Weakened defenses",
        ModifierType.DecreasedDifficulty, 1.0, ModifierRarity.Uncommon);

    public static readonly MissionModifier SpecialObjective = new(
        "special_objective", "Special Objective", "Additional objective required",
        ModifierType.SpecialObjective, 1.0, ModifierRarity.Rare);

    public static readonly MissionModifier TeamRequired = new(
        "team_required", "Team Required", "Must complete with 2+ players",
        ModifierType.TeamRequired, 1.0, ModifierRarity.Rare);

    public static readonly MissionModifier SoloOnly = new(
        "solo_only", "Solo Only", "Must be completed alone",
        ModifierType.SoloOnly, 1.0, ModifierRarity.Uncommon);

    public static List<MissionModifier> AllModifiers { get; } = new()
    {
        TimeLimited, StealthRequired, NoDetection, SpeedRun,
        LimitedResources, BoostedRewards, ReducedRewards,
        IncreasedDifficulty, DecreasedDifficulty, SpecialObjective,
        TeamRequired, SoloOnly
    };

    public static List<MissionModifier> GetRandomModifier(int count = 1)
    {
        var takeCount = Math.Min(count, AllModifiers.Count);
        return Enumerable.Range(0, takeCount)
            .Select(_ => AllModifiers[Random.Shared.Next(AllModifiers.Count)])
            .Distinct()
            .ToList();
    }
}

public class ContractGenerator
{
    public record ContractTemplate(
        string Id,
        string TitleTemplate,
        string DescriptionTemplate,
        TaskTargetType TargetType,
        List<string> SuggestedCommands
    );

    private readonly IPlayerService _playerService;
    private readonly SemaphoreSlim _mutex = new(1, 1);

    private readonly List<ContractTemplate> _contractTemplates = new()
    {
        new("data_breach", "Data Breach: {company}",
            "Infiltrate {target} and exfiltrate the {data_type} database. {difficulty_note}",
            TaskTargetType.DataTheft, new() { "exploit", "ssh", "cat" }),
        new("pen_test", "Penetration Test: {company}",
            "Perform a full security audit of {target}. Identify and exploit vulnerabilities. {difficulty_note}",
            TaskTargetType.PenetrationTest, new() { "nmap", "exploit", "backdoor" }),
        new("ddos_job", "Service Disruption: {company}",
            "Take {target} offline for {duration} seconds. Coordinate with available resources. {difficulty_note}",
            TaskTargetType.ServiceDisruption, new() { "overload", "botnet" }),
        new("recon", "Reconnaissance: {target_net}",
            "Map out the {target_net} subnet. Identify all hosts, open ports, and running services. {difficulty_note}",
            TaskTargetType.NetworkRecon, new() { "nmap", "ping", "traceroute" }),
        new("crypto_job", "Cryptographic Challenge: {company}",
            "Intercept and decrypt {data_type} communications from {target}. Multiple encryption layers. {difficulty_note}",
            TaskTargetType.Cryptography, new() { "decrypt", "sniff", "crack" }),
        new("cleanup", "Log Cleanup: {company}",
            "Erase all traces of {prev_attacker} intrusion from {target}. Remove logs and backdoors. {difficulty_note}",
            TaskTargetType.Forensics, new() { "firewall", "trace", "worm" }),
        new("botnet_recruit", "Botnet Recruitment Drive",
            "Infect {node_count} systems in {target_net} with backdoor payload. Build your botnet. {difficulty_note}",
            TaskTargetType.BotnetOperation, new() { "backdoor", "botnet", "worm" }),
        new("social_engineer", "Social Engineering: {company}",
            "Set up a phishing campaign targeting {company} employees. Harvest {credential_count} credentials. {difficulty_note}",
            TaskTargetType.SocialEngineering, new() { "proxy", "spoof", "encrypt" }),
        new("bug_bounty", "Bug Bounty: {company}",
            "Find and document {vuln_count} vulnerabilities in {target}. Submit responsible disclosure. {difficulty_note}",
            TaskTargetType.BugBounty, new() { "sqlmap", "exploit", "nmap" })
    };

    private readonly List<string> _companyNames = new()
    {
        "NexusCorp", "ApexGlobal", "ByteDynamics", "QuantumLeap", "FusionTech",
        "OmniData", "StratoSys", "PulseNet", "VertexLogic", "CipherStack",
        "MatrixHoldings", "EchoSystems", "DeltaForce Tech", "Sigma Solutions",
        "Omega Labs", "Phoenix Cyber", "Titan Industries", "Aurora Networks"
    };

    private readonly List<string> _dataTypes = new()
    {
        "customer", "employee", "financial", "medical", "research", "classified"
    };

    private readonly Dictionary<TaskDifficulty, string> _difficultyNotes = new()
    {
        [TaskDifficulty.Trivial] = "A cakewalk. Even a script kiddie could do this.",
        [TaskDifficulty.Easy] = "Entry-level contract. Basic security measures.",
        [TaskDifficulty.Medium] = "Standard corporate security. Proceed with caution.",
        [TaskDifficulty.Hard] = "Enterprise-grade defenses. Bring your A-game.",
        [TaskDifficulty.Expert] = "Military-grade encryption and monitoring. Extreme caution advised.",
        [TaskDifficulty.Legendary] = "Three-letter-agency level. You didn't see this. Good luck."
    };

    private readonly List<string> _prevAttackers = new()
    {
        "Anonymous", "GhostNet", "DarkOverlord", "LulzSec"
    };

    public ContractGenerator(IPlayerService playerService)
    {
        _playerService = playerService;
    }

    public async Task<EnhancedContract?> GenerateContract(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player == null) return null;

            var template = _contractTemplates[Random.Shared.Next(_contractTemplates.Count)];
            var difficulty = RollDifficulty(player.Level);
            var company = _companyNames[Random.Shared.Next(_companyNames.Count)];
            var targetIp = GenerateTargetIp(player.Level);

            var dataType = _dataTypes[Random.Shared.Next(_dataTypes.Count)];
            var prevAttacker = _prevAttackers[Random.Shared.Next(_prevAttackers.Count)];

            var fillParams = new Dictionary<string, string>
            {
                ["company"] = company,
                ["target"] = targetIp,
                ["target_net"] = targetIp[..targetIp.LastIndexOf('.')] + ".0/24",
                ["data_type"] = dataType,
                ["duration"] = $"{(int)(30 * difficulty.Multiplier()) + 10}",
                ["node_count"] = $"{(int)(5 * difficulty.Multiplier()) + 2}",
                ["credential_count"] = $"{(int)(5 * difficulty.Multiplier())}",
                ["vuln_count"] = $"{Math.Max(2, (int)(2 * difficulty.Multiplier()))}",
                ["prev_attacker"] = prevAttacker,
                ["difficulty_note"] = _difficultyNotes.GetValueOrDefault(difficulty, "")
            };

            var modifierCount = Random.Shared.Next(4);
            var modifiers = modifierCount > 0
                ? MissionModifiers.GetRandomModifier(modifierCount)
                : new List<MissionModifier>();

            var finalDifficulty = difficulty;
            var rewardMultiplier = 1.0;

            foreach (var modifier in modifiers)
            {
                switch (modifier.ModifierType)
                {
                    case ModifierType.IncreasedDifficulty:
                        var currentIdx = (int)finalDifficulty;
                        if (currentIdx < Enum.GetValues<TaskDifficulty>().Length - 1)
                            finalDifficulty = (TaskDifficulty)(currentIdx + 1);
                        break;
                    case ModifierType.DecreasedDifficulty:
                        var curIdx = (int)finalDifficulty;
                        if (curIdx > 0)
                            finalDifficulty = (TaskDifficulty)(curIdx - 1);
                        break;
                    case ModifierType.BoostedRewards:
                        rewardMultiplier *= 1.5;
                        break;
                    case ModifierType.ReducedRewards:
                        rewardMultiplier *= 0.7;
                        break;
                    case ModifierType.SpeedRun:
                        rewardMultiplier *= 2.0;
                        break;
                }
            }

            var xpBase = (long)(50 * difficulty.Multiplier() * player.Level);
            var creditsBase = (long)(100 * difficulty.Multiplier() * player.Level);
            var finalXp = (long)(xpBase * rewardMultiplier);
            var finalCredits = (long)(creditsBase * rewardMultiplier);

            var title = FillTemplate(template.TitleTemplate, fillParams);
            var description = FillTemplate(template.DescriptionTemplate, fillParams);

            return new EnhancedContract(
                Id: Guid.NewGuid().ToString(),
                Title: title,
                Description: description,
                TargetType: template.TargetType,
                TargetIp: targetIp,
                RequiredCommands: new(template.SuggestedCommands),
                Difficulty: finalDifficulty,
                SuggestedLevel: player.Level,
                BaseXp: xpBase,
                BaseCredits: creditsBase,
                Modifiers: new(modifiers),
                FinalRewardMultiplier: rewardMultiplier,
                FinalXp: finalXp,
                FinalCredits: finalCredits,
                CreatedAt: DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                TimeLimitMs: 600_000
            );
        }
        finally
        {
            _mutex.Release();
        }
    }

    private TaskDifficulty RollDifficulty(int playerLevel)
    {
        var maxIdx = playerLevel switch
        {
            < 3 => 0,
            < 5 => 1,
            < 7 => 2,
            < 10 => 3,
            < 14 => 4,
            _ => 5
        };
        var roll = Random.Shared.Next(maxIdx + 2);
        var diffs = Enum.GetValues<TaskDifficulty>();
        return diffs[Math.Min(roll, diffs.Length - 1)];
    }

    private static string GenerateTargetIp(int playerLevel)
    {
        var baseIp = playerLevel switch
        {
            < 3 => "10.0.0",
            < 5 => "10.0.1",
            < 7 => "10.0.2",
            < 10 => "10.0.3",
            < 14 => "172.16.0",
            < 18 => "198.51.100",
            _ => "203.0.113"
        };
        return $"{baseIp}.{Random.Shared.Next(2, 255)}";
    }

    private static string FillTemplate(string template, Dictionary<string, string> parameters)
    {
        var result = template;
        foreach (var (key, value) in parameters)
        {
            result = result.Replace($"{{{key}}}", value);
        }
        return result;
    }
}
