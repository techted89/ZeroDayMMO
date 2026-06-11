package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.reqStr
import com.zeroday.model.SkillTree
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import com.zeroday.service.SkillService
import kotlinx.serialization.json.JsonObject

class SkillHandler(
    private val skillService: SkillService
) : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.GET_SKILL_TREE,
        RequestTypes.UNLOCK_SKILL
    )

    override suspend fun handle(ctx: HandlerContext, type: String, payload: JsonObject): HandlerResult {
        val playerId = ctx.authenticatedPlayerId()
            ?: return HandlerResult.Err(ServerError.fromMessage("Authentication required", ErrorCategory.AUTHENTICATION))
        return when (type) {
            RequestTypes.GET_SKILL_TREE -> tree(ctx, playerId)
            RequestTypes.UNLOCK_SKILL -> unlock(ctx, playerId, payload)
            else -> HandlerResult.Err(ServerError.fromMessage("Unsupported skill request: $type", ErrorCategory.VALIDATION))
        }
    }

    private suspend fun tree(ctx: HandlerContext, playerId: String): HandlerResult {
        val player = ctx.services.playerService.getPlayer(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        val m = skillService.effectiveMultipliers(player)
        val grouped = SkillTree.values().associateWith { skillService.byTree(it) }
        val trees = grouped.map { (tree, defs) ->
            mapOf(
                "tree" to tree.name,
                "skills" to defs.map { def ->
                    val unlocked = player.unlockedSkills.contains(def.id)
                    val canUnlock = !unlocked &&
                        player.skillPoints >= def.cost &&
                        def.prerequisites.all { player.unlockedSkills.contains(it) }
                    mapOf(
                        "id" to def.id,
                        "name" to def.name,
                        "description" to def.description,
                        "tier" to def.tier,
                        "cost" to def.cost,
                        "prerequisites" to def.prerequisites,
                        "unlocked" to unlocked,
                        "can_unlock" to canUnlock,
                        "effects" to def.effects.map { effect ->
                            mapOf("kind" to effect.kind, "params" to effectParams(effect))
                        }
                    )
                }
            )
        }
        return HandlerResult.Ok(
            type = ResponseTypes.SKILL_TREE,
            payload = mapOf(
                "trees" to trees,
                "skill_points" to player.skillPoints,
                "unlocked_count" to player.unlockedSkills.size,
                "multipliers" to mapOf(
                    "credits_earned" to m.creditsEarned,
                    "xp_earned" to m.xpEarned,
                    "resource_cost" to m.resourceCost,
                    "regen_rate" to m.regenRate,
                    "bonus_cpu" to m.bonusCpu,
                    "bonus_ram" to m.bonusRam,
                    "bonus_bandwidth" to m.bonusBandwidth
                )
            )
        )
    }

    private suspend fun unlock(ctx: HandlerContext, playerId: String, payload: JsonObject): HandlerResult {
        val skillId = try { payload.reqStr("skill_id") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Skill ID required", ErrorCategory.VALIDATION))
        }
        val player = ctx.services.playerService.getPlayer(playerId)
            ?: return HandlerResult.Err(ServerError.fromMessage("Player not found", ErrorCategory.NOT_FOUND))
        return when (val result = skillService.unlock(player, skillId)) {
            is SkillService.UnlockResult.Success -> HandlerResult.Ok(
                type = ResponseTypes.SKILL_UNLOCKED,
                payload = mapOf(
                    "skill_id" to result.skill.id,
                    "name" to result.skill.name,
                    "skill_points_remaining" to player.skillPoints
                )
            )
            is SkillService.UnlockResult.AlreadyOwned -> HandlerResult.Ok(
                type = ResponseTypes.SKILL_UNLOCKED,
                payload = mapOf(
                    "skill_id" to result.skill.id,
                    "name" to result.skill.name,
                    "already_owned" to true,
                    "skill_points_remaining" to player.skillPoints
                )
            )
            is SkillService.UnlockResult.InsufficientPoints -> HandlerResult.Err(
                ServerError.fromMessage(
                    "Need ${result.cost} skill points (have ${result.available})",
                    ErrorCategory.VALIDATION
                )
            )
            is SkillService.UnlockResult.MissingPrereqs -> HandlerResult.Err(
                ServerError.fromMessage(
                    "Missing prerequisite skills: ${result.missing.joinToString()}",
                    ErrorCategory.VALIDATION
                )
            )
            is SkillService.UnlockResult.Unknown -> HandlerResult.Err(
                ServerError.fromMessage("Unknown skill: ${result.id}", ErrorCategory.NOT_FOUND)
            )
        }
    }

    private suspend fun effectParams(effect: com.zeroday.model.SkillEffect): Map<String, Any?> = when (effect) {
        is com.zeroday.model.SkillEffect.MultiplyCreditsEarned -> mapOf("factor" to effect.factor)
        is com.zeroday.model.SkillEffect.MultiplyXpEarned -> mapOf("factor" to effect.factor)
        is com.zeroday.model.SkillEffect.AddMaxResource -> mapOf("cpu" to effect.cpu, "ram" to effect.ram, "bandwidth" to effect.bandwidth)
        is com.zeroday.model.SkillEffect.ReduceResourceCost -> mapOf("factor" to effect.factor)
        is com.zeroday.model.SkillEffect.IncreaseRegenRate -> mapOf("factor" to effect.factor)
    }
}
