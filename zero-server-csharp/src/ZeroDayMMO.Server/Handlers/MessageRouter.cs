using ZeroDayMMO.Shared;
using ZeroDayMMO.Shared.Protocol;

namespace ZeroDayMMO.Server.Handlers;

public class MessageRouter
{
    private readonly Dictionary<string, IHandler> _handlers = new();

    public void Register(IHandler handler)
    {
        _handlers[handler.MessageType] = handler;
    }

    public async Task<IActionResult> RouteAsync(string connectionId, GameMessage message)
    {
        if (message.Type is null)
        {
            return new MessageResult("error", connectionId, new { message = "Missing message type" });
        }

        if (!_handlers.TryGetValue(message.Type, out var handler))
        {
            return new MessageResult("error", connectionId, new { message = "Unknown message type" });
        }

        return await handler.HandleAsync(connectionId, message.Payload, message.RequestId);
    }
}
