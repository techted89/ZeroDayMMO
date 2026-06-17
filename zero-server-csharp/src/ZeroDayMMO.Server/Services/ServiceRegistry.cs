using ZeroDayMMO.Server.Config;

namespace ZeroDayMMO.Server.Services;

public class ServiceRegistry
{
    private readonly Dictionary<Type, object> _services = new();

    public void Register<T>(T service) where T : class
    {
        _services[typeof(T)] = service;
    }

    public T Get<T>() where T : class
    {
        return (T)_services[typeof(T)];
    }

    public static ServiceRegistry Minimal(IPlayerService playerService, ServerConfig config)
    {
        var registry = new ServiceRegistry();
        registry.Register(playerService);
        registry.Register(config);
        return registry;
    }
}
