using System.Text.Json;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class SubscriptionHandler : IHandler
{
    private static readonly HashSet<string> AllChannels = new(StringComparer.OrdinalIgnoreCase)
    {
        "player", "notifications", "inventory", "tasks",
        "world_events", "storyline", "chat", "system"
    };

    private static readonly HashSet<string> DefaultChannels = new(StringComparer.OrdinalIgnoreCase)
    {
        "player", "notifications"
    };

    private readonly Dictionary<string, HashSet<string>> _subscriptions = new();

    public string MessageType => "subscription";

    public Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Missing payload" }));

        if (!payload.Value.TryGetProperty("channels", out var channelsElement) || channelsElement.ValueKind != JsonValueKind.Array)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "'channels' must be an array of channel names" }));

        var channelNames = new List<string>();
        foreach (var item in channelsElement.EnumerateArray())
        {
            if (item.ValueKind == JsonValueKind.String)
                channelNames.Add(item.GetString()!);
        }

        if (channelNames.Count == 0)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Empty channel list" }));

        var action = payload.Value.Str("action");
        return action switch
        {
            "subscribe" => HandleSubscribe(connectionId, channelNames),
            "unsubscribe" => HandleUnsubscribe(connectionId, channelNames),
            _ => Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = $"Unknown subscription action: {action}" }))
        };
    }

    private Task<IActionResult> HandleSubscribe(string connectionId, List<string> channels)
    {
        if (!_subscriptions.ContainsKey(connectionId))
            _subscriptions[connectionId] = new HashSet<string>(DefaultChannels, StringComparer.OrdinalIgnoreCase);

        var subs = _subscriptions[connectionId];
        var added = new List<string>();
        var invalid = new List<string>();

        foreach (var ch in channels)
        {
            if (subs.Contains(ch))
                continue;
            if (!AllChannels.Contains(ch))
            {
                invalid.Add(ch);
                continue;
            }
            subs.Add(ch);
            added.Add(ch);
        }

        return Task.FromResult<IActionResult>(
            new MessageResult("subscribed", connectionId, new
            {
                subscribed = added,
                invalid = invalid.Count > 0 ? invalid : null,
                active = subs.ToList()
            }));
    }

    private Task<IActionResult> HandleUnsubscribe(string connectionId, List<string> channels)
    {
        if (!_subscriptions.TryGetValue(connectionId, out var subs))
            return Task.FromResult<IActionResult>(new MessageResult("unsubscribed", connectionId, new
            {
                unsubscribed = new List<string>(),
                @protected = (object?)null,
                active = new List<string>()
            }));

        var removed = new List<string>();
        var protectedChannels = new List<string>();

        foreach (var ch in channels)
        {
            if (DefaultChannels.Contains(ch))
            {
                protectedChannels.Add(ch);
                continue;
            }
            if (subs.Remove(ch))
                removed.Add(ch);
        }

        return Task.FromResult<IActionResult>(
            new MessageResult("unsubscribed", connectionId, new
            {
                unsubscribed = removed,
                @protected = protectedChannels.Count > 0 ? protectedChannels : (object?)null,
                active = subs.ToList()
            }));
    }
}
