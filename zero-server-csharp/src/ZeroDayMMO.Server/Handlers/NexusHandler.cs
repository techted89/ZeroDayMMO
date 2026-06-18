using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class NexusHandler : IHandler
{
    public string MessageType => "nexus";

    public NexusHandler()
    {
    }

    public Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Missing payload" }));

        var action = payload.Value.Str("action");
        return action switch
        {
            "run" => HandleRun(connectionId, payload.Value),
            "save" => HandleSave(connectionId, payload.Value),
            "list" => HandleList(connectionId, payload.Value),
            "delete" => HandleDelete(connectionId, payload.Value),
            "validate" => HandleValidate(connectionId, payload.Value),
            _ => Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = $"Unknown nexus action: {action}" }))
        };
    }

    private static Task<IActionResult> HandleRun(string connectionId, JsonElement payload)
    {
        var code = payload.Str("code");
        var scriptId = payload.Str("script_id");

        if (string.IsNullOrEmpty(code) && string.IsNullOrEmpty(scriptId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Provide 'code' or 'script_id'" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("nexus_result", connectionId, new
            {
                output = "Script executed (placeholder)",
                console = "",
                variables = new Dictionary<string, object>(),
                success = true
            }));
    }

    private static Task<IActionResult> HandleSave(string connectionId, JsonElement payload)
    {
        var name = payload.Str("name");
        var code = payload.Str("code");

        if (string.IsNullOrEmpty(name))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Script name required" }));
        if (string.IsNullOrEmpty(code))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Script code required" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("script_saved", connectionId, new
            {
                script = new
                {
                    id = Guid.NewGuid().ToString(),
                    name,
                    description = payload.Str("description") ?? ""
                }
            }));
    }

    private static Task<IActionResult> HandleList(string connectionId, JsonElement payload)
    {
        return Task.FromResult<IActionResult>(
            new MessageResult("script_list", connectionId, new
            {
                scripts = Array.Empty<object>()
            }));
    }

    private static Task<IActionResult> HandleDelete(string connectionId, JsonElement payload)
    {
        var scriptId = payload.Str("script_id");
        if (string.IsNullOrEmpty(scriptId))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Script ID required" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("script_deleted", connectionId, new { message = "Script deleted" }));
    }

    private static Task<IActionResult> HandleValidate(string connectionId, JsonElement payload)
    {
        var code = payload.Str("code");
        if (string.IsNullOrEmpty(code))
            return Task.FromResult<IActionResult>(new MessageResult("error", connectionId, new { message = "Code required" }));

        return Task.FromResult<IActionResult>(
            new MessageResult("validation_result", connectionId, new
            {
                valid = true,
                errors = Array.Empty<string>()
            }));
    }
}
