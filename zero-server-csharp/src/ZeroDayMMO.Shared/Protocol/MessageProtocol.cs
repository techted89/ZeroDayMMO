using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Protocol;

public static class MessageProtocol
{
    public const string Login = "login";
    public const string LoginSuccess = "login_success";
    public const string LoginFailure = "login_failure";
    public const string ChatSend = "chat_send";
    public const string ChatReceive = "chat_receive";
    public const string GameCommand = "game_command";
    public const string GameAction = "game_action";
    public const string Error = "error";
    public const string Heartbeat = "heartbeat";

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower
    };

    public static GameMessage Parse(string json)
    {
        return JsonSerializer.Deserialize<GameMessage>(json, JsonOptions)
               ?? throw new JsonException("Failed to deserialize GameMessage");
    }

    public static GameMessage Create<T>(string type, T? payload = default, string? requestId = null)
    {
        var element = payload is null
            ? JsonSerializer.SerializeToElement(new { }, JsonOptions)
            : JsonSerializer.SerializeToElement(payload, JsonOptions);

        return new GameMessage
        {
            Type = type,
            Payload = element,
            RequestId = requestId,
            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        };
    }

    public static string Serialize(GameMessage msg)
    {
        return JsonSerializer.Serialize(msg, JsonOptions);
    }
}

public class GameMessage
{
    public string Type { get; set; } = string.Empty;
    public JsonElement? Payload { get; set; }
    public string? RequestId { get; set; }
    public long Timestamp { get; set; }
}

public class LoginRequest
{
    public string Username { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}

public class LoginResponse
{
    public bool Success { get; set; }
    public string? Token { get; set; }
    public string? Message { get; set; }
}

public class ChatRequest
{
    public string Message { get; set; } = string.Empty;
    public string? Channel { get; set; }
}

public class ChatReceived
{
    public string Sender { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
    public string? Channel { get; set; }
    public long Timestamp { get; set; }
}

public class ErrorMessage
{
    public string Code { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
}
