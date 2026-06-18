namespace ZeroDayMMO.Server.Util;

public static class IdGenerator
{
    public static string NewId() => Guid.NewGuid().ToString();

    public static string ShortId() => Guid.NewGuid().ToString()[..8];

    public static string SessionId(string playerId) =>
        $"session_{playerId}_{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}";

    public static string TaskInstanceId() => $"task_{ShortId()}";

    public static string EventInstanceId() => $"evt_{ShortId()}";
}
