package com.zeroday.service

import com.zeroday.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WorldZoneService(
    private val playerService: PlayerService,
    private val gameEventBus: GameEventBus? = null
) {
    private val zones = mutableMapOf<String, Zone>()
    private val mutex = Mutex()
    private var travelJob: Job? = null

    data class TravelResult(val success: Boolean, val message: String, val travelTimeMs: Long = 0)
    data class ZoneActionResult(val success: Boolean, val message: String, val threatGained: Int = 0, val repGained: Int = 0)

    fun start(scope: CoroutineScope) {
        initializeZones()
        travelJob = scope.launch {
            while (isActive) {
                delay(10_000L)
                processTravelArrivals()
            }
        }
    }

    fun stop() {
        travelJob?.cancel()
    }

    private fun initializeZones() {
        val zoneDefs = listOf(
            Zone("safehouse", "Safehouse", "Your personal hideout. No faction activity here.",
                ZoneFaction.Neutral, ZoneState.Safe, 100, requiredLevel = 1,
                connectedZoneIds = listOf("downtown", "suburbs"), hasBoss = false, threatLevel = 0),
            Zone("downtown", "Downtown", "The bustling city center. Heavy network traffic.",
                ZoneFaction.CorpNet, ZoneState.Contested, 60, securityLevel = 2, requiredLevel = 1,
                connectedZoneIds = listOf("safehouse", "industrial", "financial", "suburbs"),
                hasBoss = true, bossName = "CorpSec AI", bossLevel = 5, threatLevel = 15),
            Zone("suburbs", "Suburbs", "Residential district with moderate security.",
                ZoneFaction.Neutral, ZoneState.Safe, 80, securityLevel = 1, requiredLevel = 1,
                connectedZoneIds = listOf("safehouse", "downtown", "industrial"),
                hasBoss = false, threatLevel = 5),
            Zone("industrial", "Industrial Zone", "Factory sector with weak security but little of value.",
                ZoneFaction.Syndicate, ZoneState.Controlled, 90, securityLevel = 1, requiredLevel = 3,
                connectedZoneIds = listOf("suburbs", "downtown", "ports"),
                hasBoss = true, bossName = "Syndicate Foreman", bossLevel = 8, threatLevel = 20),
            Zone("financial", "Financial District", "High-security banking sector.",
                ZoneFaction.CorpNet, ZoneState.Controlled, 85, securityLevel = 4, requiredLevel = 5,
                connectedZoneIds = listOf("downtown", "ports"),
                hasBoss = true, bossName = "Trading Algorithm", bossLevel = 12, threatLevel = 30),
            Zone("ports", "Port Authority", "Shipping and logistics hub.",
                ZoneFaction.GhostNet, ZoneState.Contested, 55, securityLevel = 2, requiredLevel = 4,
                connectedZoneIds = listOf("industrial", "financial", "research"),
                hasBoss = true, bossName = "Ghost Net Controller", bossLevel = 10, threatLevel = 25),
            Zone("research", "Research Lab", "Underground research facility.",
                ZoneFaction.FreeWorld, ZoneState.Controlled, 70, securityLevel = 3, requiredLevel = 6,
                connectedZoneIds = listOf("ports", "darknet"),
                hasBoss = true, bossName = "Lead Scientist", bossLevel = 15, threatLevel = 35),
            Zone("darknet", "Darknet", "The hidden underbelly of the network.",
                ZoneFaction.ZeroDay, ZoneState.Warzone, 40, securityLevel = 5, requiredLevel = 8,
                connectedZoneIds = listOf("research", "safehouse"),
                hasBoss = true, bossName = "Darknet Overlord", bossLevel = 20, threatLevel = 50),
            Zone("govt", "Govt Network", "Classified government systems. Maximum security.",
                ZoneFaction.LawEnforcement, ZoneState.Locked, 100, securityLevel = 5, requiredLevel = 10,
                connectedZoneIds = listOf("downtown", "darknet"),
                hasBoss = true, bossName = "AI Sentinel", bossLevel = 25, threatLevel = 60)
        )
        zoneDefs.forEach { zones[it.id] = it }
    }

    suspend fun getZone(zoneId: String): Zone? = zones[zoneId]

    suspend fun getAllZones(): List<Zone> = zones.values.toList()

    suspend fun getZoneSnapshots(): List<ZoneSnapshot> =
        zones.values.map { ZoneSnapshot.from(it) }.toList()

    suspend fun getZoneSnapshot(zoneId: String): ZoneSnapshot? =
        zones[zoneId]?.let { ZoneSnapshot.from(it) }

    suspend fun travelTo(playerId: String, targetZoneId: String): TravelResult = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock TravelResult(false, "Player not found")
        val zone = zones[targetZoneId] ?: return@withLock TravelResult(false, "Zone not found")
        if (targetZoneId == player.currentZoneId) return@withLock TravelResult(false, "Already in that zone")
        if (zone.state == ZoneState.Locked) return@withLock TravelResult(false, "Zone is locked")
        if (player.level < zone.requiredLevel) return@withLock TravelResult(
            false, "Level ${zone.requiredLevel} required to enter ${zone.name}")

        // Verify connection from current zone
        val currentZone = zones[player.currentZoneId]
        if (currentZone != null && !currentZone.connectedZoneIds.contains(targetZoneId) &&
            !zone.connectedZoneIds.contains(player.currentZoneId)) {
            return@withLock TravelResult(false, "No route from ${currentZone.name} to ${zone.name}")
        }

        val travelTimeMs = (1000L + zone.securityLevel * 2000L + zone.threatLevel * 100L)
            .coerceAtMost(30_000L)

        playerService.withPlayerAction(playerId) { p ->
            p.currentZoneId = targetZoneId
        }

        TravelResult(true, "Arrived at ${zone.name}", travelTimeMs)
    }

    suspend fun attackZone(playerId: String, targetZoneId: String): ZoneActionResult = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock ZoneActionResult(false, "Player not found")
        val zone = zones[targetZoneId] ?: return@withLock ZoneActionResult(false, "Zone not found")
        if (zone.state == ZoneState.Locked) return@withLock ZoneActionResult(false, "Zone is locked")

        val playerPower = player.level * 10 + (player.breakthroughMultiplier * 100).toInt()
        val zoneDefense = zone.securityLevel * 50 + zone.controlLevel * 2 + zone.threatLevel * 3
        val success = Math.random() < (playerPower.toDouble() / (playerPower + zoneDefense)).coerceIn(0.05, 0.95)

        if (success) {
            val controlGain = (5 + player.level / 3).coerceAtMost(20)
            zone.controlLevel = (zone.controlLevel - controlGain).coerceAtLeast(0)
            val repGain = controlGain / 2
            playerService.withPlayerAction(playerId) { p ->
                p.zoneControlContributions[targetZoneId] =
                    (p.zoneControlContributions[targetZoneId] ?: 0) + controlGain
                p.factionReputations[zone.controllingFaction.name] =
                    (p.factionReputations[zone.controllingFaction.name] ?: 0) - repGain
            }
            updateZoneState(zone)
            gameEventBus?.emit(GameEvent(playerId, AchievementEvent.NETWORK_COMPROMISED, controlGain.toLong()))
            ZoneActionResult(true, "Attack successful! Reduced ${zone.name} control by $controlGain.", repGain, repGain)
        } else {
            playerService.withPlayerAction(playerId) { p ->
                p.cpu = (p.cpu - 10).coerceAtLeast(0)
            }
            ZoneActionResult(false, "Attack failed. Zone defense too strong. Lost 10 CPU.")
        }
    }

    suspend fun claimZone(playerId: String, targetZoneId: String): ZoneActionResult = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock ZoneActionResult(false, "Player not found")
        val zone = zones[targetZoneId] ?: return@withLock ZoneActionResult(false, "Zone not found")
        if (zone.controlLevel > 0) return@withLock ZoneActionResult(false, "Zone still has ${zone.controlLevel}% control remaining. Attack first.")

        val careerBonus = if (player.careerPath == "blackhat") 10 else 5
        playerService.withPlayerAction(playerId) { p ->
            p.credits += (500L + zone.threatLevel * 10L)
            p.factionReputations[ZoneFaction.ZeroDay.name] =
                (p.factionReputations[ZoneFaction.ZeroDay.name] ?: 0) + careerBonus
        }

        zone.controllingFaction = ZoneFaction.ZeroDay
        zone.state = ZoneState.Controlled
        zone.controlLevel = 20

        gameEventBus?.emit(GameEvent(playerId, AchievementEvent.WORLD_EVENT_PARTICIPATED, zone.threatLevel.toLong()))
        ZoneActionResult(true, "Zone claimed for ZeroDay! Earned ${500 + zone.threatLevel * 10} credits.", 0, careerBonus)
    }

    fun updateZoneState(zone: Zone) {
        zone.state = when {
            zone.controlLevel <= 0 -> ZoneState.Warzone
            zone.controlLevel <= 25 -> ZoneState.Contested
            zone.controlLevel >= 75 -> ZoneState.Controlled
            zone.controlLevel < 75 -> ZoneState.Contested
            else -> ZoneState.Safe
        }
    }

    fun getActiveFaction(): ZoneFaction {
        val weekMs = 7L * 24 * 60 * 60 * 1000
        val cycleIndex = (System.currentTimeMillis() / weekMs).toInt() % ZoneFaction.values().size
        return ZoneFaction.values()[cycleIndex]
    }

    /**
     * Returns the cycle info and applies temporary buffs to zones based on
     * the active faction. Buffs are computed on-the-fly (stored data unchanged):
     *   - Active faction zones: threat +20%, security -15%
     *   - Enemy zones: threat -10%, security +10%
     */
    fun getActiveFactionCycle(): Map<String, Any> {
        val activeFaction = getActiveFaction()
        val weekMs = 7L * 24 * 60 * 60 * 1000
        val buffedZoneIds = zones.values
            .filter { it.controllingFaction == activeFaction }
            .map { it.id }
        return mapOf(
            "activeFaction" to activeFaction.name,
            "displayName" to activeFaction.displayName,
            "bonus" to "1.5x reputation gain for ${activeFaction.displayName} activities",
            "cycleRemainingMs" to (weekMs - (System.currentTimeMillis() % weekMs)),
            "buffedZoneIds" to buffedZoneIds,
            "repMultiplier" to 1.5,
            "threatBoost" to 0.2,
            "securityReduction" to 0.15
        )
    }

    /**
     * Returns a zone snapshot with cycle buffs applied for display.
     */
    fun getBuffedZoneSnapshot(zoneId: String): Map<String, Any>? {
        val zone = zones[zoneId] ?: return null
        val active = getActiveFaction()
        val isActiveFaction = zone.controllingFaction == active
        return mapOf(
            "id" to zone.id,
            "name" to zone.name,
            "controllingFaction" to zone.controllingFaction.name,
            "state" to zone.state.name,
            "controlLevel" to zone.controlLevel,
            "maxControlLevel" to zone.maxControlLevel,
            "securityLevel" to if (isActiveFaction)
                (zone.securityLevel * 0.85).toInt().coerceAtLeast(1) else
                (zone.securityLevel * 1.10).toInt(),
            "threatLevel" to if (isActiveFaction)
                (zone.threatLevel * 1.20).toInt() else
                (zone.threatLevel * 0.90).toInt().coerceAtLeast(0),
            "requiredLevel" to zone.requiredLevel,
            "isBuffed" to isActiveFaction,
            "hasBoss" to zone.hasBoss,
            "bossName" to zone.bossName,
            "bossLevel" to zone.bossLevel
        )
    }

    private val travelingPlayers = mutableMapOf<String, TravelSession>()

    data class TravelSession(val playerId: String, val fromZoneId: String, val toZoneId: String, val arrivalTimeMs: Long)

    suspend fun initiateTravel(playerId: String, targetZoneId: String): TravelResult {
        val result = travelTo(playerId, targetZoneId)
        if (result.success && result.travelTimeMs > 100) {
            val player = playerService.getPlayer(playerId)
            travelingPlayers[playerId] = TravelSession(
                playerId, player?.currentZoneId ?: "safehouse",
                targetZoneId, System.currentTimeMillis() + result.travelTimeMs
            )
            return TravelResult(true, "Traveling... ETA ${result.travelTimeMs / 1000}s", result.travelTimeMs)
        }
        return result
    }

    private suspend fun processTravelArrivals() {
        val now = System.currentTimeMillis()
        val arrived = travelingPlayers.filter { it.value.arrivalTimeMs <= now }.keys.toList()
        for (pid in arrived) {
            travelingPlayers.remove(pid)
            gameEventBus?.emit(GameEvent(pid, AchievementEvent.NODE_DISCOVERED, 1))
        }
    }
}
