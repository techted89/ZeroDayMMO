package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.reqStr
import com.zeroday.handler.str
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import com.zeroday.zdscript.NexusScriptEngine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Routes messages to the in-game Nexus scripting engine. Scripts can be
 * run inline, saved, listed, deleted, or syntax-validated without execution.
 */
class NexusHandler(
    private val scriptEngine: NexusScriptEngine
) : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.NEXUS_RUN,
        RequestTypes.NEXUS_SAVE,
        RequestTypes.NEXUS_LIST,
        RequestTypes.NEXUS_DELETE,
        RequestTypes.NEXUS_VALIDATE
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.NEXUS_RUN -> run(ctx, playerId, payload)
            RequestTypes.NEXUS_SAVE -> save(ctx, playerId, payload)
            RequestTypes.NEXUS_LIST -> listScripts(ctx, playerId)
            RequestTypes.NEXUS_DELETE -> delete(ctx, playerId, payload)
            RequestTypes.NEXUS_VALIDATE -> validate(payload)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported nexus request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun run(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val code = payload.str("code")
        val scriptId = payload.str("script_id")
        if (code == null && scriptId == null) {
            return HandlerResult.Err(ServerError.fromMessage("Provide 'code' or 'script_id'", ErrorCategory.VALIDATION))
        }
        val result = if (code != null) {
            ctx.services.hackToLearnService.runCode(playerId, code)
        } else {
            val argsJson = payload["args"]?.jsonObject
            val args: Map<String, String> = argsJson?.entries?.associate { (k, v) -> k to (v.jsonPrimitive.content) } ?: emptyMap()
            ctx.services.hackToLearnService.executeScript(playerId, scriptId!!, args)
        }
        return result.fold(
            onSuccess = { scriptResult ->
                HandlerResult.Ok(
                    type = ResponseTypes.NEXUS_RESULT,
                    payload = mapOf(
                        "output" to scriptResult.output,
                        "console" to scriptResult.console,
                        "variables" to scriptResult.variables,
                        "success" to true
                    )
                )
            },
            onFailure = { e ->
                HandlerResult.Err(
                    ServerError.fromMessage(e.message ?: "Execution failed", ErrorCategory.INTERNAL),
                    type = ResponseTypes.NEXUS_ERROR
                )
            }
        )
    }

    private suspend fun save(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val name = payload.str("name")
            ?: return HandlerResult.Err(ServerError.fromMessage("Script name required", ErrorCategory.VALIDATION))
        val code = payload.str("code")
            ?: return HandlerResult.Err(ServerError.fromMessage("Script code required", ErrorCategory.VALIDATION))
        val desc = payload.str("description") ?: ""
        return ctx.services.hackToLearnService.saveScript(playerId, name, code, desc).fold(
            onSuccess = { script -> HandlerResult.Ok(ResponseTypes.SCRIPT_SAVED, mapOf("script" to script)) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Save failed", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun listScripts(ctx: HandlerContext, playerId: String): HandlerResult {
        val scripts = ctx.services.hackToLearnService.getPlayerScripts(playerId)
        return HandlerResult.Ok(ResponseTypes.SCRIPT_LIST, mapOf("scripts" to scripts))
    }

    private suspend fun delete(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val scriptId = payload.str("script_id")
            ?: return HandlerResult.Err(ServerError.fromMessage("Script ID required", ErrorCategory.VALIDATION))
        return ctx.services.hackToLearnService.deleteScript(playerId, scriptId).fold(
            onSuccess = { HandlerResult.Ok(ResponseTypes.SCRIPT_DELETED, mapOf("message" to "Script deleted")) },
            onFailure = { error -> HandlerResult.Err(ServerError.fromMessage(error.message ?: "Delete failed", ErrorCategory.INTERNAL)) }
        )
    }

    private suspend fun validate(payload: JsonObject): HandlerResult {
        val code = payload.str("code")
            ?: return HandlerResult.Err(ServerError.fromMessage("Code required", ErrorCategory.VALIDATION))
        val result = scriptEngine.validate(code)
        return HandlerResult.Ok(
            type = ResponseTypes.VALIDATION_RESULT,
            payload = mapOf(
                "valid" to result.valid,
                "errors" to result.errors
            )
        )
    }
}
