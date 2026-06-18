using System.Text.Json;
using Moq;
using ZeroDayMMO.Server.Handlers;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;
using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Tests.Handlers;

public class LoginHandlerTests
{
    private readonly Mock<IPlayerService> _playerServiceMock;
    private readonly LoginHandler _handler;

    public LoginHandlerTests()
    {
        _playerServiceMock = new Mock<IPlayerService>(MockBehavior.Strict);
        _handler = new LoginHandler(_playerServiceMock.Object);
    }

    [Fact]
    public async Task Handle_ValidLogin_ReturnsLoginSuccess()
    {
        var player = new Player
        {
            Id = "p1",
            Username = "TestUser",
            PasswordHash = "hash",
            Level = 5
        };

        _playerServiceMock
            .Setup(ps => ps.Authenticate("TestUser", "secret"))
            .Returns(player);

        var payload = JsonSerializer.Deserialize<JsonElement>(@"{
            ""username"": ""TestUser"",
            ""password"": ""secret""
        }");

        var result = await _handler.HandleAsync("conn-1", payload, null);

        var msgResult = Assert.IsType<MessageResult>(result);
        Assert.Equal("login_success", msgResult.Type);
        Assert.Equal("conn-1", msgResult.ConnectionId);
        Assert.NotNull(msgResult.Payload);
    }

    [Fact]
    public async Task Handle_InvalidPassword_ReturnsLoginFailure()
    {
        _playerServiceMock
            .Setup(ps => ps.Authenticate("TestUser", "wrong"))
            .Returns((Player?)null);

        var payload = JsonSerializer.Deserialize<JsonElement>(@"{
            ""username"": ""TestUser"",
            ""password"": ""wrong""
        }");

        var result = await _handler.HandleAsync("conn-1", payload, null);

        var msgResult = Assert.IsType<MessageResult>(result);
        Assert.Equal("login_failure", msgResult.Type);
        Assert.Equal("conn-1", msgResult.ConnectionId);
    }

    [Fact]
    public async Task Handle_MissingCredentials_ReturnsError()
    {
        var payload = JsonSerializer.Deserialize<JsonElement>(@"{}");

        var result = await _handler.HandleAsync("conn-1", payload, null);

        var msgResult = Assert.IsType<MessageResult>(result);
        Assert.Equal("login_failure", msgResult.Type);
        Assert.Equal("conn-1", msgResult.ConnectionId);
    }
}
