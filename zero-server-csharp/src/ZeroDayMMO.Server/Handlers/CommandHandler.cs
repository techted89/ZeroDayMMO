using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class CommandHandler : IHandler
{
    private readonly CommandService _commandService;

    public string MessageType => "command";

    public CommandHandler(CommandService commandService)
    {
        _commandService = commandService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return new MessageResult("error", connectionId, new { message = "Missing payload" });

        string input;
        try
        {
            input = payload.Value.ReqStr("input");
        }
        catch
        {
            return new MessageResult("error", connectionId, new { message = "No command input" });
        }

        var result = await _commandService.ExecuteCommand(connectionId, input);

        return new MessageResult("command_result", connectionId, new
        {
            command = result.Command,
            output = result.Output,
            status = result.Status.ToString()
        });
    }
}
