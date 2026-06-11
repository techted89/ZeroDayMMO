package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.str
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import kotlinx.serialization.json.JsonObject

class BossHandler : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.BOSS_CREATE,
        RequestTypes.BOSS_JOIN,
        RequestTypes.BOSS_ACTION,
        RequestTypes.BOSS_STATUS,
        RequestTypes.BOSS_LIST_AVAILABLE
    )

    override val requiresAuth: Boolean = true

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.BOSS_CREATE -> createBossInstance(ctx, playerId, payload)
            RequestTypes.BOSS_JOIN -> joinBossInstance(ctx, playerId, payload)
            RequestTypes.BOSS_ACTION -> performAction(ctx, playerId, payload)
            RequestTypes.BOSS_STATUS -> getStatus(ctx, playerId, payload)
            RequestTypes.BOSS_LIST_AVAILABLE -> listAvailable(ctx, playerId, payload)
            else -> HandlerResult.Err(ServerError.fromMessage("Unknown boss request", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun createBossInstance(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val zoneId = payload.str("zoneId")
            ?: return HandlerResult.Err(ServerError.fromMessage("zoneId required", ErrorCategory.VALIDATION))
        val result = ctx.services.coopBossService.createInstance(zoneId, playerId)
        return result.fold(
            onSuccess = { instance ->
                HandlerResult.Ok(ResponseTypes.BOSS_INSTANCE_CREATED, ctx.services.coopBossService.serializeInstance(instance))
            },
            onFailure = { error ->
                HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not create instance", ErrorCategory.INTERNAL))
            }
        )
    }

    private suspend fun joinBossInstance(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val instanceId = payload.str("instanceId")
            ?: return HandlerResult.Err(ServerError.fromMessage("instanceId required", ErrorCategory.VALIDATION))
        val result = ctx.services.coopBossService.joinInstance(instanceId, playerId)
        return result.fold(
            onSuccess = { instance ->
                HandlerResult.Ok(ResponseTypes.BOSS_INSTANCE_JOINED, ctx.services.coopBossService.serializeInstance(instance))
            },
            onFailure = { error ->
                HandlerResult.Err(ServerError.fromMessage(error.message ?: "Could not join instance", ErrorCategory.VALIDATION))
            }
        )
    }

    private suspend fun performAction(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val instanceId = payload.str("instanceId")
            ?: return HandlerResult.Err(ServerError.fromMessage("instanceId required", ErrorCategory.VALIDATION))
        val action = payload.str("action")
            ?: return HandlerResult.Err(ServerError.fromMessage("action required (attack/scan/defend/heal)", ErrorCategory.VALIDATION))
        val result = ctx.services.coopBossService.performAction(instanceId, playerId, action)
        return result.fold(
            onSuccess = { actionResult ->
                HandlerResult.Ok(ResponseTypes.BOSS_ACTION_RESULT, mapOf(
                    "action" to actionResult.action.action,
                    "message" to actionResult.action.message,
                    "damage" to actionResult.action.damage,
                    "healAmount" to actionResult.action.healAmount,
                    "isCrit" to actionResult.action.isCrit,
                    "isDodged" to actionResult.action.isDodged,
                    "bossPhase" to actionResult.bossPhase.name,
                    "bossHp" to actionResult.bossHp,
                    "bossMaxHp" to actionResult.bossMaxHp,
                    "phaseChanged" to actionResult.phaseChanged,
                    "abilityName" to actionResult.abilityName,
                    "abilityDamage" to actionResult.abilityDamage,
                    "knockedOut" to (actionResult.knockedOut ?: ""),
                    "fightOver" to actionResult.fightOver,
                    "result" to (actionResult.result ?: "")
                ))
            },
            onFailure = { error ->
                HandlerResult.Err(ServerError.fromMessage(error.message ?: "Action failed", ErrorCategory.INTERNAL))
            }
        )
    }

    private suspend fun getStatus(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val instanceId = payload.str("instanceId")
        val data = if (instanceId != null) {
            val instance = ctx.services.coopBossService.getInstance(instanceId)
            if (instance != null) ctx.services.coopBossService.serializeInstance(instance) else null
        } else {
            val instance = ctx.services.coopBossService.getPlayerActiveInstance(playerId)
            if (instance != null) ctx.services.coopBossService.serializeInstance(instance) else null
        }
        return if (data != null) {
            HandlerResult.Ok(ResponseTypes.BOSS_STATUS, data)
        } else {
            HandlerResult.Err(ServerError.fromMessage("No active boss instance found", ErrorCategory.NOT_FOUND))
        }
    }

    private suspend fun listAvailable(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val zoneId = payload.str("zoneId")
            ?: return HandlerResult.Err(ServerError.fromMessage("zoneId required", ErrorCategory.VALIDATION))
        val instances = ctx.services.coopBossService.getAvailableInstances(zoneId)
        return HandlerResult.Ok(ResponseTypes.BOSS_AVAILABLE_INSTANCES, mapOf(
            "instances" to instances.map { ctx.services.coopBossService.serializeInstance(it) },
            "count" to instances.size
        ))
    }
}
