using ZeroDayMMO.Server.Config;
using ZeroDayMMO.Server.Handlers;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Server.WebSocket;

var config = new ServerConfig();

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.ConfigureKestrel(options =>
{
    options.ListenAnyIP(config.Port);
    options.Limits.MaxRequestBodySize = config.FrameSizeLimit;
});

var playerService = new PlayerService();
var connectionRegistry = new ConnectionRegistry(config);
var connectionManager = new WebSocketConnectionManager();
var messageRouter = new MessageRouter();

var gameHandler = new GameHandler(playerService);
messageRouter.Register(new LoginHandler(playerService));
messageRouter.Register(gameHandler);
messageRouter.Register(new HandlerDelegate("game_command", gameHandler.HandleAsync));
messageRouter.Register(new ChatHandler(connectionManager, playerService));

var webSocketHandler = new WebSocketHandler(connectionManager, connectionRegistry, messageRouter, config);

builder.Services.AddSingleton(config);
builder.Services.AddSingleton(playerService);
builder.Services.AddSingleton(connectionRegistry);
builder.Services.AddSingleton(connectionManager);
builder.Services.AddSingleton(messageRouter);

builder.Services.AddHostedService<PlayerPersistence>();
builder.Services.AddHostedService<ResourceRegenTicker>();

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
