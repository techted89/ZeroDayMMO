using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Text;

namespace ZeroDayMMO.Server.Services;

public class WebSocketConnectionManager
{
    private readonly ConcurrentDictionary<string, System.Net.WebSockets.WebSocket> _connections = new();

    public void Add(string connectionId, System.Net.WebSockets.WebSocket socket)
    {
        _connections[connectionId] = socket;
    }

    public void Remove(string connectionId)
    {
        _connections.TryRemove(connectionId, out _);
    }

    public System.Net.WebSockets.WebSocket? Get(string connectionId)
    {
        return _connections.TryGetValue(connectionId, out var socket) ? socket : null;
    }

    public async Task SendAsync(string connectionId, string message)
    {
        if (!_connections.TryGetValue(connectionId, out var socket))
            return;

        if (socket.State != WebSocketState.Open)
            return;

        var bytes = Encoding.UTF8.GetBytes(message);
        try
        {
            await socket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, CancellationToken.None);
        }
        catch { }
    }

    public async Task BroadcastAsync(string message, string? excludeConnectionId = null)
    {
        var bytes = Encoding.UTF8.GetBytes(message);
        var tasks = new List<Task>();

        foreach (var (id, socket) in _connections)
        {
            if (id == excludeConnectionId)
                continue;

            if (socket.State != WebSocketState.Open)
                continue;

            tasks.Add(SendSafeAsync(socket, bytes));
        }

        if (tasks.Count > 0)
            await Task.WhenAll(tasks);
    }

    private static async Task SendSafeAsync(System.Net.WebSockets.WebSocket socket, byte[] bytes)
    {
        try
        {
            await socket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, CancellationToken.None);
        }
        catch { }
    }
}
