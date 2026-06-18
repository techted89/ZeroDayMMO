# ZeroDayMMO Server — Kotlin-to-C# Porting Plan

## 1. Overview

This document maps every source file in the Kotlin `zero-server` codebase to its C# equivalent.
The target is **.NET 8.0** with **ASP.NET Core + SignalR** for WebSocket transport.

### Project Layout (C#)

```
zero-server-csharp/
├── ZeroDayMMO.Server.sln
├── src/
│   ├── ZeroDayMMO.Shared/       # Models, Protocol, Security, ZDScript (no ASP.NET dependency)
│   └── ZeroDayMMO.Server/       # Server entrypoint, Handlers, Services, Config
└── tests/
    └── ZeroDayMMO.Tests/        # xUnit tests
```

### NuGet Dependencies

| Kotlin | C# Equivalent | Project |
|--------|---------------|---------|
| Ktor 2.3.7 (Netty) | ASP.NET Core 8.0 (`Microsoft.AspNetCore.App`) | Server |
| Ktor WebSocket | SignalR (`Microsoft.AspNetCore.SignalR`) | Server |
| kotlinx-serialization-json 1.6.2 | `System.Text.Json` (built-in) | Shared |
| jBCrypt 0.4 | `BCrypt.Net-Next` | Shared |
| Logback 1.4.14 | `Microsoft.Extensions.Logging` + `Serilog.AspNetCore` | Server |
| JUnit 5 / kotlin.test | xUnit 2.4.2 + Moq 4.20.72 | Tests |
| Gradle 7.6 | .NET SDK 8.0 (csproj) | — |

---

## 2. Mapping: Kotlin Package → C# Namespace & File

### 2.1 Shared Library (`ZeroDayMMO.Shared`)

All files in this project have **zero ASP.NET dependencies** — they are pure C# POCOs, enums, and utilities.

#### `com.zeroday.protocol` → `ZeroDayMMO.Shared.Protocol`

| Kotlin File | C# File | Notes |
|-------------|---------|-------|
| `MessageProtocol.kt` | `MessageProtocol.cs` | Static class with `const string` fields for type/payload keys |
| (embedded in above) | `RequestTypes.cs` | Static class with `const string` fields for 40+ request types |
| (embedded in above) | `ResponseTypes.cs` | Static class with `const string` fields for response types |
| (embedded in above) | `ServerError.cs` | Static class with `const string` error codes |
| (embedded in above) | `ErrorCategory.cs` | Enum for error categories |

**Design Decision:** In Kotlin these are all in one file as `object` declarations. In C# split into separate static classes for maintainability. Use `public static class` with `public const string` fields.

#### `com.zeroday.model` → `ZeroDayMMO.Shared.Models`

| Kotlin File | C# File | Notes |
|-------------|---------|-------|
| `Player.kt` | `Player.cs` | Record/class with `[JsonPropertyName]` attributes. 60+ properties. |
| `Command.kt` | `GameCommand.cs`, `CommandCategory.cs`, `CommandRegistry.cs`, `CommandResult.cs`, `CommandStatus.cs` | Split enums/classes for clarity |
| `Task.kt` | `GameTask.cs`, `TaskInstance.cs`, `TaskDifficulty.cs`, `TaskTargetType.cs`, `TaskRewards.cs`, `TaskTemplates.cs` | Multiple files |
| `Achievement.kt` | `AchievementCategory.cs`, `AchievementDefinition.cs`, `AchievementProgress.cs`, `AchievementEvent.cs`, `AchievementRewards.cs`, `AchievementUpdate.cs` | One per type |
| `Event.kt` | `StoryEvent.cs`, `EventStage.cs`, `EventRewards.cs`, `StorylineRegistry.cs` | Split by concern |
| `Skill.kt` | `SkillDefinition.cs`, `SkillTree.cs`, `SkillEffect.cs` (discriminated union), `SkillMultipliers.cs` | Sealed class → discriminated union via inheritance |
| `Faction.kt` | `Faction.cs`, `FactionMainframe.cs`, `FactionBuff.cs`, `BuffType.cs`, `FactionUpgrade.cs`, `FactionCycleSnapshot.cs` | Split |
| `Challenge.kt` | `ChallengeDefinition.cs`, `ChallengeCadence.cs`, `ActiveChallenge.cs` | |
| `AdminModels.kt` | `BanRecord.cs`, `AdminAction.cs`, `CheatAlert.cs`, `CheatAlertType.cs`, `CheatSeverity.cs`, `AdminPlayerView.cs` | |
| `Research.kt` | `InventoryItem.cs`, `ItemType.cs`, `ItemRarity.cs`, `CraftingRecipe.cs`, `Ingredient.cs`, `ResearchProgress.cs`, `CraftingRecipes.cs` | |
| `Notification.kt` | `Notification.cs`, `NotificationType.cs` | |
| `NetworkNode.kt` | `NetworkNode.cs`, `NodeType.cs`, `AccessLevel.cs`, `NetworkTopology.cs` | |
| `Zone.kt` | `ZoneFaction.cs`, `ZoneState.cs`, `Zone.cs`, `ZoneSnapshot.cs` | |
| `WorldEvent.kt` | `WorldEvent.cs`, `WorldEventType.cs`, `WorldEventSeverity.cs`, `WorldEventEffect.cs`, `EffectType.cs` | |
| `MissionModifier.kt` | `MissionModifier.cs`, `ModifierType.cs`, `ModifierRarity.cs`, `EnhancedContract.cs`, `MissionModifiers.cs` | |

**Design Decisions:**
- Kotlin `data class` → C# `record` or `class` with `init`-only properties for immutability
- Kotlin `enum class` → C# `enum`
- Kotlin `sealed class` (e.g., `SkillEffect`) → C# discriminated union via base class + subclass pattern, or `OneOf` library
- JSON annotations: `[JsonPropertyName("snake_case_name")]` on all properties to match Kotlin serialization
- Kotlin `object` → C# `public static class`
- Kotlin `companion object` → C# `public static class` nested or separate
- Kotlin default parameter values → C# optional parameters or overloads
- Kotlin `require()`/`check()` → C# `ArgumentException.ThrowIfNullOrEmpty()` / `ArgumentOutOfRangeException.ThrowIfNegative()`

#### `com.zeroday.security` → `ZeroDayMMO.Shared.Security`

| Kotlin File | C# File | Notes |
|-------------|---------|-------|
| `PasswordHasher.kt` | `IPasswordHasher.cs`, `BcryptPasswordHasher.cs` | Interface + implementation |
| `RateLimiter.kt` | `RateLimiter.cs` | Sliding-window, swap `ConcurrentHashMap` → `ConcurrentDictionary` |
| `InputValidation.kt` | `InputValidation.cs` | Static helper class |
| `AuditLog.kt` | `AuditLog.cs` | Structured logging via `ILogger` |

**Design Decisions:**
- `RateLimiter` uses `ConcurrentHashMap<String, MutableList<Long>>` → `ConcurrentDictionary<string, List<long>>` (use `ConcurrentQueue<long>` for O(1) enqueue/dequeue)
- `AuditLog` uses SLF4J MDC → `ILogger.BeginScope()` with `Serilog` for structured logging
- Kotlin `inline class` (e.g., password hash wrapper) → C# `readonly record struct`

#### `com.zeroday.zdscript` → `ZeroDayMMO.Shared.ZDScript`

| Kotlin File | C# File | Notes |
|-------------|---------|-------|
| `ZDScriptModels.kt` | Multiple files: `Scanner.cs`, `Token.cs`, `TokenType.cs`, `Keywords.cs`, `Operators.cs`, `Parser.cs`, `AstNode.cs` (and subtypes), `Interpreter.cs`, `ScriptContext.cs`, `ScriptResult.cs`, `KnowledgeFragment.cs` | The largest conversion effort — full lexer/parser/interpreter for custom scripting language |
| `ZDScriptEngine.kt` | `NexusScriptEngine.cs` | Pipeline orchestrator |

**Design Decisions:**
- Kotlin sealed class for AST nodes → C# discriminated union (base `AstNode` with subclasses)
- Kotlin `when` exhaustive matching → C# `switch` with patterns
- Kotlin `Sequence<T>` → C# `IEnumerable<T>` (lazy evaluation)
- ZDScript token types, keywords, and operators as C# enums

#### `com.zeroday.util` → `ZeroDayMMO.Shared.Utilities`

| Kotlin File | C# File | Notes |
|-------------|---------|-------|
| `IdGenerator.kt` | `IdGenerator.cs` | Static class — `Guid.NewGuid()` for UUIDs |
| `AppScope.kt` | *(not needed)* | CoroutineScope → Task Parallel Library (async/await handles this) |

---

### 2.2 Server Library (`ZeroDayMMO.Server`)

#### Root → `ZeroDayMMO.Server`

| Kotlin File | C# File | Notes |
|-------------|---------|-------|
| `ZeroDayServer.kt` | `Program.cs` | ASP.NET Core minimal API / Startup — configures SignalR, services, middleware |

**Design Decision:** Use ASP.NET Core Minimal API for startup. `WebApplication.CreateBuilder(args)` with SignalR hub registration. No reflection-based dispatch.

#### `com.zeroday.config` → `ZeroDayMMO.Server.Configuration`

| Kotlin File | C# File | Notes |
|-------------|---------|-------|
| `ServerConfig.kt` | `ServerConfig.cs` | `IOptions<ServerConfig>` pattern with env-var binding |
| `AdminConfig.kt` | `AdminConfig.cs` | Same pattern |

**Design Decision:** Use `IConfiguration` + `IOptions<T>` with `appsettings.json` and environment variable overrides (e.g., `ZERODAY_PORT` → `"Server:Port"` or named config section).

#### `com.zeroday.handler` → `ZeroDayMMO.Server.Handlers` (top-level infrastructure)

| Kotlin File | C# File | Notes |
|-------------|---------|-------|
| `MessageRouter.kt` | `MessageRouter.cs` | O(1) dispatch via `Dictionary<string, Func<HandlerContext, Task>>` |
| `HandlerContext.kt` | `HandlerContext.cs` | Context record with `HttpContext`, `ConnectionInfo`, `ServiceRegistry` |
| `WebSocketHandler.kt` | `GameHub.cs` (SignalR Hub) | SignalR `Hub<T>` replaces raw WS frame pump |
| `ConnectionRegistry.kt` | `ConnectionRegistry.cs` | Track connected users — `ConcurrentDictionary` |
| `ConnectionWatchdog.kt` | `ConnectionWatchdog.cs` | `IHostedService` background task |
| `ServiceRegistry.kt` | `ServiceRegistry.cs` | Aggregate reference — uses DI `IServiceProvider` |
| `JsonExt.kt` | `JsonExtensions.cs` | Extension methods on `JsonElement` / `JsonObject` |
| `AdminRoutes.kt` | `AdminController.cs` (or `AdminEndpoints.cs`) | ASP.NET Core MVC controller or minimal API endpoints |

**Design Decisions:**
- SignalR Hub replaces the raw WebSocket frame loop — types are sent as Hub method names (e.g., `SendAsync("login", payload)`)
- `MessageRouter` can be simplified: SignalR Hub methods already route by name; keep `MessageRouter` for internal routing if needed
- `ConnectionRegistry` integrates with SignalR `HubCallerContext` + `Groups`
- `ConnectionWatchdog` = `IHostedService` with `PeriodicTimer`
- `AdminRoutes` = ASP.NET Core minimal API endpoints on a secondary port (8081)

#### `com.zeroday.handler.handlers` → `ZeroDayMMO.Server.Handlers` (individual hubs)

Each handler Kotlin class becomes a **partial** of the SignalR Hub or a separate **Hub method group**.

| Kotlin File | C# File / Hub Method | Notes |
|-------------|----------------------|-------|
| `AuthHandler.kt` | `GameHub.Auth.cs` | `RegisterAsync`, `LoginAsync`, `LogoutAsync`, `PingAsync` |
| `PlayerHandler.kt` | `GameHub.Player.cs` | `GetStatusAsync`, `GetOnlinePlayersAsync`, `GetLeaderboardAsync`, `CreatePartyAsync`, `JoinPartyAsync` |
| `CommandHandler.kt` | `GameHub.Command.cs` | `ExecuteCommandAsync` |
| `StorylineHandler.kt` | `GameHub.Storyline.cs` | `GetStorylinesAsync`, `StartStorylineAsync`, `AdvanceStorylineAsync` |
| `TaskHandler.kt` | `GameHub.Task.cs` | `GetTasksAsync`, `AcceptTaskAsync`, `CompleteTaskAsync` |
| `NetworkHandler.kt` | `GameHub.Network.cs` | `DiscoverNetworkAsync`, `DiscoverNodeAsync`, `ScanSubnetAsync` |
| `FactionHandler.kt` | `GameHub.Faction.cs` | `CreateFactionAsync`, `JoinFactionAsync`, `LeaveFactionAsync`, `DonateAsync`, `UpgradeAsync` |
| `ResearchHandler.kt` | `GameHub.Research.cs` | `GetRecipesAsync`, `GetInventoryAsync`, `GatherFragmentsAsync`, `StartCraftingAsync`, `ClaimCraftingAsync`, `UseItemAsync` |
| `NexusHandler.kt` | `GameHub.Nexus.cs` | `RunScriptAsync`, `SaveScriptAsync`, `ListScriptsAsync`, `DeleteScriptAsync`, `ValidateScriptAsync` |
| `KMapHandler.kt` | `GameHub.KnowledgeMap.cs` | `GetKnowledgeMapAsync`, `DiscoverFragmentAsync`, `ListAvailableAsync` |
| `WorldEventHandler.kt` | `GameHub.WorldEvent.cs` | `ListActiveEventsAsync`, `JoinEventAsync`, `LeaveEventAsync` |
| `AdHandler.kt` | `GameHub.Ad.cs` | `WatchAdAsync` |
| `AchievementHandler.kt` | `GameHub.Achievement.cs` | `GetAchievementsAsync`, `ClaimAchievementAsync` |
| `ChallengeHandler.kt` | `GameHub.Challenge.cs` | `GetChallengesAsync`, `ClaimChallengeAsync`, `RotateChallengeAsync` |
| `NotificationHandler.kt` | `GameHub.Notification.cs` | `GetNotificationsAsync`, `MarkReadAsync`, `MarkAllReadAsync` |
| `SkillHandler.kt` | `GameHub.Skill.cs` | `GetSkillTreeAsync`, `UnlockSkillAsync` |
| `ProfileHandler.kt` | `GameHub.Profile.cs` | `GetProfileAsync` |
| `CareerHandler.kt` | `GameHub.Career.cs` | `ChooseCareerAsync`, `GetCareerStatusAsync`, `AddHeatAsync`, `ArrestAsync`, `GetHunterStatusAsync` |
| `ZoneHandler.kt` | `GameHub.Zone.cs` | `GetZoneInfoAsync`, `ListZonesAsync`, `TravelAsync`, `AttackZoneAsync`, `ClaimZoneAsync`, `GetFactionCycleAsync` |
| `BossHandler.kt` | `GameHub.Boss.cs` | `CreateBossInstanceAsync`, `JoinBossInstanceAsync`, `BossActionAsync`, `GetBossStatusAsync`, `ListBossInstancesAsync` |
| `AuctionHandler.kt` | `GameHub.Auction.cs` | `ListAuctionsAsync`, `SearchAuctionsAsync`, `CreateAuctionAsync`, `BidAsync`, `BuyoutAsync`, `GetMyListingsAsync`, `CancelAuctionAsync` |
| `GameEventHandler.kt` | `GameHub.GameEvent.cs` | `ListGameEventsAsync`, `JoinGameEventAsync`, `LeaveGameEventAsync`, `GetLeaderboardAsync`, `GetScoreAsync`, `GetMyEventsAsync` |
| `SubscriptionHandler.kt` | *(SignalR Groups)* | Channel subscriptions → SignalR Groups (`Groups.AddToGroupAsync`) |

**Design Decision:** Use partial classes for `GameHub` to split handler methods across files (like the Kotlin handler classes are separate files but all part of `handlers` package). Each C# partial file corresponds to one Kotlin handler file.

#### `com.zeroday.service` → `ZeroDayMMO.Server.Services`

All services registered as singletons or scoped in DI.

| Kotlin File | C# File | DI Lifetime | Notes |
|-------------|---------|-------------|-------|
| `PlayerService.kt` | `PlayerService.cs` | Singleton | Player CRUD, per-player `SemaphoreSlim` locks |
| `PlayerPersistence.kt` | `PlayerPersistenceService.cs` | Singleton | Periodic JSON snapshot, `IHostedService` |
| `CommandService.kt` | `CommandService.cs` | Singleton | 30+ command implementations |
| `EventService.kt` | `EventService.cs` | Singleton | Storyline progression |
| `TaskService.kt` | `TaskService.cs` | Singleton | Background task generation, `IHostedService` |
| `NetworkService.kt` | `NetworkService.cs` | Singleton | Network topology |
| `FactionService.kt` | `FactionService.cs` | Singleton | Faction management, passive income, `IHostedService` |
| `ResearchService.kt` | `ResearchService.cs` | Singleton | Crafting/research |
| `AchievementService.kt` | `AchievementService.cs` | Singleton | Achievement definitions and progress |
| `GameEventBus.kt` | `GameEventBus.cs` | Singleton | In-process pub/sub, `Channel<T>` or `System.Reactive` |
| `GameEventService.kt` | `GameEventService.cs` | Singleton | PvP events |
| `ServerScheduler.kt` | `ServerStatsLogger.cs` | `IHostedService` | Periodic stats logging |
| `ResourceRegenTicker.kt` | `ResourceRegenService.cs` | `IHostedService` | 5s regen ticker |
| `ServerStats.kt` | `ServerStats.cs` | Model | JSON response |
| `ChallengeService.kt` | `ChallengeService.cs` | Singleton | Daily/weekly challenges |
| `NotificationService.kt` | `NotificationService.cs` | Singleton | Player inbox, cap at 100 |
| `SkillService.kt` | `SkillService.cs` | Singleton | Skill trees |
| `BanService.kt` | `BanService.cs` | Singleton | Ban/unban with audit |
| `AdminService.kt` | `AdminService.cs` | Singleton | Admin auth, logs |
| `ContractGenerator.kt` | `ContractGeneratorService.cs` | Singleton | Procedural contracts |
| `HackToLearnService.kt` | `HackToLearnService.cs` | Singleton | Knowledge fragments |
| `CareerService.kt` | `CareerService.cs` | Singleton | Career path, heat, jail |
| `DailyLoginService.kt` | `DailyLoginService.cs` | Singleton | Login streaks |
| `HeatCascadeService.kt` | `HeatCascadeService.cs` | Singleton | NPC hunters, `IHostedService` |
| `CoopBossService.kt` | `CoopBossService.cs` | Singleton | Boss instances |
| `AuctionService.kt` | `AuctionService.cs` | Singleton | Auction house, `IHostedService` (expiry) |
| `WorldEventService.kt` | `WorldEventService.cs` | Singleton | World events, `IHostedService` |
| `WorldZoneService.kt` | `WorldZoneService.cs` | Singleton | Zone travel, faction control |
| `CheatDetectionService.kt` | `CheatDetectionService.cs` | Singleton | Anti-cheat |
| `AdRewardService.kt` | `AdRewardService.cs` | Singleton | Ad rewards |

**Design Decisions:**
- Kotlin `suspend fun` → C# `async Task`
- Kotlin `launch { }` → C# `Task.Run()` or `Task.Factory.StartNew()` inside `IHostedService`
- Kotlin `flow` → C# `IAsyncEnumerable<T>` or `Channel<T>`
- Kotlin `Mutex` / `synchronized` → C# `SemaphoreSlim` (per-player locks in `PlayerService`)
- Kotlin `mutableStateOf` / `MutableStateFlow` → C# `ConcurrentDictionary` or `ReaderWriterLockSlim` for player state
- Kotlin `delay()` → C# `Task.Delay()`
- Kotlin coroutine `Scope` → C# `CancellationTokenSource` (scoped cancellation)
- Kotlin `Job` → C# `Task`
- In-memory state remains in-memory (same as Kotlin) — snapshot to disk
- `GameEventBus`: Kotlin uses `MutableSharedFlow<AchievementEvent>` + coroutine dispatch → C# `Channel<AchievementEvent>` + `ChannelReader<T>.ReadAllAsync()` with background consumer

#### Kotlin patterns → C# equivalents (cross-cutting)

| Kotlin | C# | Notes |
|--------|-----|-------|
| `object` (singleton) | `static class` or DI singleton | |
| `companion object` | `static class` or nested static members | |
| `data class` | `record` (for DTOs) or `class` with `init` accessors | |
| `data class` with defaults | `record` with optional constructor params | |
| `sealed class` | Base class + subclasses (discriminated union) or `OneOf<T1, T2>` | |
| `sealed interface` | Interface | |
| `enum class` with properties | `enum` with `DescriptionAttribute` or `EnumMemberAttribute` | |
| `inline class` | `readonly record struct` | |
| `require()` / `check()` | `ArgumentException.ThrowIfNullOrEmpty()` / `ArgumentOutOfRangeException.ThrowIfNegative()` | .NET 8 built-in |
| `?.let { }` | `?.` + pattern match or `if (x is not null)` | |
| `!!` (assert not null) | `!` null-forgiving operator (use sparingly) | |
| `?:` (elvis) | `??` (null-coalescing) | |
| `by lazy { }` | `Lazy<T>` | |
| `?.` (safe call) | `?.` (null-conditional) | Same operator |
| `filterNotNull()` | `.Where(x => x is not null)` | |
| `firstOrNull { pred }` | `.FirstOrDefault(pred)` | |
| `map { }` / `flatMap { }` | `.Select()` / `.SelectMany()` | LINQ |
| `forEach { }` | `foreach` loop or `.ToList().ForEach()` | |
| `groupBy { }` | `.GroupBy()` | LINQ |
| `sortedBy { }` | `.OrderBy()` | LINQ |
| `take(n)` | `.Take(n)` | LINQ |
| `filter { }` | `.Where()` | LINQ |
| `toSet()` / `distinct()` | `.Distinct()` / `.ToHashSet()` | |
| `associateWith { }` | `.ToDictionary(keySelector, valueSelector)` | LINQ |
| `zip` | `.Zip()` | LINQ |
| `chunked(n)` | `.Chunk(n)` | .NET 6+ |
| `buildString { }` | `new StringBuilder().Append().ToString()` | |
| `kotlin.random.Random` | `System.Random` or `Random.Shared` | |
| `kotlinx.datetime.Instant` | `DateTimeOffset` / `DateTime.UtcNow` | |
| `kotlin.time.Duration` | `TimeSpan` / `TimeProvider` | .NET 8 `TimeProvider` |
| `UUID.randomUUID()` (IdGenerator) | `Guid.NewGuid().ToString()` | |
| `UInt` / `ULong` | `uint` / `ulong` | |
| `IntRange` | `Enumerable.Range()` | |
| `ClosedRange<T>` | `..` range operator or custom `Range<T>` | |
| `kotlin.Result<T>` | No direct equivalent — use exceptions or `OneOf<T, Exception>` | |
| `runCatching { }` | `try { } catch { }` | |
| `kotlin.system.exitProcess()` | `Environment.Exit()` | |
| `println()` / `logger.info {}` | `ILogger.LogInformation()` | |

---

## 3. Architecture & Implementation Notes

### 3.1 SignalR Hub vs Raw WebSocket

The Kotlin server uses raw WebSocket frames with JSON envelopes (`{"type": "login", "payload": {...}}`).
SignalR provides:
- Strongly-typed Hub method calls (no string-based type routing)
- Built-in JSON serialization (or MessagePack)
- Connection management (Groups, UserId, ConnectionId)
- Reconnection handling
- Backpressure

**Migration approach:**
- Keep the existing JSON envelope protocol as the **wire format** for backward compatibility
- OR adopt SignalR native protocol (Hub method name = type, arguments = payload)

**Recommended: Hybrid approach**
- Each Hub method accepts a `JsonElement` payload parameter
- Server dispatches to the same service layer
- Clients use SignalR `.InvokeAsync("login", payload)` instead of raw `SendAsync`

### 3.2 Startup Flow

```csharp
// Program.cs
var builder = WebApplication.CreateBuilder(args);

// Configuration
builder.Services.Configure<ServerConfig>(builder.Configuration.GetSection("Server"));
builder.Services.Configure<AdminConfig>(builder.Configuration.GetSection("Admin"));

// Singletons (stateful services)
builder.Services.AddSingleton<PlayerService>();
builder.Services.AddSingleton<CommandService>();
// ... all singleton services ...

// IHostedService (background loops)
builder.Services.AddHostedService<PlayerPersistenceService>();
builder.Services.AddHostedService<TaskGenerationService>();
builder.Services.AddHostedService<ResourceRegenService>();
builder.Services.AddHostedService<HeatCascadeService>();
// ... etc ...

// SignalR
builder.Services.AddSignalR()
    .AddJsonProtocol(options => { options.PayloadSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower; });

// Admin endpoints
builder.Services.AddControllers(); // or minimal API

var app = builder.Build();

app.MapHub<GameHub>("/game");

app.MapGet("/", () => "ZeroDayMMO Server");
app.MapGet("/health", () => Results.Ok(new { status = "healthy" }));
app.MapGet("/live", () => Results.Ok("alive"));
app.MapGet("/ready", () => Results.Ok("ready"));
app.MapGet("/stats", (ServerStats stats) => stats.GetSnapshot());

// Admin endpoints on separate port
app.MapAdminEndpoints(); // extension method

app.Run();
```

### 3.3 Background Services Pattern

Each background loop (task generation, faction income, resource regen, etc.) becomes:

```csharp
public class TaskGenerationService : IHostedService, IDisposable
{
    private Timer? _timer;

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _timer = new Timer(DoWork, null, TimeSpan.Zero, TimeSpan.FromSeconds(60));
        return Task.CompletedTask;
    }

    private void DoWork(object? state) { /* ... */ }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        _timer?.Change(Timeout.Infinite, 0);
        return Task.CompletedTask;
    }

    public void Dispose() => _timer?.Dispose();
}
```

For async work inside the timer:
```csharp
private async void DoWork(object? state)
{
    try { await _playerService.DoSomethingAsync(); }
    catch (Exception ex) { _logger.LogError(ex, "Error in background task"); }
}
```

### 3.4 GameEventBus

Kotlin uses `MutableSharedFlow<AchievementEvent>` with subscribers launched as coroutines.

**C# equivalent with `System.Threading.Channels`:**

```csharp
public class GameEventBus
{
    private readonly Channel<AchievementEvent> _channel = Channel.CreateUnbounded<AchievementEvent>();
    private readonly List<Func<AchievementEvent, Task>> _listeners = new();

    public void Publish(AchievementEvent evt) => _channel.Writer.TryWrite(evt);

    public void Subscribe(Func<AchievementEvent, Task> listener)
    {
        lock (_listeners) { _listeners.Add(listener); }
    }

    public async Task StartDispatchingAsync(CancellationToken ct)
    {
        await foreach (var evt in _channel.Reader.ReadAllAsync(ct))
        {
            List<Func<AchievementEvent, Task>> listeners;
            lock (_listeners) { listeners = _listeners.ToList(); }
            await Task.WhenAll(listeners.Select(l => l(evt)));
        }
    }
}
```

### 3.5 Per-Player Mutex Locks

Kotlin uses `ConcurrentHashMap<String, Mutex>` with `mutex.withLock { }`.

C#:
```csharp
private readonly ConcurrentDictionary<string, SemaphoreSlim> _playerLocks = new();

public async Task UpdatePlayerAsync(string playerId, Func<Player, Task> update)
{
    var semaphore = _playerLocks.GetOrAdd(playerId, _ => new SemaphoreSlim(1, 1));
    await semaphore.WaitAsync();
    try
    {
        var player = GetPlayer(playerId);
        await update(player);
    }
    finally { semaphore.Release(); }
}
```

### 3.6 JSON Serialization

Kotlin uses `kotlinx.serialization` with `@SerialName("snake_case")`.

C#: Use `System.Text.Json` with `JsonNamingPolicy.SnakeCaseLower` and `[JsonPropertyName("original_name")]` for any non-conforming cases.

```csharp
// Program.cs
builder.Services.AddSignalR()
    .AddJsonProtocol(options =>
    {
        options.PayloadSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower;
        options.PayloadSerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull;
    });
```

### 3.7 Admin Routes

Kotlin serves admin on port 8081 via Ktor routes. In ASP.NET Core:

```csharp
// Use WebApplication.CreateBuilder to set up Kestrel with two endpoints
builder.WebHost.ConfigureKestrel(options =>
{
    options.Listen(IPAddress.Any, 8080); // Game
    options.Listen(IPAddress.Any, 8081); // Admin
});
```

Then use app.MapGroup for admin endpoints with a custom middleware for token auth.

### 3.8 Config (Environment Variables)

`ServerConfig.Port` → `IConfiguration["Server:Port"]` or env var `Server__Port` / `ZERODAY_PORT`

```csharp
public class ServerConfig
{
    public int Port { get; set; } = 8080;
    public int AdminPort { get; set; } = 8081;
    public int MaxConnections { get; set; } = 2000;
    public int MaxConnectionsPerIp { get; set; } = 8;
    public int RateLimitPerPlayer { get; set; } = 30;
    // ... all other fields from ServerConfig.kt
}
```

```json
// appsettings.json
{
  "Server": {
    "Port": 8080,
    "AdminPort": 8081,
    "MaxConnections": 2000,
    "MaxConnectionsPerIp": 8,
    "RateLimitPerPlayer": 30
  }
}
```

---

## 4. Porting Order (Recommended Sequence)

### Phase 1: Shared Library (No ASP.NET dependency)
1. **Models layer** — All model classes/enums/records with JSON attributes
2. **Protocol layer** — MessageProtocol, RequestTypes, ResponseTypes, ServerError, ErrorCategory
3. **Security layer** — PasswordHasher, RateLimiter, InputValidation, AuditLog
4. **Utilities** — IdGenerator

### Phase 2: Server Skeleton
5. **Project setup** — ASP.NET Core + SignalR + DI configuration
6. **Config** — ServerConfig, AdminConfig with IOptions binding
7. **Program.cs** — Startup with Kestrel, SignalR hub, admin endpoints
8. **ConnectionRegistry + ConnectionWatchdog** — Connection tracking and idle sweeper
9. **ServiceRegistry** — DI service aggregation

### Phase 3: Services (No Hub dependency)
10. **GameEventBus** — Event bus infrastructure
11. **PlayerService + PlayerPersistence** — Core state management
12. **CommandService** — All 30+ commands
13. **NotificationService** — Player inbox

### Phase 4: Domain Services
14. **SkillService + AchievementService** — Progression systems
15. **EventService + TaskService** — Storyline and tasks
16. **NetworkService** — Network topology
17. **FactionService** — Faction management
18. **ResearchService** — Crafting/research
19. **CareerService + HeatCascadeService** — Career and heat
20. **ChallengeService** — Daily/weekly challenges
21. **WorldZoneService + WorldEventService** — World systems
22. **CoopBossService + AuctionService** — Multiplayer features
23. **GameEventService** — PvP events
24. **ContractGenerator + HackToLearnService** — Content generation
25. **DailyLoginService + AdRewardService** — Engagement systems
26. **CheatDetectionService + BanService + AdminService** — Administration
27. **ServerStats + ServerScheduler** — Monitoring
28. **ResourceRegenTicker** — Resource regeneration

### Phase 5: SignalR Hub (Handlers → Hub Methods)
29. **GameHub base class** — Common Hub infrastructure, error handling, auth
30. **Auth methods** — Register, Login, Logout, Ping
31. **Core game methods** — Player status, commands
32. **Progression methods** — Skills, achievements, challenges, storyline
33. **Domain methods** — Network, tasks, research, factions, zones
34. **Multiplayer methods** — Boss, auction, game events, parties
35. **Meta methods** — Notifications, profile, career, ads, nexus scripting
36. **Subscription methods** — Channel subscriptions (SignalR Groups)

### Phase 6: ZDScript (Complex Custom Language)
37. **Token types and scanner** — Lexer
38. **Parser and AST nodes** — Full grammar parser
39. **Interpreter and context** — Runtime
40. **NexusScriptEngine** — Pipeline orchestrator
41. **KnowledgeFragment system** — Learning/unlocking

### Phase 7: Tests (Parallel with all phases)
42. **Security tests** — RateLimiter, InputValidation, PasswordHasher
43. **Service tests** — PlayerService, NotificationService, DailyLoginService, etc.
44. **Event bus tests** — GameEventBus wiring, AchievementService events
45. **Handler tests** — MessageRouter, connection management
46. **Integration tests** — Hub method end-to-end

---

## 5. Riak KV Note

The Kotlin codebase **does not use Riak KV** in any source file despite the `deploy.sh` mentioning it. All state is in-memory with periodic JSON snapshots. The C# port should likewise use in-memory state with disk persistence — no database dependency needed.

---

## 6. Key Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| ZDScript custom language is complex to port | Port last (Phase 6); use a PEG parser library (e.g., `Pidgin` or `Superpower`) instead of hand-rolled recursive descent |
| Kotlin sealed class pattern matching is more powerful than C# switch | Use C# 8+ switch expressions with patterns; or use `OneOf<T1,T2>` library |
| Kotlin coroutines are pervasive; async/await is not always a direct 1:1 | Use `Task.WhenAll`, `Channel<T>`, and `System.Threading.Channels` for fan-out; be careful with `ConfigureAwait(false)` |
| Per-player `Mutex` in Kotlin has no contention overhead; `SemaphoreSlim` has more overhead | Acceptable trade-off; use `SemaphoreSlim(1,1)` with `WaitAsync()` |
| SignalR Groups vs Ktor channel subscriptions | SignalR Groups are the natural equivalent; map `subscribe`/`unsubscribe` to `Groups.AddToGroupAsync`/`RemoveFromGroupAsync` |
| Kotlin `kotlinx.serialization` polymorphic serialization (sealed classes) | Use `System.Text.Json` custom `JsonConverter` for polymorphic types, or `JsonDerivedType` attribute |
| No `buildString {}` equivalent | Use `StringBuilder` directly |

---

## 7. File Count Summary

| Layer | Kotlin Files | C# Files (estimated) |
|-------|--------------|---------------------|
| Models | 17 | ~45 (split by type) |
| Protocol | 1 | ~5 (split by concern) |
| Security | 4 | ~6 |
| ZDScript | 2 | ~12 |
| Utilities | 2 | ~1 |
| Config | 2 | ~2 |
| Handler infrastructure | 7 | ~7 |
| Handler implementations | 20 | ~4 (partial Hub files) |
| Services | 27 | ~27 |
| Tests | 18 | ~18 |
| **Total** | **67** | **~127** |
