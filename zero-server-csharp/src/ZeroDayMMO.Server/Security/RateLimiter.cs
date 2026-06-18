using System.Collections.Concurrent;

namespace ZeroDayMMO.Server.Security;

public interface IRateLimiterStorage
{
    int HitAndCount(string key, long now, long windowMs);
    void Reset(string key);
}

public class RateLimiter
{
    private readonly IRateLimiterStorage _storage;

    public RateLimiter() : this(new InMemoryRateLimiterStorage()) { }

    public RateLimiter(IRateLimiterStorage storage)
    {
        _storage = storage;
    }

    public bool TryConsume(string key, int limit, TimeSpan window)
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var count = _storage.HitAndCount(key, now, (long)window.TotalMilliseconds);
        return count <= limit;
    }

    public void Reset(string key) => _storage.Reset(key);
}

public class InMemoryRateLimiterStorage : IRateLimiterStorage
{
    private readonly ConcurrentDictionary<string, Bucket> _buckets = new();

    public int HitAndCount(string key, long now, long windowMs)
    {
        var bucket = _buckets.AddOrUpdate(key,
            _ => new Bucket { WindowStart = now, Count = 1 },
            (_, current) =>
            {
                if (now - current.WindowStart >= windowMs)
                    return new Bucket { WindowStart = now, Count = 1 };
                var count = Interlocked.Increment(ref current.Count);
                return current;
            });
        return bucket.Count;
    }

    public void Reset(string key)
    {
        _buckets.TryRemove(key, out _);
    }

    private sealed class Bucket
    {
        internal long WindowStart;
        internal int Count;
    }
}
