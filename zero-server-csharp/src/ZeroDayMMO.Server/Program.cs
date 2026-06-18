using ZeroDayMMO.Server.Config;
using ZeroDayMMO.Server.Handlers;
using ZeroDayMMO.Server.Security;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Server.WebSocket;

var config = new ServerConfig();

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.ConfigureKestrel(options =>
{
    options.ListenAnyIP(config.Port);
    options.Limits.MaxRequestBodySize = config.FrameSizeLimit;
});

// --- Services ---
var passwordHasher = new BcryptPasswordHasher();
var playerService = new PlayerService(passwordHasher);
var connectionRegistry = new ConnectionRegistry(config);
var connectionManager = new WebSocketConnectionManager();
var gameEventBus = new GameEventBus();
var notificationService = new NotificationService(playerService);
var achievementService = new AchievementService();
var skillService = new SkillService();
var commandService = new CommandService(playerService);
var networkService = new NetworkService(playerService, gameEventBus);
var worldZoneService = new WorldZoneService(playerService);
var factionService = new FactionService(playerService, gameEventBus);
var researchService = new ResearchService(playerService, gameEventBus);
var challengeService = new ChallengeService();
var taskService = new TaskService(playerService, new ContractGenerator(playerService));
var auctionService = new AuctionService(playerService, gameEventBus);
var careerService = new CareerService(playerService, gameEventBus);
var heatCascadeService = new HeatCascadeService(playerService, careerService, gameEventBus);
var hackToLearnService = new HackToLearnService(playerService);
var dailyLoginService = new DailyLoginService(playerService);
var coopBossService = new CoopBossService(playerService, worldZoneService);
var nexusService = new NexusService(playerService, gameEventBus);
var serverScheduler = new ServerScheduler(playerService, connectionRegistry, null, new Microsoft.Extensions.Logging.Abstractions.NullLogger<ServerScheduler>());

// --- Handlers ---
var messageRouter = new MessageRouter();

messageRouter.Register(new LoginHandler(playerService));
messageRouter.Register(new ChatHandler(connectionManager, playerService));
messageRouter.Register(new CommandHandler(commandService));
messageRouter.Register(new PlayerHandler(playerService));
messageRouter.Register(new SkillHandler(playerService, skillService));
messageRouter.Register(new AchievementHandler(playerService, achievementService, notificationService));
messageRouter.Register(new TaskHandler(taskService, playerService));
messageRouter.Register(new ChallengeHandler(playerService, challengeService, notificationService));
messageRouter.Register(new ProfileHandler(playerService, skillService, achievementService));
messageRouter.Register(new NetworkHandler(networkService));
messageRouter.Register(new ZoneHandler(worldZoneService, playerService));
messageRouter.Register(new FactionHandler(factionService, playerService));
messageRouter.Register(new ResearchHandler(researchService, playerService));
messageRouter.Register(new AuctionHandler(auctionService, playerService));
messageRouter.Register(new BossHandler(coopBossService, playerService));
messageRouter.Register(new CareerHandler(careerService, heatCascadeService, playerService));
messageRouter.Register(new GameEventHandler(playerService));
messageRouter.Register(new KMapHandler(hackToLearnService));
messageRouter.Register(new NotificationHandler(notificationService, playerService));
messageRouter.Register(new WorldEventHandler(playerService, notificationService));
messageRouter.Register(new StorylineHandler(playerService));
messageRouter.Register(new SubscriptionHandler());
messageRouter.Register(new AdHandler(playerService));
messageRouter.Register(new AuthHandler(playerService));
messageRouter.Register(new NexusHandler());

var gameHandler = new GameHandler(playerService);
messageRouter.Register(gameHandler);
messageRouter.Register(new HandlerDelegate("game_command", gameHandler.HandleAsync));

var webSocketHandler = new WebSocketHandler(connectionManager, connectionRegistry, messageRouter, config);

// --- DI Registration ---
builder.Services.AddSingleton(config);
builder.Services.AddSingleton<IPlayerService>(playerService);
builder.Services.AddSingleton(connectionRegistry);
builder.Services.AddSingleton(connectionManager);
builder.Services.AddSingleton(messageRouter);
builder.Services.AddSingleton(gameEventBus);
builder.Services.AddSingleton(notificationService);
builder.Services.AddSingleton(achievementService);
builder.Services.AddSingleton(skillService);
builder.Services.AddSingleton(commandService);
builder.Services.AddSingleton(networkService);
builder.Services.AddSingleton(worldZoneService);
builder.Services.AddSingleton(factionService);
builder.Services.AddSingleton(researchService);
builder.Services.AddSingleton(challengeService);
builder.Services.AddSingleton(taskService);
builder.Services.AddSingleton(auctionService);
builder.Services.AddSingleton(careerService);
builder.Services.AddSingleton(heatCascadeService);
builder.Services.AddSingleton(hackToLearnService);
builder.Services.AddSingleton(dailyLoginService);
builder.Services.AddSingleton(coopBossService);
builder.Services.AddSingleton(nexusService);
builder.Services.AddSingleton(serverScheduler);

builder.Services.AddHostedService<PlayerPersistence>();
builder.Services.AddHostedService<ResourceRegenTicker>();
builder.Services.AddHostedService<HeatCascadeService>();
builder.Services.AddHostedService<ServerScheduler>();

var app = builder.Build();

app.UseWebSockets(new WebSocketOptions
{
    KeepAliveInterval = TimeSpan.FromSeconds(30)
});

var connectionCounter = 0;

app.Map("/game", async (HttpContext context) =>
{
    if (!context.WebSockets.IsWebSocketRequest)
    {
        context.Response.StatusCode = 400;
        await context.Response.WriteAsync("WebSocket connection expected");
        return;
    }

    var socket = await context.WebSockets.AcceptWebSocketAsync();
    var connectionId = Interlocked.Increment(ref connectionCounter).ToString();
    var remoteIp = context.Connection.RemoteIpAddress?.ToString() ?? "unknown";

    await webSocketHandler.HandleConnectionAsync(socket, connectionId, remoteIp);
});

Console.WriteLine($"ZeroDayMMO Server starting on port {config.Port}");
app.Run();
