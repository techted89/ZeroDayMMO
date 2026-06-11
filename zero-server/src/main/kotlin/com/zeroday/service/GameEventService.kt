package com.zeroday.service

import com.zeroday.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class GameEventService(
    private val playerService: PlayerService,
    private val gameEventBus: GameEventBus? = null
) {
    enum class EventMode { CAPTURE_THE_CORE, KING_OF_NODE, DATA_RACE, LAST_STAND, THE_HEIST }
    enum class EventStatus { UPCOMING, ACTIVE, ENDED }

    data class GameEvent(
        val eventId: String = UUID.randomUUID().toString(),
        val title: String,
        val description: String,
        val mode: EventMode,
        val zoneId: String,
        var status: EventStatus = EventStatus.UPCOMING,
        val minLevel: Int = 1,
        val maxParticipants: Int = 20,
        var participantCount: Int = 0,
        var entryFee: Long = 0L,
        var prizePool: Long = 0L,
        val startsAt: Long = System.currentTimeMillis(),
        var endsAt: Long = System.currentTimeMillis() + 3_600_000L,
        var leaderboard: MutableList<EventParticipant> = mutableListOf()
    )

    data class EventParticipant(
        val playerId: String,
        val playerName: String,
        var score: Long = 0L,
        var rank: Int = 0
    )

    data class EventReward(
        val minRank: Int,
        val credits: Long = 0L,
        val itemName: String = "",
        val itemRarity: String = "Common"
    )

    private val events = mutableMapOf<String, GameEvent>()
    private val mutex = Mutex()
    private var tickJob: Job? = null

    private val defaultRewards = mapOf(
        EventMode.CAPTURE_THE_CORE to listOf(
            EventReward(1, 5000, "Legendary CPU Core", "Legendary"),
            EventReward(2, 2500, "Epic Scanner", "Epic"),
            EventReward(3, 1000, "Rare Firewall", "Rare")
        ),
        EventMode.KING_OF_NODE to listOf(
            EventReward(1, 12000, "Mythic Payload", "Mythic"),
            EventReward(2, 6000, "Legendary Toolkit", "Legendary"),
            EventReward(3, 3000, "Epic Armor", "Epic")
        ),
        EventMode.DATA_RACE to listOf(
            EventReward(1, 8000, "Legendary RAM", "Legendary"),
            EventReward(2, 4000, "Epic Scanner", "Epic"),
            EventReward(3, 2000, "Rare Payload", "Rare")
        ),
        EventMode.LAST_STAND to listOf(
            EventReward(1, 25000, "Exotic Armor", "Exotic"),
            EventReward(2, 12000, "Mythic Toolkit", "Mythic"),
            EventReward(3, 6000, "Legendary Firewall", "Legendary")
        ),
        EventMode.THE_HEIST to listOf(
            EventReward(1, 50000, "Exotic Payload", "Exotic"),
            EventReward(2, 25000, "Mythic Firewall", "Mythic"),
            EventReward(3, 12000, "Legendary Toolkit", "Legendary")
        )
    )

    fun start(scope: CoroutineScope) {
        seedEvents()
        tickJob = scope.launch {
            while (isActive) {
                delay(30_000L)
                tick()
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
    }

    private fun seedEvents() {
        val baseTime = System.currentTimeMillis()
        events["evt_ctf_001"] = GameEvent(
            title = "Capture the Core", description = "Two teams compete to capture and hold the central data core. First to 5 captures wins.",
            mode = EventMode.CAPTURE_THE_CORE, zoneId = "industrial", status = EventStatus.ACTIVE,
            minLevel = 5, maxParticipants = 20, participantCount = 12, entryFee = 100, prizePool = 5000,
            startsAt = baseTime, endsAt = baseTime + 2700000
        )
        events["evt_koth_002"] = GameEvent(
            title = "King of the Node", description = "Control the central server node for the longest cumulative time.",
            mode = EventMode.KING_OF_NODE, zoneId = "darknet", status = EventStatus.ACTIVE,
            minLevel = 10, maxParticipants = 16, participantCount = 8, entryFee = 250, prizePool = 12000,
            startsAt = baseTime, endsAt = baseTime + 1800000
        )
        events["evt_race_003"] = GameEvent(
            title = "Data Race", description = "Race through network nodes to the finish. Avoid traps and firewalls.",
            mode = EventMode.DATA_RACE, zoneId = "financial", status = EventStatus.ACTIVE,
            minLevel = 8, maxParticipants = 10, participantCount = 6, entryFee = 150, prizePool = 8000,
            startsAt = baseTime, endsAt = baseTime + 3600000
        )
        events["evt_stand_004"] = GameEvent(
            title = "Last Stand", description = "Survive waves of AI attackers. Last player standing wins.",
            mode = EventMode.LAST_STAND, zoneId = "nexus", status = EventStatus.UPCOMING,
            minLevel = 15, maxParticipants = 8, entryFee = 500, prizePool = 25000,
            startsAt = baseTime + 3600000, endsAt = baseTime + 7200000
        )
        events["evt_heist_005"] = GameEvent(
            title = "The Great Heist", description = "Coordinate to breach the central vault. Highest haul wins.",
            mode = EventMode.THE_HEIST, zoneId = "govt", status = EventStatus.ACTIVE,
            minLevel = 12, maxParticipants = 6, participantCount = 4, entryFee = 1000, prizePool = 50000,
            startsAt = baseTime, endsAt = baseTime + 5400000
        )
    }

    private suspend fun tick() {
        val now = System.currentTimeMillis()
        mutex.withLock {
            for (event in events.values) {
                // Start upcoming events
                if (event.status == EventStatus.UPCOMING && now >= event.startsAt) {
                    event.status = EventStatus.ACTIVE
                }
                // End expired events
                if (event.status == EventStatus.ACTIVE && now >= event.endsAt) {
                    resolveEvent(event)
                }
            }
        }
    }

    private suspend fun resolveEvent(event: GameEvent) {
        event.status = EventStatus.ENDED
        val rewards = defaultRewards[event.mode] ?: return

        val sorted = event.leaderboard.sortedByDescending { it.score }
        for ((idx, participant) in sorted.withIndex()) {
            val rank = idx + 1
            participant.rank = rank

            val reward = rewards.firstOrNull { it.minRank == rank }
            if (reward != null) {
                playerService.withPlayerAction(participant.playerId) { player ->
                    player.credits += reward.credits
                }
                gameEventBus?.emit(GameEvent(participant.playerId, AchievementEvent.WORLD_EVENT_PARTICIPATED, reward.credits))
            }
        }
    }

    suspend fun joinEvent(eventId: String, playerId: String): Result<GameEvent> = mutex.withLock {
        val event = events[eventId] ?: return@withLock Result.failure(Exception("Event not found"))
        if (event.status != EventStatus.ACTIVE && event.status != EventStatus.UPCOMING)
            return@withLock Result.failure(Exception("Event is not available"))
        if (event.participantCount >= event.maxParticipants)
            return@withLock Result.failure(Exception("Event is full"))
        if (event.leaderboard.any { it.playerId == playerId })
            return@withLock Result.failure(Exception("Already joined"))

        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        if (player.level < event.minLevel)
            return@withLock Result.failure(Exception("Level ${event.minLevel} required"))

        if (event.entryFee > 0) {
            if (player.credits < event.entryFee)
                return@withLock Result.failure(Exception("Insufficient credits for entry fee"))
            player.credits -= event.entryFee
            event.prizePool += event.entryFee
        }

        event.leaderboard.add(EventParticipant(playerId = playerId, playerName = player.username))
        event.participantCount++
        Result.success(event)
    }

    suspend fun leaveEvent(eventId: String, playerId: String): Result<Unit> = mutex.withLock {
        val event = events[eventId] ?: return@withLock Result.failure(Exception("Event not found"))
        val removed = event.leaderboard.removeAll { it.playerId == playerId }
        if (!removed) return@withLock Result.failure(Exception("Not a participant"))
        event.participantCount--
        Result.success(Unit)
    }

    suspend fun updateScore(eventId: String, playerId: String, scoreDelta: Long): Result<EventParticipant> = mutex.withLock {
        val event = events[eventId] ?: return@withLock Result.failure(Exception("Event not found"))
        val participant = event.leaderboard.firstOrNull { it.playerId == playerId }
            ?: return@withLock Result.failure(Exception("Not a participant"))
        participant.score += scoreDelta
        Result.success(participant)
    }

    suspend fun getActiveEvents(): List<GameEvent> = mutex.withLock {
        events.values.filter { it.status == EventStatus.ACTIVE || it.status == EventStatus.UPCOMING }
            .sortedBy { if (it.status == EventStatus.ACTIVE) 0 else 1 }
    }

    suspend fun getEventLeaderboard(eventId: String): List<EventParticipant> = mutex.withLock {
        events[eventId]?.leaderboard?.sortedByDescending { it.score } ?: emptyList()
    }

    suspend fun getPlayerEvents(playerId: String): List<GameEvent> = mutex.withLock {
        events.values.filter { it.leaderboard.any { p -> p.playerId == playerId } }
    }

    fun getEvent(eventId: String): GameEvent? = events[eventId]

    fun serializeEvent(event: GameEvent): Map<String, Any?> = mapOf(
        "eventId" to event.eventId,
        "title" to event.title,
        "description" to event.description,
        "modeType" to event.mode.name.lowercase(),
        "status" to event.status.name.lowercase(),
        "zoneId" to event.zoneId,
        "minLevel" to event.minLevel,
        "participantCount" to event.participantCount,
        "maxParticipants" to event.maxParticipants,
        "entryFee" to event.entryFee,
        "prizePool" to event.prizePool,
        "timeRemainingMinutes" to ((event.endsAt - System.currentTimeMillis()) / 60_000).coerceAtLeast(0),
        "topPlayers" to event.leaderboard.sortedByDescending { it.score }.take(10).mapIndexed { idx, p ->
            val reward = defaultRewards[event.mode]?.firstOrNull { it.minRank == idx + 1 }
            mapOf(
                "playerName" to p.playerName,
                "score" to p.score,
                "reward" to (reward?.let { "${it.itemName} + ${it.credits}cr" } ?: ""),
                "rank" to (idx + 1)
            )
        }
    )
}
