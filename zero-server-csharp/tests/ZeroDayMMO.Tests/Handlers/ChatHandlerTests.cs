using System.Text.Json;
using Moq;
using ZeroDayMMO.Server.Handlers;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;
using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Tests.Handlers;

public class ChatHandlerTests
{
    private readonly Mock<IPlayerService> _playerServiceMock;
    private readonly ChatHandler _handler;

    public ChatHandlerTests()
    {
        _playerServiceMock = new Mock<IPlayerService>(MockBehavior.Strict);
        var wsManager = new WebSocketConnectionManager();
        _handler = new ChatHandler(wsManager, _playerServiceMock.Object);
    }

    [Fact]
    public async Task Handle_ChatMessage_BroadcastsToAll()
    {
        var payload = JsonSerializer.Deserialize<JsonElement>(@"{
            ""playerId"": ""p1"",
            ""message"": ""Hello, world!""
        }");

        var player = new PlayerData
        {
            Id = "p1",
            Username = "TestUser",
            DisplayName = "TestUser"
        };

        _playerServiceMock
            .Setup(ps => ps.GetPlayer("p1"))
            .Returns(player);

        var result = await _handler.HandleAsync("conn-1", payload, "req-1");

        var broadcast = Assert.IsType<BroadcastResult>(result);
        Assert.Equal("chat_receive", broadcast.Type);
    }
}
