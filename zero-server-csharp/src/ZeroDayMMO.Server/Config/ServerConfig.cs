namespace ZeroDayMMO.Server.Config;

public class ServerConfig
{
    public int Port { get; set; } = GetEnvInt("PORT", 8080);
    public int MaxConnections { get; set; } = GetEnvInt("MAX_CONNECTIONS", 2000);
    public int MaxConnectionsPerIp { get; set; } = GetEnvInt("MAX_CONNECTIONS_PER_IP", 8);
    public int RateLimitPerSecond { get; set; } = GetEnvInt("RATE_LIMIT_PER_SECOND", 30);
    public int FrameSizeLimit { get; set; } = GetEnvInt("FRAME_SIZE_LIMIT", 64 * 1024);
    public int PlayerSaveIntervalSeconds { get; set; } = GetEnvInt("PLAYER_SAVE_INTERVAL_SECONDS", 60);
    public int ResourceRegenIntervalSeconds { get; set; } = GetEnvInt("RESOURCE_REGEN_INTERVAL_SECONDS", 5);

    private static int GetEnvInt(string key, int defaultValue)
    {
        var value = Environment.GetEnvironmentVariable(key);
        return int.TryParse(value, out var parsed) ? parsed : defaultValue;
    }
}
