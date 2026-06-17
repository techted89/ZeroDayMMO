using System.Text.Json;
using ZeroDayMMO.Shared.Protocol;

namespace ZeroDayMMO.Tests.Protocol;

public class MessageProtocolTests
{
    private const string ValidJson =
        """
        {
            "type": "login",
            "payload": {
                "username": "TestUser",
                "password": "secret"
            },
            "request_id": "abc-123",
            "timestamp": 1717500000000
        }
        """;

    private static readonly JsonSerializerOptions SnakeCase = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower
    };

    [Fact]
    public void Parse_ValidJson_ReturnsGameMessage()
    {
        var msg = MessageProtocol.Parse(ValidJson);

        Assert.NotNull(msg);
        Assert.Equal("login", msg.Type);
        Assert.Equal("abc-123", msg.RequestId);
        Assert.Equal(1717500000000L, msg.Timestamp);

        Assert.NotNull(msg.Payload);
        var login = JsonSerializer.Deserialize<LoginRequest>(msg.Payload.Value.GetRawText(), SnakeCase);
        Assert.NotNull(login);
        Assert.Equal("TestUser", login.Username);
        Assert.Equal("secret", login.Password);
    }

    [Fact]
    public void Parse_InvalidJson_ThrowsException()
    {
        Assert.Throws<JsonException>(() => MessageProtocol.Parse("not valid json"));
    }

    [Fact]
    public void Create_ReturnsCorrectJson()
    {
        var msg = MessageProtocol.Create(MessageProtocol.LoginSuccess, new LoginResponse
        {
            Success = true,
            Token = "tok-xyz",
            Message = "Welcome!"
        }, requestId: "req-1");

        Assert.Equal(MessageProtocol.LoginSuccess, msg.Type);
        Assert.Equal("req-1", msg.RequestId);

        var json = MessageProtocol.Serialize(msg);
        var parsed = MessageProtocol.Parse(json);

        Assert.Equal(MessageProtocol.LoginSuccess, parsed.Type);
        Assert.Equal("req-1", parsed.RequestId);
    }

    [Fact]
    public void Serialize_Deserialize_RoundTrip()
    {
        var original = MessageProtocol.Create(MessageProtocol.ChatReceive, new ChatReceived
        {
            Sender = "TestUser",
            Message = "Hello",
            Channel = "general",
            Timestamp = 1717500000000L
        }, requestId: "rt-1");

        var json = MessageProtocol.Serialize(original);
        var deserialized = MessageProtocol.Parse(json);

        Assert.Equal(original.Type, deserialized.Type);
        Assert.Equal(original.RequestId, deserialized.RequestId);
        Assert.NotNull(deserialized.Payload);

        var chat = JsonSerializer.Deserialize<ChatReceived>(deserialized.Payload.Value.GetRawText(), SnakeCase);
        Assert.NotNull(chat);
        Assert.Equal("TestUser", chat.Sender);
        Assert.Equal("Hello", chat.Message);
        Assert.Equal("general", chat.Channel);
    }
}
