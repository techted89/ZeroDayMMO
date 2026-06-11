package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.reqStr
import com.zeroday.model.TaskInstance
import com.zeroday.model.toMap
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import kotlinx.serialization.json.JsonObject

/**
 * Contract / task lifecycle: listing available contracts, accepting them
 * (turning an EnhancedContract into a TaskInstance on the player), and
 * marking a task as completed.
 */
class TaskHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.GET_TASKS,
        RequestTypes.ACCEPT_TASK,
        RequestTypes.COMPLETE_TASK
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.GET_TASKS -> getTasks(ctx, playerId)
            RequestTypes.ACCEPT_TASK -> acceptTask(ctx, playerId, payload)
            RequestTypes.COMPLETE_TASK -> completeTask(ctx, playerId, payload)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported task request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun getTasks(ctx: HandlerContext, playerId: String): HandlerResult {
        val tasks = ctx.services.taskService.getAvailableTasks(playerId)
        return HandlerResult.Ok(ResponseTypes.TASKS, mapOf("available_tasks" to tasks))
    }

    private suspend fun acceptTask(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val taskId = try { payload.reqStr("task_instance_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Task instance ID required", ErrorCategory.VALIDATION))
        }
        return ctx.services.taskService.acceptTask(playerId, taskId).fold(
            onSuccess = { task -> HandlerResult.Ok(ResponseTypes.TASK_ACCEPTED, mapOf("task" to task)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not accept task", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun completeTask(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val taskId = try { payload.reqStr("task_instance_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Task instance ID required", ErrorCategory.VALIDATION))
        }
        return ctx.services.taskService.completeTask(playerId, taskId).fold(
            onSuccess = { task ->
                HandlerResult.Ok(
                    type = ResponseTypes.TASK_COMPLETED,
                    payload = mapOf(
                        "task" to task.toMap(),
                        "player" to ctx.services.playerService.getSnapshot(playerId)?.toMap()
                    )
                )
            },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not complete task", ErrorCategory.INTERNAL)) }
        )
    }
}
