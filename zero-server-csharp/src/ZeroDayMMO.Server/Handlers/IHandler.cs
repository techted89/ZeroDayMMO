using System.Text.Json;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public interface IHandler
{
    string MessageType { get; }
    Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId);
}

public class HandlerDelegate : IHandler
{
    public string MessageType { get; }
    private readonly Func<string, JsonElement?, string?, Task<IActionResult>> _handle;

    public HandlerDelegate(string messageType, Func<string, JsonElement?, string?, Task<IActionResult>> handle)
    {
        MessageType = messageType;
        _handle = handle;
    }

    public Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
        => _handle(connectionId, payload, requestId);
}
