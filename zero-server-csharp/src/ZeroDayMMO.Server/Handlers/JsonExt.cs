using System.Text.Json;

namespace ZeroDayMMO.Server.Handlers;

public static class JsonElementExtensions
{
    public static string? Str(this JsonElement element, string propertyName)
    {
        return element.TryGetProperty(propertyName, out var prop) ? prop.GetString() : null;
    }

    public static string ReqStr(this JsonElement element, string propertyName)
    {
        if (!element.TryGetProperty(propertyName, out var prop))
            throw new KeyNotFoundException($"Missing required field '{propertyName}'");
        return prop.GetString() ?? throw new InvalidOperationException($"Field '{propertyName}' is not a string");
    }

    public static int? Int(this JsonElement element, string propertyName)
    {
        if (!element.TryGetProperty(propertyName, out var prop))
            return null;
        return prop.ValueKind == JsonValueKind.Number ? prop.GetInt32() : null;
    }

    public static long? Long(this JsonElement element, string propertyName)
    {
        if (!element.TryGetProperty(propertyName, out var prop))
            return null;
        return prop.ValueKind == JsonValueKind.Number ? prop.GetInt64() : null;
    }

    public static bool? Bool(this JsonElement element, string propertyName)
    {
        if (!element.TryGetProperty(propertyName, out var prop))
            return null;
        return prop.ValueKind == JsonValueKind.True || prop.ValueKind == JsonValueKind.False ? prop.GetBoolean() : null;
    }
}
