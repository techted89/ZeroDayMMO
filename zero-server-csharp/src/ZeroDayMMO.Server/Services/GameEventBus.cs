using System.Collections.Concurrent;
using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public class GameEventBus : IGameEventBus
{
    private readonly ConcurrentDictionary<Type, List<Delegate>> _listeners = new();

    public void Subscribe<T>(Action<T> handler) where T : class
    {
        var listeners = _listeners.GetOrAdd(typeof(T), _ => new List<Delegate>());
        lock (listeners)
        {
            listeners.Add(handler);
        }
    }

    public void Unsubscribe<T>(Action<T> handler) where T : class
    {
        if (_listeners.TryGetValue(typeof(T), out var listeners))
        {
            lock (listeners)
            {
                listeners.Remove(handler);
            }
        }
    }

    public Task Emit(GameEvent gameEvent)
    {
        Publish(gameEvent);
        return Task.CompletedTask;
    }

    public void Publish<T>(T eventData) where T : class
    {
        if (!_listeners.TryGetValue(typeof(T), out var listeners))
            return;

        List<Delegate> snapshot;
        lock (listeners)
        {
            snapshot = new List<Delegate>(listeners);
        }

        foreach (var handler in snapshot)
        {
            try
            {
                ((Action<T>)handler)(eventData);
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"GameEventBus: Listener threw for {typeof(T).Name}: {ex.Message}");
            }
        }
    }
}
