using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using ZeroDayMMO.Server.Handlers;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;
using ZeroDayMMO.Shared.Protocol;

namespace ZeroDayMMO.Server.WebSocket;

public class WebSocketHandler
{
    private readonly WebSocketConnectionManager _connectionManager;
    private readonly ConnectionRegistry _connectionRegistry;
    private readonly MessageRouter _messageRouter;
    private readonly Config.ServerConfig _config;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower
    };

    public WebSocketHandler(
        WebSocketConnectionManager connectionManager,
        ConnectionRegistry connectionRegistry,
        MessageRouter messageRouter,
        Config.ServerConfig config)
    {
        _connectionManager = connectionManager;
        _connectionRegistry = connectionRegistry;
        _messageRouter = messageRouter;
        _config = config;
    }

    public async Task HandleConnectionAsync(System.Net.WebSockets.WebSocket socket, string connectionId, string remoteIp)
    {
        if (!_connectionRegistry.TryRegister(connectionId, remoteIp))
        {
            await CloseSocketAsync(socket, WebSocketCloseStatus.PolicyViolation, "Connection limit exceeded");
            return;
        }

        _connectionManager.Add(connectionId, socket);

        try
        {
            await ReceiveLoopAsync(socket, connectionId);
        }
        finally
        {
            _connectionManager.Remove(connectionId);
            _connectionRegistry.Unregister(connectionId);

            if (socket.State != WebSocketState.Closed && socket.State != WebSocketState.Aborted)
            {
                try
                {
                    await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Connection closed", CancellationToken.None);
                }
                catch { }
            }

            socket.Dispose();
        }
    }

    private async Task ReceiveLoopAsync(System.Net.WebSockets.WebSocket socket, string connectionId)
    {
        var buffer = new byte[_config.FrameSizeLimit];
        var messageBuffer = new StringBuilder();

        while (socket.State == WebSocketState.Open)
        {
            WebSocketReceiveResult result;
            try
            {
                result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
            }
            catch (WebSocketException) { break; }
            catch (OperationCanceledException) { break; }

            if (result.MessageType == WebSocketMessageType.Close)
                break;

            if (result.MessageType == WebSocketMessageType.Text)
            {
                messageBuffer.Append(Encoding.UTF8.GetString(buffer, 0, result.Count));

                if (result.EndOfMessage)
                {
                    var raw = messageBuffer.ToString();
                    messageBuffer.Clear();

                    if (!_connectionRegistry.TryConsumeRateLimit(connectionId))
                    {
                        await SendToConnectionAsync(connectionId,
                            MessageProtocol.Serialize(MessageProtocol.Create("error", new { message = "Rate limit exceeded" })));
                        continue;
                    }

                    await ProcessMessageAsync(connectionId, raw);
                }
            }
        }
    }

    private async Task ProcessMessageAsync(string connectionId, string raw)
    {
        GameMessage? message;
        try
        {
            message = MessageProtocol.Parse(raw);
        }
        catch (JsonException)
        {
            await SendToConnectionAsync(connectionId,
                MessageProtocol.Serialize(MessageProtocol.Create("error", new { message = "Invalid JSON" })));
            return;
        }

        if (message is null)
        {
            await SendToConnectionAsync(connectionId,
                MessageProtocol.Serialize(MessageProtocol.Create("error", new { message = "Empty message" })));
            return;
        }

        var result = await _messageRouter.RouteAsync(connectionId, message);
        await DispatchResultAsync(result, message);
    }

    private async Task DispatchResultAsync(IActionResult result, GameMessage original)
    {
        switch (result)
        {
            case MessageResult mr:
                var response = MessageProtocol.Create(mr.Type, mr.Payload, original.RequestId);
                await SendToConnectionAsync(mr.ConnectionId ?? string.Empty, MessageProtocol.Serialize(response));
                break;

            case BroadcastResult br:
                var broadcast = MessageProtocol.Create(br.Type, br.Payload, null);
                await _connectionManager.BroadcastAsync(MessageProtocol.Serialize(broadcast), br.ExcludeConnectionId);
                break;

            case EmptyResult:
                break;
        }
    }

    private async Task SendToConnectionAsync(string connectionId, string message)
    {
        await _connectionManager.SendAsync(connectionId, message);
    }

    private static async Task CloseSocketAsync(System.Net.WebSockets.WebSocket socket, WebSocketCloseStatus status, string reason)
    {
        try
        {
            await socket.CloseAsync(status, reason, CancellationToken.None);
        }
        catch { }
        socket.Dispose();
    }
}
