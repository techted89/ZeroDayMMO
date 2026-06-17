namespace ZeroDayMMO.Shared;

public interface IActionResult
{
    string Type { get; }
    string? ConnectionId { get; }
    object? Payload { get; }
}

public class MessageResult : IActionResult
{
    public string Type { get; }
    public string? ConnectionId { get; }
    public object? Payload { get; }

    public MessageResult(string type, string connectionId, object? payload = null)
    {
        Type = type;
        ConnectionId = connectionId;
        Payload = payload;
    }
}

public class BroadcastResult : IActionResult
{
    public string Type { get; }
    public string? ConnectionId => null;
    public object? Payload { get; }
    public string? ExcludeConnectionId { get; }

    public BroadcastResult(string type, object? payload = null, string? excludeConnectionId = null)
    {
        Type = type;
        Payload = payload;
        ExcludeConnectionId = excludeConnectionId;
    }
}

public class EmptyResult : IActionResult
{
    public string Type => "empty";
    public string? ConnectionId => null;
    public object? Payload => null;
}
