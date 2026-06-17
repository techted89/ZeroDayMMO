using ZeroDayMMO.Server.Config;
using ZeroDayMMO.Server.Services;

namespace ZeroDayMMO.Tests.Services;

public class ConnectionRegistryTests
{
    private const string TestIp = "192.168.1.1";
    private const string TestConnId = "conn-1";

    private static ConnectionRegistry CreateRegistry()
    {
        return new ConnectionRegistry(new ServerConfig());
    }

    [Fact]
    public void TryRegister_UnderLimit_ReturnsTrue()
    {
        var registry = CreateRegistry();

        var result = registry.TryRegister(TestConnId, TestIp);

        Assert.True(result);
    }

    [Fact]
    public void TryRegister_OverGlobalLimit_ReturnsFalse()
    {
        var registry = CreateRegistry();

        for (int i = 0; i < 2000; i++)
        {
            registry.TryRegister($"conn-{i}", $"10.0.0.{i % 255}");
        }

        var result = registry.TryRegister("conn-over", "10.0.0.99");

        Assert.False(result);
    }

    [Fact]
    public void TryRegister_OverPerIpLimit_ReturnsFalse()
    {
        var registry = CreateRegistry();

        for (int i = 0; i < 8; i++)
        {
            registry.TryRegister($"conn-{i}", TestIp);
        }

        var result = registry.TryRegister("conn-over", TestIp);

        Assert.False(result);
    }

    [Fact]
    public void Unregister_FreesSlot()
    {
        var registry = CreateRegistry();

        Assert.True(registry.TryRegister(TestConnId, TestIp));

        registry.Unregister(TestConnId);

        var canReRegister = registry.TryRegister("conn-2", TestIp);
        Assert.True(canReRegister);
    }

    [Fact]
    public void TryConsumeRateLimit_UnderLimit_ReturnsTrue()
    {
        var registry = CreateRegistry();
        registry.TryRegister(TestConnId, TestIp);

        for (int i = 0; i < 29; i++)
        {
            Assert.True(registry.TryConsumeRateLimit(TestConnId));
        }
    }

    [Fact]
    public void TryConsumeRateLimit_OverLimit_ReturnsFalse()
    {
        var registry = CreateRegistry();
        registry.TryRegister(TestConnId, TestIp);

        for (int i = 0; i < 30; i++)
        {
            registry.TryConsumeRateLimit(TestConnId);
        }

        var result = registry.TryConsumeRateLimit(TestConnId);

        Assert.False(result);
    }
}
