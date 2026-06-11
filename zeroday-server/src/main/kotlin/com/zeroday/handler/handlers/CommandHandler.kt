package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.reqStr
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import kotlinx.serialization.json.JsonObject

/**
 * Thin wrapper around [com.zeroday.service.CommandService] so the WebSocket
 * layer can route a single "command" message type to the in-game terminal
 * interpreter.
 */
class CommandHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(RequestTypes.COMMAND)

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        val input = try {
            payload.reqStr("input")
        } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("No command input", ErrorCategory.VALIDATION))
        }
        val result = ctx.services.commandService.executeCommand(playerId, input)
        return HandlerResult.Ok(
            type = ResponseTypes.COMMAND_RESULT,
            payload = mapOf(
                "command" to result.command,
                "output" to result.output,
                "status" to result.status.name
            )
        )
    }
}
