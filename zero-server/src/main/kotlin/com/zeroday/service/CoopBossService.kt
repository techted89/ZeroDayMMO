package com.zeroday.service

import com.zeroday.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.random.Random

class CoopBossService(
    private val playerService: PlayerService,
    private val worldZoneService: WorldZoneService,
    private val gameEventBus: GameEventBus? = null
) {
    private val instances = mutableMapOf<String, BossInstance>()
    private val mutex = Mutex()
    private var cleanupJob: Job? = null
    private var timerCheckJob: Job? = null

    companion object {
        private const val MIN_PLAYERS = 2
        private const val MAX_PLAYERS = 3
        private const val INSTANCE_TIMEOUT_MS = 600_000L
        private const val TURN_TIMEOUT_MS = 30_000L
        private const val TIMER_CHECK_INTERVAL_MS = 5_000L
    }

    enum class BossPhase { PHASE_1, PHASE_2, PHASE_3 }
    enum class BossState { WAITING, IN_PROGRESS, COMPLETED, EXPIRED }
    enum class BossResult { VICTORY, DEFEAT, NO_SHOW }
    enum class StatusEffect { NONE, BURN, STUN, SHIELD }

    data class BossInstance(
        val id: String = UUID.randomUUID().toString(),
        val zoneId: String,
        val bossName: String,
        val bossLevel: Int,
        val bossPower: Int,
        val threatLevel: Int,
        var currentHp: Int,
        val participants: MutableList<BossParticipant> = mutableListOf(),
        var state: BossState = BossState.WAITING,
        var phase: BossPhase = BossPhase.PHASE_1,
        val createdAt: Long = System.currentTimeMillis(),
        var resolvedAt: Long = 0L,
        var result: BossResult? = null,
        var currentTurnPlayerIndex: Int = 0,
        var lastActionTime: Long = System.currentTimeMillis(),
        var turnNumber: Int = 0
    )

    data class BossParticipant(
        val playerId: String,
        val username: String,
        val playerLevel: Int,
        val playerPower: Int,
        var currentHp: Int,
        val maxHp: Int,
        var contributionScore: Int = 0,
        var survived: Boolean = true,
        var statusEffect: StatusEffect = StatusEffect.NONE,
        var statusDuration: Int = 0,
        var shieldActive: Boolean = false,
        var dodgeBuff: Float = 0f
    )

    data class PlayerBossAction(
        val action: String,
        val damage: Int = 0,
        val healAmount: Int = 0,
        val isCrit: Boolean = false,
        val isDodged: Boolean = false,
        val message: String = ""
    )

    data class BossActionResult(
        val action: PlayerBossAction,
        val bossPhase: BossPhase,
        val bossHp: Int,
        val bossMaxHp: Int,
        val phaseChanged: Boolean = false,
        val abilityName: String = "",
        val abilityDamage: Int = 0,
        val knockedOut: String? = null,
        val fightOver: Boolean = false,
        val result: String? = null
    )

    fun start(scope: CoroutineScope) {
        cleanupJob = scope.launch {
            while (isActive) {
                delay(30_000L)
                cleanupStaleInstances()
            }
        }
        timerCheckJob = scope.launch {
            while (isActive) {
                delay(TIMER_CHECK_INTERVAL_MS)
                checkTurnTimeouts()
            }
        }
    }

    fun stop() {
        cleanupJob?.cancel()
        timerCheckJob?.cancel()
    }

    suspend fun createInstance(zoneId: String, creatorPlayerId: String): Result<BossInstance> = mutex.withLock {
        val zone = worldZoneService.getZone(zoneId) ?: return@withLock Result.failure(Exception("Zone not found"))
        if (!zone.hasBoss) return@withLock Result.failure(Exception("No boss in this zone"))
        val creator = playerService.getPlayer(creatorPlayerId) ?: return@withLock Result.failure(Exception("Player not found"))

        val existingInstance = instances.values.firstOrNull {
            it.zoneId == zoneId && it.state == BossState.WAITING &&
            it.participants.any { p -> p.playerId == creatorPlayerId }
        }
        if (existingInstance != null) return@withLock Result.failure(Exception("You already have a pending instance in this zone"))

        val playerPower = creator.level * 10 + (creator.breakthroughMultiplier * 100).toInt()
        val bossPower = zone.bossLevel * 15 + zone.threatLevel
        val playerMaxHp = playerPower.coerceAtLeast(50)

        val instance = BossInstance(
            zoneId = zoneId,
            bossName = zone.bossName,
            bossLevel = zone.bossLevel,
            bossPower = bossPower,
            threatLevel = zone.threatLevel,
            currentHp = bossPower,
            participants = mutableListOf(
                BossParticipant(
                    playerId = creatorPlayerId,
                    username = creator.username,
                    playerLevel = creator.level,
                    playerPower = playerPower,
                    currentHp = playerMaxHp,
                    maxHp = playerMaxHp
                )
            )
        )
        instances[instance.id] = instance
        Result.success(instance)
    }

    suspend fun joinInstance(instanceId: String, playerId: String): Result<BossInstance> = mutex.withLock {
        val instance = instances[instanceId] ?: return@withLock Result.failure(Exception("Instance not found"))
        if (instance.state != BossState.WAITING) return@withLock Result.failure(Exception("Boss fight already started or ended"))
        if (instance.participants.size >= MAX_PLAYERS) return@withLock Result.failure(Exception("Instance is full"))
        if (instance.participants.any { it.playerId == playerId }) return@withLock Result.failure(Exception("Already in this instance"))

        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val playerPower = player.level * 10 + (player.breakthroughMultiplier * 100).toInt()
        val zone = worldZoneService.getZone(instance.zoneId)
        if (zone != null && player.level < zone.requiredLevel) {
            return@withLock Result.failure(Exception("Level ${zone.requiredLevel} required for this zone"))
        }

        val playerMaxHp = playerPower.coerceAtLeast(50)
        instance.participants.add(
            BossParticipant(
                playerId = playerId,
                username = player.username,
                playerLevel = player.level,
                playerPower = playerPower,
                currentHp = playerMaxHp,
                maxHp = playerMaxHp
            )
        )

        if (instance.participants.size >= MIN_PLAYERS) {
            instance.state = BossState.IN_PROGRESS
            instance.lastActionTime = System.currentTimeMillis()
        }

        Result.success(instance)
    }

    suspend fun performAction(instanceId: String, playerId: String, action: String): Result<BossActionResult> = mutex.withLock {
        val instance = instances[instanceId] ?: return@withLock Result.failure(Exception("Instance not found"))
        if (instance.state != BossState.IN_PROGRESS) return@withLock Result.failure(Exception("Fight is not in progress"))
        val participant = instance.participants.firstOrNull { it.playerId == playerId }
            ?: return@withLock Result.failure(Exception("Not a participant"))
        if (!participant.survived) return@withLock Result.failure(Exception("You are knocked out"))

        instance.lastActionTime = System.currentTimeMillis()
        instance.turnNumber++

        // Resolve player action
        var playerAction = resolvePlayerAction(participant, action, instance)
        if (playerAction.action == "invalid") {
            return@withLock Result.failure(Exception("Invalid action: use attack/scan/defend/heal"))
        }

        // Apply status effect burn damage
        if (participant.statusEffect == StatusEffect.BURN && participant.survived) {
            val burnDmg = (participant.maxHp * 0.08).toInt().coerceAtLeast(5)
            participant.currentHp = (participant.currentHp - burnDmg).coerceAtLeast(0)
            playerAction = playerAction.copy(message = playerAction.message + " | Burn: -$burnDmg HP")
            if (participant.currentHp <= 0) {
                participant.survived = false
            }
        }

        // Tick status durations
        if (participant.statusDuration > 0) {
            participant.statusDuration--
            if (participant.statusDuration <= 0) {
                participant.statusEffect = StatusEffect.NONE
            }
        }

        // Check boss defeat
        if (instance.currentHp <= 0) {
            resolveFight(instance, BossResult.VICTORY)
            return@withLock Result.success(BossActionResult(
                action = playerAction,
                bossPhase = instance.phase,
                bossHp = 0,
                bossMaxHp = instance.bossPower,
                fightOver = true,
                result = "victory"
            ))
        }

        // Check phase transition
        val newPhase = getPhaseForHp(instance.currentHp, instance.bossPower)
        val phaseChanged = newPhase != instance.phase
        instance.phase = newPhase

        // Boss turn — use phase-based ability
        val bossActionResult = executeBossTurn(instance)

        // Check all players knocked out
        if (instance.participants.none { it.survived }) {
            resolveFight(instance, BossResult.DEFEAT)
            return@withLock Result.success(BossActionResult(
                action = playerAction,
                bossPhase = instance.phase,
                bossHp = instance.currentHp.coerceAtLeast(0),
                bossMaxHp = instance.bossPower,
                phaseChanged = phaseChanged,
                abilityName = bossActionResult.second,
                abilityDamage = bossActionResult.first,
                fightOver = true,
                result = "defeat"
            ))
        }

        // Advance turn to next alive player
        advanceTurn(instance)

        Result.success(BossActionResult(
            action = playerAction,
            bossPhase = instance.phase,
            bossHp = instance.currentHp.coerceAtLeast(0),
            bossMaxHp = instance.bossPower,
            phaseChanged = phaseChanged,
            abilityName = bossActionResult.second,
            abilityDamage = bossActionResult.first,
            knockedOut = bossActionResult.third
        ))
    }

    private fun resolvePlayerAction(
        participant: BossParticipant,
        action: String,
        instance: BossInstance
    ): PlayerBossAction {
        return when (action.lowercase()) {
            "attack" -> {
                var dmg = (participant.playerPower * (0.4 + Random.nextDouble() * 0.6)).toInt()
                val isCrit = Random.nextDouble() < 0.15
                if (isCrit) dmg = (dmg * 1.5).toInt()
                if (participant.statusEffect == StatusEffect.STUN) dmg = (dmg * 0.5).toInt()

                participant.contributionScore += dmg
                instance.currentHp -= dmg

                // Apply burn on crit to boss
                if (isCrit && Random.nextDouble() < 0.3) {
                    applyStatusToBoss(instance, StatusEffect.BURN, 2)
                }

                val msg = if (isCrit) "CRIT! Dealt $dmg damage to ${instance.bossName}"
                else "Dealt $dmg damage to ${instance.bossName}"
                PlayerBossAction("attack", dmg, isCrit = isCrit, message = msg)
            }
            "scan" -> {
                val bonus = (participant.playerPower * 0.2).toInt() + 1
                participant.contributionScore += bonus
                participant.dodgeBuff = 0.15f  // +15% dodge next turn
                val hpPct = if (instance.bossPower > 0) (instance.currentHp * 100 / instance.bossPower) else 0
                PlayerBossAction("scan", 0, message = "Scanned ${instance.bossName} — HP at $hpPct%, Phase: ${instance.phase.name}")
            }
            "defend" -> {
                participant.shieldActive = true
                participant.dodgeBuff = 0.25f
                PlayerBossAction("defend", 0, message = "Shields raised against ${instance.bossName}'s next attack")
            }
            "heal" -> {
                val restored = (participant.playerPower * 0.2).toInt().coerceAtLeast(10)
                val actualHeal = (participant.currentHp + restored).coerceAtMost(participant.maxHp) - participant.currentHp
                participant.currentHp += actualHeal
                participant.contributionScore += actualHeal / 2
                // Cleanse burn on heal
                if (participant.statusEffect == StatusEffect.BURN) {
                    participant.statusEffect = StatusEffect.NONE
                    participant.statusDuration = 0
                    PlayerBossAction("heal", healAmount = actualHeal, message = "Restored $actualHeal HP and cleansed burn")
                } else {
                    PlayerBossAction("heal", healAmount = actualHeal, message = "Restored $actualHeal HP to self")
                }
            }
            else -> PlayerBossAction("invalid", message = "Unknown action")
        }
    }

    private fun executeBossTurn(instance: BossInstance): Triple<Int, String, String?> {
        val (baseDmg, abilityName) = when (instance.phase) {
            BossPhase.PHASE_1 -> Pair(instance.bossPower * 0.08, "Data Spike")
            BossPhase.PHASE_2 -> Pair(instance.bossPower * 0.14, "Firewall Surge")
            BossPhase.PHASE_3 -> Pair(instance.bossPower * 0.22, "System Purge")
        }

        val alive = instance.participants.filter { it.survived }
        var totalDmg = 0
        var knockedOut: String? = null

        when (instance.phase) {
            BossPhase.PHASE_1 -> {
                // Single target attack
                val target = alive.randomOrNull() ?: return Triple(0, abilityName, null)
                val dmg = resolveBossDamage(target, baseDmg.toInt(), abilityName)
                totalDmg = dmg
                if (!target.survived) knockedOut = target.username
            }
            BossPhase.PHASE_2 -> {
                // Multi-hit (50% chance) or single empowered
                if (Random.nextDouble() < 0.4 && alive.size >= 2) {
                    val hits = (2..3).random()
                    repeat(hits) {
                        val t = alive.filter { it.survived }.randomOrNull() ?: return@repeat
                        val dmg = resolveBossDamage(t, (baseDmg * 0.5).toInt().coerceAtLeast(5), abilityName)
                        totalDmg += dmg
                        if (!t.survived) knockedOut = t.username
                    }
                } else {
                    val target = alive.randomOrNull() ?: return Triple(0, abilityName, null)
                    val dmg = resolveBossDamage(target, (baseDmg * 1.2).toInt(), "$abilityName+")
                    totalDmg = dmg
                    if (!target.survived) knockedOut = target.username
                }
            }
            BossPhase.PHASE_3 -> {
                // Desperate attack: high damage but can miss
                if (Random.nextDouble() < 0.35) {
                    // Miss
                    return Triple(0, "$abilityName (miss)", null)
                }
                // AoE all alive players
                for (target in alive) {
                    val dmg = resolveBossDamage(target, (baseDmg * 0.8).toInt(), abilityName)
                    totalDmg += dmg
                    if (!target.survived) knockedOut = target.username
                }
            }
        }

        return Triple(totalDmg, abilityName, knockedOut)
    }

    private fun resolveBossDamage(participant: BossParticipant, damage: Int, abilityName: String): Int {
        // Dodge check
        val dodgeChance = 0.1f + participant.dodgeBuff
        if (Random.nextDouble() < dodgeChance) {
            participant.dodgeBuff = 0f
            return 0 // dodged
        }

        var dmg = damage

        // Shield blocks 50%
        if (participant.shieldActive) {
            dmg = (dmg * 0.5).toInt()
            participant.shieldActive = false
        }

        participant.currentHp = (participant.currentHp - dmg).coerceAtLeast(0)
        participant.dodgeBuff = 0f

        if (participant.currentHp <= 0) {
            participant.survived = false
        }

        return dmg
    }

    private fun applyStatusToBoss(instance: BossInstance, effect: StatusEffect, duration: Int) {
        // Boss status effects tracked via a simple field
        // For now, just reduce boss power temporarily
    }

    private fun getPhaseForHp(currentHp: Int, maxHp: Int): BossPhase {
        val pct = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
        return when {
            pct <= 0.33f -> BossPhase.PHASE_3
            pct <= 0.66f -> BossPhase.PHASE_2
            else -> BossPhase.PHASE_1
        }
    }

    private fun advanceTurn(instance: BossInstance) {
        val alive = instance.participants.filter { it.survived }
        if (alive.isEmpty()) return
        val currentIdx = alive.indexOfFirst { it.playerId == instance.participants.getOrNull(instance.currentTurnPlayerIndex)?.playerId }
        val nextIdx = if (currentIdx >= 0) (currentIdx + 1) % alive.size else 0
        val nextPlayer = alive[nextIdx]
        instance.currentTurnPlayerIndex = instance.participants.indexOfFirst { it.playerId == nextPlayer.playerId }
    }

    private suspend fun checkTurnTimeouts() {
        val now = System.currentTimeMillis()
        mutex.withLock {
            for (instance in instances.values) {
                if (instance.state == BossState.IN_PROGRESS && now - instance.lastActionTime > TURN_TIMEOUT_MS) {
                    // Auto-pass the turn for the current player — treat as defend
                    val currentPlayer = instance.participants.getOrNull(instance.currentTurnPlayerIndex) ?: continue
                    if (currentPlayer.survived) {
                        currentPlayer.shieldActive = true
                        advanceTurn(instance)
                        instance.lastActionTime = now
                    }
                }
            }
        }
    }

    private suspend fun resolveFight(instance: BossInstance, result: BossResult) {
        instance.state = BossState.COMPLETED
        instance.result = result
        instance.resolvedAt = System.currentTimeMillis()

        if (result == BossResult.VICTORY) {
            val baseCredits = instance.bossLevel * 200 + instance.threatLevel * 10
            val baseXp = instance.bossLevel * 50 + instance.threatLevel * 2
            val perPlayerCredits = (baseCredits * 1.5 / instance.participants.size).toLong().coerceAtLeast(50)
            val perPlayerXp = (baseXp * 1.5 / instance.participants.size).toInt().coerceAtLeast(20)

            // Bonus for top contributor
            val topContributor = instance.participants.maxByOrNull { it.contributionScore }
            val bonusCredits = perPlayerCredits * 0.2

            for (p in instance.participants) {
                playerService.withPlayerAction(p.playerId) { player ->
                    player.credits += perPlayerCredits
                    player.experience += perPlayerXp.toLong()
                    if (p == topContributor) {
                        player.credits += bonusCredits.toLong()
                    }
                    if (player.experience >= player.experienceToNext) {
                        player.level += 1
                        player.experience -= player.experienceToNext
                        player.experienceToNext = (player.experienceToNext * 1.2).toLong()
                    }
                }
                gameEventBus?.emit(GameEvent(p.playerId, AchievementEvent.WORLD_EVENT_PARTICIPATED, instance.bossLevel.toLong()))
            }
        }
    }

    private suspend fun cleanupStaleInstances() {
        val now = System.currentTimeMillis()
        mutex.withLock {
            val staleIds = instances.values
                .filter { now - it.createdAt > INSTANCE_TIMEOUT_MS && it.state == BossState.WAITING }
                .map { it.id }
            staleIds.forEach { id -> instances[id]?.state = BossState.EXPIRED }

            val expiredIds = instances.values
                .filter { it.state == BossState.COMPLETED && now - it.resolvedAt > 300_000L }
                .map { it.id }
            expiredIds.forEach { instances.remove(it) }
        }
    }

    suspend fun getInstance(instanceId: String): BossInstance? = mutex.withLock { instances[instanceId] }

    suspend fun getPlayerActiveInstance(playerId: String): BossInstance? = mutex.withLock {
        instances.values.firstOrNull {
            it.state != BossState.COMPLETED && it.state != BossState.EXPIRED &&
            it.participants.any { p -> p.playerId == playerId }
        }
    }

    suspend fun getAvailableInstances(zoneId: String): List<BossInstance> = mutex.withLock {
        instances.values.filter { it.zoneId == zoneId && it.state == BossState.WAITING && it.participants.size < MAX_PLAYERS }
    }

    fun serializeInstance(instance: BossInstance): Map<String, Any?> = mapOf(
        "id" to instance.id,
        "zoneId" to instance.zoneId,
        "bossName" to instance.bossName,
        "bossLevel" to instance.bossLevel,
        "bossPower" to instance.bossPower,
        "bossCurrentHp" to instance.currentHp.coerceAtLeast(0),
        "phase" to instance.phase.name,
        "state" to instance.state.name,
        "turnNumber" to instance.turnNumber,
        "currentTurnPlayerId" to instance.participants.getOrNull(instance.currentTurnPlayerIndex)?.playerId,
        "participants" to instance.participants.map { mapOf(
            "playerId" to it.playerId,
            "username" to it.username,
            "playerLevel" to it.playerLevel,
            "playerPower" to it.playerPower,
            "currentHp" to it.currentHp,
            "maxHp" to it.maxHp,
            "survived" to it.survived,
            "contributionScore" to it.contributionScore,
            "statusEffect" to it.statusEffect.name,
            "shieldActive" to it.shieldActive
        )},
        "result" to instance.result?.name
    )
}
