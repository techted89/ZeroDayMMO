package com.zeroday.service

import com.zeroday.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class FactionService(
    private val playerService: PlayerService,
    private val gameEventBus: GameEventBus? = null
) {
    private val factions = mutableMapOf<String, Faction>()
    private val mutex = Mutex()
    private var passiveIncomeJob: Job? = null

    fun start(scope: CoroutineScope) {
        passiveIncomeJob = scope.launch {
            while (isActive) {
                delay(60_000L)
                processPassiveIncome()
            }
        }
    }

    fun stop() {
        passiveIncomeJob?.cancel()
    }

    suspend fun createFaction(playerId: String, name: String, tag: String, description: String): Result<Faction> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        if (player.level < 5) return@withLock Result.failure(Exception("Level 5 required to create a faction"))
        if (player.factionId != null) return@withLock Result.failure(Exception("Already in a faction"))

        if (factions.values.any { it.name.equals(name, ignoreCase = true) })
            return@withLock Result.failure(Exception("Faction name already taken"))
        if (factions.values.any { it.tag.equals(tag, ignoreCase = true) })
            return@withLock Result.failure(Exception("Faction tag already taken"))

        val faction = Faction(
            name = name,
            tag = tag.uppercase().take(5),
            description = description,
            leaderId = playerId,
            members = mutableListOf(playerId)
        )
        factions[faction.id] = faction
        playerService.setFaction(playerId, faction.id)
        playerService.addCredits(playerId, -1000)
        Result.success(faction)
    }

    suspend fun joinFaction(playerId: String, factionId: String): Result<Faction> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val faction = factions[factionId] ?: return@withLock Result.failure(Exception("Faction not found"))

        if (player.factionId != null) return@withLock Result.failure(Exception("Already in a faction"))
        if (!faction.isOpen && faction.leaderId != playerId) return@withLock Result.failure(Exception("Faction is invite-only"))
        if (faction.members.size >= getMemberLimit(faction)) return@withLock Result.failure(Exception("Faction is full"))

        faction.members.add(playerId)
        playerService.setFaction(playerId, factionId)
        if (gameEventBus != null) {
            gameEventBus.emit(GameEvent(playerId, com.zeroday.model.AchievementEvent.FACTION_JOINED, 1L))
        }
        Result.success(faction)
    }

    suspend fun leaveFaction(playerId: String): Result<Unit> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val factionId = player.factionId ?: return@withLock Result.failure(Exception("Not in a faction"))
        val faction = factions[factionId] ?: return@withLock Result.failure(Exception("Faction not found"))

        if (faction.leaderId == playerId) {
            if (faction.members.size > 1)
                return@withLock Result.failure(Exception("Transfer leadership before leaving"))
            factions.remove(factionId)
        } else {
            faction.members.remove(playerId)
        }
        playerService.setFaction(playerId, null)
        Result.success(Unit)
    }

    suspend fun donateToMainframe(playerId: String, cpu: Long = 0, ram: Long = 0, bandwidth: Long = 0, credits: Long = 0): Result<FactionMainframe> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val factionId = player.factionId ?: return@withLock Result.failure(Exception("Not in a faction"))
        val faction = factions[factionId] ?: return@withLock Result.failure(Exception("Faction not found"))

        val mf = faction.mainframe
        if (cpu > 0) {
            if (player.cpu < cpu.toInt()) return@withLock Result.failure(Exception("Not enough CPU"))
            playerService.consumeResources(playerId, cpu.toInt(), 0)
            mf.cpuUsed = (mf.cpuUsed - cpu).coerceAtLeast(0)
            mf.cpuTotal += cpu
        }
        if (ram > 0) {
            if (player.ram < ram.toInt()) return@withLock Result.failure(Exception("Not enough RAM"))
            playerService.consumeResources(playerId, 0, ram.toInt())
            mf.ramUsed = (mf.ramUsed - ram).coerceAtLeast(0)
            mf.ramTotal += ram
        }
        if (bandwidth > 0) {
            if (player.bandwidth < bandwidth.toInt()) return@withLock Result.failure(Exception("Not enough bandwidth"))
            mf.bandwidthTotal += bandwidth
        }
        if (credits > 0) {
            if (player.credits < credits) return@withLock Result.failure(Exception("Not enough credits"))
            playerService.addCredits(playerId, -credits)
            mf.creditsPool += credits
        }
        Result.success(mf)
    }

    suspend fun upgradeMainframe(playerId: String, upgradeId: String): Result<FactionMainframe> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val factionId = player.factionId ?: return@withLock Result.failure(Exception("Not in a faction"))
        val faction = factions[factionId] ?: return@withLock Result.failure(Exception("Faction not found"))

        if (faction.leaderId != playerId) return@withLock Result.failure(Exception("Only the leader can upgrade"))

        val upgrade = FactionUpgradesRegistry.allUpgrades.find { it.id == upgradeId }
            ?: return@withLock Result.failure(Exception("Unknown upgrade"))

        val currentLevel = faction.mainframe.upgradeProgress[upgradeId]?.toInt() ?: 0
        val nextLevel = upgrade.levels.find { it.level == currentLevel + 1 }
            ?: return@withLock Result.failure(Exception("Max level reached for this upgrade"))

        if (faction.level < nextLevel.requiredFactionLevel)
            return@withLock Result.failure(Exception("Faction level ${nextLevel.requiredFactionLevel} required"))

        if (faction.mainframe.creditsPool < nextLevel.costCredits)
            return@withLock Result.failure(Exception("Not enough credits in pool"))
        if (faction.mainframe.cpuTotal - faction.mainframe.cpuUsed < nextLevel.costCpu)
            return@withLock Result.failure(Exception("Not enough CPU capacity"))
        if (faction.mainframe.ramTotal - faction.mainframe.ramUsed < nextLevel.costRam)
            return@withLock Result.failure(Exception("Not enough RAM capacity"))

        faction.mainframe.creditsPool -= nextLevel.costCredits
        faction.mainframe.cpuUsed += nextLevel.costCpu
        faction.mainframe.ramUsed += nextLevel.costRam
        faction.mainframe.upgradeProgress[upgradeId] = (currentLevel + 1).toLong()

        applyUpgradeBenefit(faction, upgrade.category, nextLevel)
        Result.success(faction.mainframe)
    }

    private fun applyUpgradeBenefit(faction: Faction, category: UpgradeCategory, level: UpgradeLevel) {
        when (category) {
            UpgradeCategory.CPU_CAPACITY -> {}
            UpgradeCategory.RAM_CAPACITY -> {}
            UpgradeCategory.BANDWIDTH_CAPACITY -> {}
            UpgradeCategory.SECURITY -> faction.mainframe.securityLevel += level.value.toInt()
            UpgradeCategory.PASSIVE_INCOME -> faction.mainframe.passiveIncomeRate = level.value
            UpgradeCategory.MEMBER_LIMIT -> {}
            UpgradeCategory.BUFF_DURATION -> {}
            UpgradeCategory.FEATURE_UNLOCK -> {
                val features = mapOf(
                    1 to "faction_scan", 2 to "mass_ddos", 3 to "faction_shield"
                )
                features[level.level]?.let { faction.mainframe.unlockedFeatures.add(it) }
            }
        }
    }

    suspend fun getFaction(playerId: String): Result<Faction> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val factionId = player.factionId ?: return@withLock Result.failure(Exception("Not in a faction"))
        val faction = factions[factionId] ?: return@withLock Result.failure(Exception("Faction not found"))
        Result.success(faction)
    }

    suspend fun listFactions(): List<FactionSummary> = mutex.withLock {
        factions.values.map { f ->
            FactionSummary(
                id = f.id, name = f.name, tag = f.tag,
                memberCount = f.members.size, level = f.level,
                leaderName = playerService.getPlayer(f.leaderId)?.username ?: "Unknown",
                isOpen = f.isOpen
            )
        }
    }

    suspend fun getFactionById(factionId: String): Faction? = mutex.withLock {
        factions[factionId]
    }

    private suspend fun processPassiveIncome() = mutex.withLock {
        for ((_, faction) in factions) {
            if (faction.mainframe.passiveIncomeRate > 0 && faction.members.isNotEmpty()) {
                val incomePerMember = (faction.mainframe.passiveIncomeRate * faction.mainframe.level).toLong()
                for (memberId in faction.members) {
                    playerService.addCredits(memberId, incomePerMember)
                }
                faction.mainframe.creditsPool += incomePerMember * faction.members.size / 10
            }
        }
    }

    private fun getMemberLimit(faction: Faction): Int {
        val level = faction.mainframe.upgradeProgress["member_limit"]?.toInt() ?: 1
        return when (level) {
            0 -> 10; 1 -> 15; 2 -> 25; 3 -> 50; else -> 10
        }
    }

    suspend fun getOnlineMembers(factionId: String): List<String> = mutex.withLock {
        val faction = factions[factionId] ?: return@withLock emptyList()
        faction.members.filter { memberId ->
            playerService.getPlayer(memberId)?.isOnline == true
        }
    }
}

data class FactionSummary(
    val id: String,
    val name: String,
    val tag: String,
    val memberCount: Int,
    val level: Int,
    val leaderName: String,
    val isOpen: Boolean
)
