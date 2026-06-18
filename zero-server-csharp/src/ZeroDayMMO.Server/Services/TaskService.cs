using ZeroDayMMO.Shared.Models;
using TaskStatus = ZeroDayMMO.Shared.Models.TaskStatus;

namespace ZeroDayMMO.Server.Services;

public enum TaskDifficulty
{
    Trivial,
    Easy,
    Medium,
    Hard,
    Expert,
    Legendary
}

public static class TaskDifficultyExtensions
{
    public static double Multiplier(this TaskDifficulty d) => d switch
    {
        TaskDifficulty.Trivial => 0.5,
        TaskDifficulty.Easy => 1.0,
        TaskDifficulty.Medium => 1.5,
        TaskDifficulty.Hard => 2.5,
        TaskDifficulty.Expert => 4.0,
        TaskDifficulty.Legendary => 8.0,
        _ => 1.0
    };
}

public enum TaskTargetType
{
    PenetrationTest,
    DataTheft,
    ServiceDisruption,
    DefenseSetup,
    Cryptography,
    SocialEngineering,
    BotnetOperation,
    NetworkRecon,
    Forensics,
    BugBounty
}

public record TaskRewards(
    long Experience = 0,
    long Credits = 0,
    int Reputation = 0,
    int CpuUpgrade = 0,
    int RamUpgrade = 0,
    int BandwidthUpgrade = 0,
    string? CommandUnlock = null
);

public record GameTask(
    string Id,
    string Title,
    string Description,
    TaskDifficulty Difficulty,
    int RequiredLevel,
    List<string> RequiredCommands,
    TaskTargetType TargetType,
    string? TargetIp,
    string Objective,
    long TimeLimitMs,
    TaskRewards Rewards,
    bool IsSoloable = true,
    int MaxPartySize = 4,
    List<string>? Tags = null
);

public class TaskInstance
{
    public string TaskId { get; }
    public string InstanceId { get; }
    public string Title { get; }
    public string Description { get; }
    public TaskDifficulty Difficulty { get; }
    public TaskTargetType TargetType { get; }
    public string? TargetIp { get; }
    public string Objective { get; }
    public long TimeLimitMs { get; }
    public TaskRewards Rewards { get; }
    public long CreatedAt { get; }
    public List<string> ClaimedBy { get; set; } = new();
    public TaskStatus Status { get; set; } = TaskStatus.AVAILABLE;
    public string? CompletedBy { get; set; }

    public TaskInstance(
        string taskId,
        string instanceId,
        string title,
        string description,
        TaskDifficulty difficulty,
        TaskTargetType targetType,
        string? targetIp,
        string objective,
        long timeLimitMs,
        TaskRewards rewards,
        long? createdAt = null)
    {
        TaskId = taskId;
        InstanceId = instanceId;
        Title = title;
        Description = description;
        Difficulty = difficulty;
        TargetType = targetType;
        TargetIp = targetIp;
        Objective = objective;
        TimeLimitMs = timeLimitMs;
        Rewards = rewards;
        CreatedAt = createdAt ?? DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    }

    public ZeroDayMMO.Shared.Models.TaskInstance ToShared() => new()
    {
        TaskId = TaskId,
        InstanceId = InstanceId,
        Title = Title,
        Description = Description,
        Difficulty = (ZeroDayMMO.Shared.Models.TaskDifficulty)(int)Difficulty,
        TargetType = (ZeroDayMMO.Shared.Models.TaskTargetType)(int)TargetType,
        TargetIp = TargetIp,
        Objective = Objective,
        TimeLimitMs = TimeLimitMs,
        Rewards = new ZeroDayMMO.Shared.Models.TaskRewards
        {
            Experience = Rewards.Experience,
            Credits = Rewards.Credits,
            Reputation = Rewards.Reputation,
            CpuUpgrade = Rewards.CpuUpgrade,
            RamUpgrade = Rewards.RamUpgrade,
            BandwidthUpgrade = Rewards.BandwidthUpgrade,
            CommandUnlock = Rewards.CommandUnlock
        },
        Status = (ZeroDayMMO.Shared.Models.TaskStatus)(int)Status,
        CreatedAt = CreatedAt,
        ClaimedBy = new List<string>(ClaimedBy),
        CompletedBy = CompletedBy
    };
}

public static class TaskTemplates
{
    public static List<GameTask> TaskPool { get; } = new()
    {
        new("t1", "Scan Corporate Network", "Perform a full reconnaissance scan of the target corporate network and identify all active hosts.",
            TaskDifficulty.Easy, 2, new() { "nmap" }, TaskTargetType.NetworkRecon, "10.0.1.0/24",
            "Identify all hosts on the subnet", 300_000, new(80, 200, 10, 5, 10, 5)),
        new("t2", "Extract Customer Database", "Breach the target server and exfiltrate the customer database.",
            TaskDifficulty.Medium, 5, new() { "exploit", "ssh" }, TaskTargetType.DataTheft, "10.0.2.50",
            "Locate and cat the database file", 600_000, new(300, 800, 25, 15, 25, 10)),
        new("t3", "Deploy Ransomware Simulation", "Simulate a ransomware attack by encrypting files on target and leaving a note.",
            TaskDifficulty.Hard, 8, new() { "exploit", "backdoor", "encrypt" }, TaskTargetType.ServiceDisruption, "10.0.3.100",
            "Encrypt 3 critical files", 900_000, new(800, 2000, 50, 25, 40, 20)),
        new("t4", "Botnet Recruitment Drive", "Infect 20 systems across the darknet with a backdoor and add them to your botnet.",
            TaskDifficulty.Hard, 12, new() { "backdoor", "botnet" }, TaskTargetType.BotnetOperation, null,
            "Build a botnet of 20+ nodes", 1_200_000, new(1500, 4000, 100, 40, 60, 30)),
        new("t5", "Crack Government Cipher", "Decrypt intercepted government communications. Multiple layers of encryption.",
            TaskDifficulty.Expert, 14, new() { "decrypt", "crack" }, TaskTargetType.Cryptography, null,
            "Decrypt all 5 message layers", 1_500_000, new(3000, 8000, 200, 50, 80, 40)),
        new("t6", "Zero-Day Auction Hijack", "Intercept a zero-day exploit auction on the darknet and steal the payload.",
            TaskDifficulty.Legendary, 18, new() { "sniff", "spoof", "zero-day" }, TaskTargetType.DataTheft, "203.0.113.50",
            "Steal the zero-day payload before the auction ends", 1_800_000,
            new(8000, 25000, 500, 100, 150, 60, "zero-day")),
        new("t7", "Defend Against APT", "A sophisticated attacker is targeting your infrastructure. Set up defenses and trace them.",
            TaskDifficulty.Medium, 9, new() { "firewall", "trace", "honeypot" }, TaskTargetType.DefenseSetup, null,
            "Block 50+ attack attempts and trace the source", 600_000, new(500, 1500, 40, 20, 30, 15)),
        new("t8", "Darknet Forum Heist", "Extract user credentials from a darknet forum database.",
            TaskDifficulty.Hard, 10, new() { "sqlmap", "exploit" }, TaskTargetType.DataTheft, "172.16.0.50",
            "Dump the forum user table", 900_000, new(1200, 3500, 80, 30, 50, 25)),
        new("t9", "Fireside Chat Intercept", "Intercept VoIP communications between two corporate executives.",
            TaskDifficulty.Expert, 15, new() { "sniff", "decrypt", "spoof" }, TaskTargetType.NetworkRecon, "10.0.5.100",
            "Capture and decrypt 5 VoIP packets", 1_200_000, new(4000, 10000, 250, 60, 100, 40)),
        new("t10", "Worm Containment", "A worm is spreading through the network. Create a vaccine and contain it.",
            TaskDifficulty.Expert, 16, new() { "firewall", "trace", "worm" }, TaskTargetType.Forensics, null,
            "Contain the worm and reverse-engineer its signature", 1_500_000, new(6000, 15000, 300, 80, 120, 50)),
        new("t11", "Simple Data Grab", "Grab a file from an unsecured server. Practice target.",
            TaskDifficulty.Trivial, 1, new(), TaskTargetType.DataTheft, "10.0.0.5",
            "Connect and cat flag.txt", 180_000, new(30, 50, 5, 2, 5, 2)),
        new("t12", "Brute Force Challenge", "Brute force the admin panel of a local business server.",
            TaskDifficulty.Easy, 5, new() { "bruteforce" }, TaskTargetType.PenetrationTest, "10.0.1.200",
            "Find admin credentials via brute force", 300_000, new(150, 400, 15, 10, 15, 5)),
        new("t13", "Phishing Campaign", "Set up a phishing page and harvest credentials from corporate employees.",
            TaskDifficulty.Medium, 7, new() { "proxy", "spoof" }, TaskTargetType.SocialEngineering, null,
            "Collect 10 sets of credentials", 600_000, new(400, 1000, 30, 15, 25, 10)),
        new("t14", "DDoS Coordination", "Coordinate a distributed denial of service attack using your botnet.",
            TaskDifficulty.Hard, 13, new() { "botnet", "overload" }, TaskTargetType.ServiceDisruption, "198.51.100.10",
            "Take the target offline for 60 seconds", 1_000_000, new(2000, 5000, 120, 35, 50, 25)),
        new("t15", "Bug Bounty: E-commerce", "Find and exploit 3 vulnerabilities in an e-commerce platform.",
            TaskDifficulty.Medium, 8, new() { "sqlmap", "exploit" }, TaskTargetType.BugBounty, "10.0.3.200",
            "Find SQLi, XSS, and LFI vulnerabilities", 900_000, new(600, 2000, 60, 20, 30, 15))
    };
}


public class TaskService
{
    private readonly IPlayerService _playerService;
    private readonly ContractGenerator? _contractGenerator;
    private readonly IGameEventBus? _gameEventBus;
    private readonly List<TaskInstance> _availableTasks = new();
    private readonly SemaphoreSlim _mutex = new(1, 1);
    private CancellationTokenSource? _cts;

    private const long TaskGenIntervalMs = 30_000;

    public TaskService(
        IPlayerService playerService,
        ContractGenerator? contractGenerator = null,
        IGameEventBus? gameEventBus = null)
    {
        _playerService = playerService;
        _contractGenerator = contractGenerator;
        _gameEventBus = gameEventBus;
    }

    public void Start()
    {
        _cts = new CancellationTokenSource();
        var token = _cts.Token;
        SeedInitialTasks();

        _ = Task.Run(async () =>
        {
            while (!token.IsCancellationRequested)
            {
                await GenerateTasks();
                await Task.Delay((int)TaskGenIntervalMs, token);
            }
        }, token);

        _ = Task.Run(async () =>
        {
            while (!token.IsCancellationRequested)
            {
                CheckExpiredTasks();
                await Task.Delay(10_000, token);
            }
        }, token);
    }

    public void Stop()
    {
        _cts?.Cancel();
    }

    private void SeedInitialTasks()
    {
        var initial = TaskTemplates.TaskPool
            .Where(t => t.Difficulty is TaskDifficulty.Trivial or TaskDifficulty.Easy)
            .ToList();
        foreach (var template in initial)
        {
            _availableTasks.Add(ToInstance(template));
        }
    }

    private async Task GenerateTasks()
    {
        await _mutex.WaitAsync();
        try
        {
            var totalAvailable = _availableTasks.Count(t => t.Status == TaskStatus.AVAILABLE);
            if (totalAvailable >= 25) return;

            var onlinePlayers = _playerService.GetOnlinePlayers();
            var avgLevel = onlinePlayers.Count > 0
                ? (int)onlinePlayers.Average(p => p.Level)
                : 1;

            var newTaskTemplates = TaskTemplates.TaskPool
                .Where(template =>
                {
                    var (minL, maxL) = template.Difficulty switch
                    {
                        TaskDifficulty.Trivial => (1, 3),
                        TaskDifficulty.Easy => (1, 5),
                        TaskDifficulty.Medium => (3, 8),
                        TaskDifficulty.Hard => (6, 12),
                        TaskDifficulty.Expert => (10, 16),
                        TaskDifficulty.Legendary => (15, 20),
                        _ => (1, 20)
                    };
                    return avgLevel >= minL && avgLevel <= maxL &&
                           _availableTasks.All(existing => existing.TaskId != template.Id);
                })
                .OrderBy(_ => Random.Shared.Next())
                .Take(2)
                .ToList();

            foreach (var template in newTaskTemplates)
            {
                _availableTasks.Add(ToInstance(template));
            }

            if (_contractGenerator != null && _availableTasks.Count < 20)
            {
                for (int i = 0; i < 2; i++)
                {
                    if (onlinePlayers.Count <= 0) break;
                    var samplePlayer = onlinePlayers[Random.Shared.Next(onlinePlayers.Count)];
                    var contract = await _contractGenerator.GenerateContract(samplePlayer.Id);
                    if (contract == null) continue;

                    var contractTaskId = "contract_" + contract.Id;
                    if (_availableTasks.Any(t => t.TaskId == contractTaskId)) continue;

                    var instance = new TaskInstance(
                        taskId: contractTaskId,
                        instanceId: "task_" + (contract.Id.Length > 8 ? contract.Id[..8] : contract.Id),
                        title: contract.Title,
                        description: contract.Description,
                        difficulty: contract.Difficulty,
                        targetType: contract.TargetType,
                        targetIp: contract.TargetIp,
                        objective: "Complete contract: " + contract.Title,
                        timeLimitMs: contract.TimeLimitMs,
                        rewards: new TaskRewards(
                            Experience: contract.FinalXp,
                            Credits: contract.FinalCredits,
                            Reputation: Math.Min((int)(contract.FinalCredits / 100), 50)
                        )
                    );
                    _availableTasks.Add(instance);
                }
            }
        }
        finally
        {
            _mutex.Release();
        }
    }

    private void CheckExpiredTasks()
    {
        _mutex.Wait();
        try
        {
            var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            _availableTasks.RemoveAll(t => t.CreatedAt + t.TimeLimitMs < now);
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<List<TaskInstance>> GetAvailableTasks(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player == null) return new();

            return _availableTasks
                .Where(task => task.Status == TaskStatus.AVAILABLE)
                .Where(task =>
                {
                    if (task.TaskId.StartsWith("contract_")) return true;
                    var template = TaskTemplates.TaskPool.Find(t => t.Id == task.TaskId);
                    return template != null &&
                           player.Level >= template.RequiredLevel &&
                           template.RequiredCommands.All(cmd => player.UnlockedCommands.Contains(cmd));
                })
                .ToList();
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<ServiceResult<TaskInstance>> AcceptTask(string playerId, string taskInstanceId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player == null)
                return ServiceResult<TaskInstance>.Failure("Player not found");

            var task = _availableTasks.Find(t => t.InstanceId == taskInstanceId);
            if (task == null)
                return ServiceResult<TaskInstance>.Failure("Task not found or already claimed");

            if (task.Status != TaskStatus.AVAILABLE)
                return ServiceResult<TaskInstance>.Failure("Task is no longer available");

            var template = TaskTemplates.TaskPool.Find(t => t.Id == task.TaskId);
            if (template == null)
                return ServiceResult<TaskInstance>.Failure("Task template not found");

            if (player.Level < template.RequiredLevel)
                return ServiceResult<TaskInstance>.Failure($"Level {template.RequiredLevel} required");

            var missingCmds = template.RequiredCommands.Where(c => !player.UnlockedCommands.Contains(c)).ToList();
            if (missingCmds.Count > 0)
                return ServiceResult<TaskInstance>.Failure($"Required commands: {string.Join(", ", missingCmds)}");

            task.ClaimedBy = new() { playerId };
            task.Status = TaskStatus.IN_PROGRESS;
            await _playerService.AssignTask(playerId, task.ToShared());
            return ServiceResult<TaskInstance>.Success(task);
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<ServiceResult<TaskInstance>> AcceptTaskParty(List<string> playerIds, string taskInstanceId)
    {
        await _mutex.WaitAsync();
        try
        {
            if (playerIds.Count < 2)
                return ServiceResult<TaskInstance>.Failure("Party requires at least 2 players");

            var task = _availableTasks.Find(t => t.InstanceId == taskInstanceId);
            if (task == null)
                return ServiceResult<TaskInstance>.Failure("Task not found");

            var template = TaskTemplates.TaskPool.Find(t => t.Id == task.TaskId);
            if (template == null)
                return ServiceResult<TaskInstance>.Failure("Task template not found");

            if (playerIds.Count > template.MaxPartySize)
                return ServiceResult<TaskInstance>.Failure($"Maximum party size is {template.MaxPartySize}");

            foreach (var pid in playerIds)
            {
                var p = _playerService.GetPlayer(pid);
                if (p == null)
                    return ServiceResult<TaskInstance>.Failure($"Player {pid} not found");
                if (p.Level < template.RequiredLevel)
                    return ServiceResult<TaskInstance>.Failure($"Player {p.Username} is below level {template.RequiredLevel}");
            }

            task.ClaimedBy = new(playerIds);
            task.Status = TaskStatus.IN_PROGRESS;
            foreach (var pid in playerIds)
            {
                await _playerService.AssignTask(pid, task.ToShared());
                await _playerService.SetPartyAsync(pid, taskInstanceId);
            }
            return ServiceResult<TaskInstance>.Success(task);
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<ServiceResult<TaskInstance>> CompleteTask(string playerId, string taskInstanceId)
    {
        await _mutex.WaitAsync();
        try
        {
            var task = _availableTasks.Find(t => t.InstanceId == taskInstanceId);
            if (task == null)
                return ServiceResult<TaskInstance>.Failure("Task not found");

            var template = TaskTemplates.TaskPool.Find(t => t.Id == task.TaskId);
            if (template == null)
                return ServiceResult<TaskInstance>.Failure("Task template not found");

            var isPartyLeader = task.ClaimedBy.FirstOrDefault() == playerId;
            if (!isPartyLeader && task.ClaimedBy.Count > 1)
                return ServiceResult<TaskInstance>.Failure("Only the party leader can submit completion");

            var isSolo = task.ClaimedBy.Count <= 1;
            var xpMultiplier = isSolo ? 1.0 : 1.2; // PARTY_EXP_BONUS

            foreach (var memberId in task.ClaimedBy)
            {
                var member = _playerService.GetPlayer(memberId);
                if (member == null) continue;

                var levelDiff = member.Level - template.RequiredLevel;
                var levelMultiplier = levelDiff <= 0 ? 1.0 : Math.Max(0.5, 1.0 - levelDiff * 0.1);

                var finalXp = (long)(template.Rewards.Experience * xpMultiplier * levelMultiplier);
                var finalCredits = template.Rewards.Credits;

                await _playerService.AddExperienceAsync(memberId, finalXp);
                await _playerService.AddCreditsAsync(memberId, finalCredits);
                await _playerService.AddReputationAsync(memberId, template.Rewards.Reputation);
                await _playerService.UpgradeResourcesAsync(memberId,
                    template.Rewards.CpuUpgrade, template.Rewards.RamUpgrade, template.Rewards.BandwidthUpgrade);

                if (template.Rewards.CommandUnlock != null)
                {
                    await _playerService.UnlockCommandAsync(memberId, template.Rewards.CommandUnlock);
                }

                await _playerService.CompleteTaskAsync(memberId, taskInstanceId);
                await _playerService.SetPartyAsync(memberId, null);

                if (_gameEventBus != null)
                {
                    await _gameEventBus.Emit(new GameEvent { PlayerId = memberId, Type = AchievementEvent.TASK_COMPLETED, Value = 1 });
                    await _gameEventBus.Emit(new GameEvent { PlayerId = memberId, Type = AchievementEvent.CREDITS_EARNED, Value = finalCredits });
                }
            }

            task.Status = TaskStatus.COMPLETED;
            task.CompletedBy = playerId;
            _availableTasks.RemoveAll(t => t.InstanceId == taskInstanceId);
            return ServiceResult<TaskInstance>.Success(task);
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<ServiceResult<TaskInstance>> FailTask(string playerId, string taskInstanceId)
    {
        await _mutex.WaitAsync();
        try
        {
            var task = _availableTasks.Find(t => t.InstanceId == taskInstanceId);
            if (task == null)
                return ServiceResult<TaskInstance>.Failure("Task not found");

            task.Status = TaskStatus.FAILED;
            foreach (var memberId in task.ClaimedBy)
            {
                await _playerService.CompleteTaskAsync(memberId, taskInstanceId);
                await _playerService.SetPartyAsync(memberId, null);
            }
            _availableTasks.RemoveAll(t => t.InstanceId == taskInstanceId);
            return ServiceResult<TaskInstance>.Success(task);
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<TaskInstance?> GenerateDynamicTask(string playerId)
    {
        var player = _playerService.GetPlayer(playerId);
        if (player == null) return null;

        var maxDifficulty = player.Level switch
        {
            < 3 => TaskDifficulty.Trivial,
            < 5 => TaskDifficulty.Easy,
            < 8 => TaskDifficulty.Medium,
            < 12 => TaskDifficulty.Hard,
            < 16 => TaskDifficulty.Expert,
            _ => TaskDifficulty.Legendary
        };

        var suitable = TaskTemplates.TaskPool
            .Where(t => (int)t.Difficulty <= (int)maxDifficulty &&
                        t.RequiredLevel <= player.Level &&
                        t.RequiredCommands.All(cmd => player.UnlockedCommands.Contains(cmd)))
            .ToList();

        if (suitable.Count == 0) return null;

        var template = suitable[Random.Shared.Next(suitable.Count)];
        var instanceId = "dyn_" + Guid.NewGuid().ToString()[..8];

        var diff = player.Level switch
        {
            >= 18 when Random.Shared.NextDouble() < 0.1 => TaskDifficulty.Legendary,
            >= 12 when Random.Shared.NextDouble() < 0.2 => TaskDifficulty.Expert,
            >= 8 when Random.Shared.NextDouble() < 0.3 => TaskDifficulty.Hard,
            >= 4 when Random.Shared.NextDouble() < 0.3 => TaskDifficulty.Medium,
            _ => TaskDifficulty.Easy
        };

        return new TaskInstance(
            taskId: template.Id,
            instanceId: instanceId,
            title: (Random.Shared.NextDouble() < 0.3 ? "[PARTY] " : "") + template.Title,
            description: template.Description,
            difficulty: diff,
            targetType: template.TargetType,
            targetIp: template.TargetIp,
            objective: template.Objective,
            timeLimitMs: (long)(template.TimeLimitMs * (1.0 + Random.Shared.NextDouble() * 0.5)),
            rewards: template.Rewards
        );
    }

    private static TaskInstance ToInstance(GameTask t)
    {
        return new TaskInstance(
            taskId: t.Id,
            instanceId: "task_" + Guid.NewGuid().ToString()[..8],
            title: t.Title,
            description: t.Description,
            difficulty: t.Difficulty,
            targetType: t.TargetType,
            targetIp: t.TargetIp,
            objective: t.Objective,
            timeLimitMs: t.TimeLimitMs,
            rewards: t.Rewards
        );
    }
}

public readonly struct ServiceResult<T>
{
    public T? Value { get; }
    public string? Error { get; }
    public bool IsSuccess => Error is null;

    private ServiceResult(T value) { Value = value; Error = null; }
    private ServiceResult(string error) { Value = default; Error = error; }

    public static ServiceResult<T> Success(T value) => new(value);
    public static ServiceResult<T> Failure(string error) => new(error);
}
