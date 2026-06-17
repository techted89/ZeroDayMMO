using System.Collections.Concurrent;

namespace ZeroDayMMO.Server.Services;

public class ConnectionRegistry
{
    private readonly ConcurrentDictionary<string, ConnectionEntry> _connections = new();
    private readonly ConcurrentDictionary<string, int> _ipCounts = new();
    private readonly SemaphoreSlim _semaphore = new(1, 1);
    private readonly int _maxConnections;
    private readonly int _maxPerIp;
    private readonly int _rateLimitPerSecond;

    public ConnectionRegistry(Config.ServerConfig config)
    {
        _maxConnections = config.MaxConnections;
        _maxPerIp = config.MaxConnectionsPerIp;
        _rateLimitPerSecond = config.RateLimitPerSecond;
    }

    public int ActiveConnections => _connections.Count;

    public bool TryRegister(string connectionId, string ip)
    {
        _semaphore.Wait();
        try
        {
            if (_connections.Count >= _maxConnections)
                return false;

            _ipCounts.AddOrUpdate(ip, 1, (_, count) => count + 1);
            if (_ipCounts[ip] > _maxPerIp)
            {
                _ipCounts.AddOrUpdate(ip, 0, (_, count) => count - 1);
                return false;
            }

            var entry = new ConnectionEntry { Ip = ip };
            _connections[connectionId] = entry;
            return true;
        }
        finally
        {
            _semaphore.Release();
        }
    }

    public void Unregister(string connectionId)
    {
        if (_connections.TryRemove(connectionId, out var entry))
        {
            _semaphore.Wait();
            try
            {
                _ipCounts.AddOrUpdate(entry.Ip, 0, (_, count) => Math.Max(0, count - 1));
                if (_ipCounts.TryGetValue(entry.Ip, out var count) && count <= 0)
                    _ipCounts.TryRemove(entry.Ip, out _);
            }
            finally
            {
                _semaphore.Release();
            }
        }
    }

    public bool TryConsumeRateLimit(string connectionId)
    {
        if (!_connections.TryGetValue(connectionId, out var entry))
            return false;

        var now = DateTime.UtcNow;
        var windowStart = now.AddSeconds(-1);

        lock (entry.Lock)
        {
            entry.Timestamps.RemoveAll(t => t < windowStart);

            if (entry.Timestamps.Count >= _rateLimitPerSecond)
                return false;

            entry.Timestamps.Add(now);
            return true;
        }
    }

    private class ConnectionEntry
    {
        public string Ip { get; set; } = string.Empty;
        public object Lock { get; } = new();
        public List<DateTime> Timestamps { get; } = new();
    }
}
