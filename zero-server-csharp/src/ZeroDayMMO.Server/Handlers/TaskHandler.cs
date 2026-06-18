using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class TaskHandler : IHandler
{
    private readonly TaskService _taskService;
    private readonly IPlayerService _playerService;

    public string MessageType => "get_tasks";

    public TaskHandler(TaskService taskService, IPlayerService playerService)
    {
        _taskService = taskService;
        _playerService = playerService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return new MessageResult("error", connectionId, new { message = "Missing payload" });

        var type = payload.Value.Str("type") ?? "get_tasks";

        return type switch
        {
            "get_tasks" => await GetTasks(connectionId),
            "accept_task" => await AcceptTask(connectionId, payload.Value),
            "complete_task" => await CompleteTask(connectionId, payload.Value),
            _ => new MessageResult("error", connectionId, new { message = $"Unsupported task request: {type}" })
        };
    }

    private async Task<IActionResult> GetTasks(string connectionId)
    {
        var tasks = await _taskService.GetAvailableTasks(connectionId);
        return new MessageResult("tasks", connectionId, new { available_tasks = tasks });
    }

    private async Task<IActionResult> AcceptTask(string connectionId, JsonElement payload)
    {
        string taskInstanceId;
        try
        {
            taskInstanceId = payload.ReqStr("task_instance_id");
        }
        catch
        {
            return new MessageResult("error", connectionId, new { message = "Task instance ID required" });
        }

        var result = await _taskService.AcceptTask(connectionId, taskInstanceId);

        if (result.IsSuccess)
            return new MessageResult("task_accepted", connectionId, new { task = result.Value });

        return new MessageResult("error", connectionId, new { message = result.Error ?? "Could not accept task" });
    }

    private async Task<IActionResult> CompleteTask(string connectionId, JsonElement payload)
    {
        string taskInstanceId;
        try
        {
            taskInstanceId = payload.ReqStr("task_instance_id");
        }
        catch
        {
            return new MessageResult("error", connectionId, new { message = "Task instance ID required" });
        }

        var result = await _taskService.CompleteTask(connectionId, taskInstanceId);

        if (result.IsSuccess)
        {
            var snapshot = _playerService.GetSnapshot(connectionId);
            return new MessageResult("task_completed", connectionId, new
            {
                task = result.Value is null ? null : new Dictionary<string, object?>
                {
                    ["taskId"] = result.Value.TaskId,
                    ["instanceId"] = result.Value.InstanceId,
                    ["title"] = result.Value.Title,
                    ["status"] = result.Value.Status.ToString()
                },
                player = snapshot?.SnapshotToMap()
            });
        }

        return new MessageResult("error", connectionId, new { message = result.Error ?? "Could not complete task" });
    }
}
