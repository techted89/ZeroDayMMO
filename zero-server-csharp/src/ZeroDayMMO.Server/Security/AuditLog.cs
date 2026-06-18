using Microsoft.Extensions.Logging;

namespace ZeroDayMMO.Server.Security;

public static class AuditLog
{
    private static ILogger? _logger;

    public static void Initialize(ILogger logger)
    {
        _logger = logger;
    }

    public static void Log(string eventType, string playerName, string ip, string details)
    {
        var message = $"event={eventType} player={playerName ?? "-"} ip={ip ?? "-"} extra={details}";

        if (_logger is not null)
        {
            _logger.LogInformation("{Message}", message);
        }
        else
        {
            Console.WriteLine($"[AUDIT] {message}");
        }
    }
}
