package com.zeroday.service

import com.zeroday.handler.ConnectionRegistry
import com.zeroday.model.*
import com.zeroday.protocol.ResponseTypes
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.random.Random

class WorldEventService(
    private val playerService: PlayerService,
    private val factionService: FactionService,
    private val gameEventBus: GameEventBus? = null
) {
    private val activeEvents = mutableMapOf<String, WorldEvent>()

    /**
     * Connection registry for real-time push. Set after construction
     * by [com.zeroday.ZeroDayServer.configure] since the registry is
     * created after the service registry.
     */
    var connectionRegistry: com.zeroday.handler.ConnectionRegistry? = null
    private val eventHistory = mutableListOf<WorldEvent>()
    private val mutex = Mutex()
    private var eventGenJob: Job? = null
    private var eventUpdateJob: Job? = null
    private var rewardDistJob: Job? = null

    fun start(scope: CoroutineScope) {
        eventGenJob = scope.launch {
            while (isActive) {
                generateRandomEvent()
                delay(Random.nextLong(300_000, 900_000)) // 5-15 minutes
            }
        }
        eventUpdateJob = scope.launch {
            while (isActive) {
                updateActiveEvents()
                delay(10_000) // Every 10 seconds
            }
        }
        rewardDistJob = scope.launch {
            while (isActive) {
                distributeEventRewards()
                delay(30_000) // Every 30 seconds
            }
        }
    }

    fun stop() {
        eventGenJob?.cancel()
        eventUpdateJob?.cancel()
        rewardDistJob?.cancel()
    }

    private suspend fun generateRandomEvent() = mutex.withLock {
        if (activeEvents.size >= 3) return@withLock // Max 3 concurrent events

        val template = WorldEventTemplates.eventTemplates.randomOrNull()
        if (template == null) return@withLock

        val event = WorldEvent(
            id = UUID.randomUUID().toString(),
            title = template.title,
            description = template.description,
            eventType = template.eventType,
            severity = template.severity,
            affectedNetworks = template.affectedNetworks,
            effects = template.effects.map { it.copy() },
            rewardPool = template.rewardPool,
            rewardPerParticipant = template.rewardPerParticipant,
            endsAt = if (template.effects.any { it.durationMs > 0 }) {
                val max = template.effects.maxByOrNull { it.durationMs }
                if (max != null) System.currentTimeMillis() + max.durationMs else 0L
            } else 0L
        )

        activeEvents[event.id] = event
        broadcastEventUpdate(event)
    }

    private suspend fun updateActiveEvents() = mutex.withLock {
        val now = System.currentTimeMillis()
        val toRemove = mutableListOf<String>()

        for ((id, event) in activeEvents) {
            // Check if event should end due to time
            if (event.endsAt > 0 && now >= event.endsAt) {
                toRemove.add(id)
                continue
            }

            // Update time remaining
            val timeRemaining = if (event.endsAt > 0) {
                (event.endsAt - now).coerceAtLeast(0)
            } else Long.MAX_VALUE // Permanent until resolved

            // Check if auto-resolved (simplified)
            if (timeRemaining < 60_000 && Random.nextDouble() < 0.001) { // 0.1% chance per tick to auto-resolve near end
                toRemove.add(id)
            }

            // Send updates to clients
            val update = WorldEventUpdate(
                eventId = event.id,
                title = event.title,
                description = event.description,
                eventType = event.eventType,
                severity = event.severity,
                isActive = event.isActive,
                timeRemainingMs = timeRemaining,
                effects = event.effects,
                participantCount = event.participatingFactions.size,
                rewardPool = event.rewardPool
            )
            broadcastEventUpdate(update)
        }

        // Remove expired events
        for (id in toRemove) {
            val event = activeEvents.remove(id)
            if (event != null) {
                eventHistory.add(event)
                // Keep only last 50 events in history
                if (eventHistory.size > 50) {
                    eventHistory.removeAt(0)
                }
                broadcastEventExpired(event.id)
            }
        }
    }

    private suspend fun distributeEventRewards() = mutex.withLock {
        for ((_, event) in activeEvents) {
            if (event.rewardPool <= 0 || event.rewardPerParticipant <= 0) continue

            val participants = event.participatingFactions.distinct()
            val totalReward = participants.size * event.rewardPerParticipant
            val actualReward = Math.min(event.rewardPool, totalReward)

            if (actualReward <= 0) continue

            val rewardPerPerson = actualReward / participants.size
            var remaining = actualReward % participants.size

            for (factionId in event.participatingFactions) {
                val faction = factionService.getFactionById(factionId) ?: continue
                for (memberId in faction.members) {
                    playerService.addCredits(memberId, rewardPerPerson.toLong())
                    if (remaining > 0) {
                        playerService.addCredits(memberId, 1)
                        remaining--
                    }
                }
            }

            event.rewardPool = 0
        }
    }

    suspend fun getActiveEvents(): List<WorldEventUpdate> = mutex.withLock {
        val now = System.currentTimeMillis()
        activeEvents.map { (_, event) ->
            val timeRemaining = if (event.endsAt > 0) {
                (event.endsAt - now).coerceAtLeast(0)
            } else Long.MAX_VALUE
            WorldEventUpdate(
                eventId = event.id,
                title = event.title,
                description = event.description,
                eventType = event.eventType,
                severity = event.severity,
                isActive = event.isActive,
                timeRemainingMs = timeRemaining,
                effects = event.effects,
                participantCount = event.participatingFactions.size,
                rewardPool = event.rewardPool
            )
        }
    }

    suspend fun joinEvent(playerId: String, eventId: String): Result<WorldEvent> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val event = activeEvents[eventId] ?: return@withLock Result.failure(Exception("Event not found or expired"))

        if (!event.isActive) {
            return@withLock Result.failure(Exception("Event is no longer active"))
        }

        // Add faction if player has one
        val factionId = player.factionId
        if (factionId != null && !event.participatingFactions.contains(factionId)) {
            event.participatingFactions.add(factionId)
        }

        if (gameEventBus != null) {
            gameEventBus.emit(GameEvent(playerId, com.zeroday.model.AchievementEvent.WORLD_EVENT_PARTICIPATED, 1L))
        }
        Result.success(event)
    }

    suspend fun leaveEvent(playerId: String, eventId: String): Result<Unit> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val event = activeEvents[eventId] ?: return@withLock Result.failure(Exception("Event not found"))

        val factionId = player.factionId
        if (factionId != null) {
            event.participatingFactions.removeAll { it == factionId }
        }

        Result.success(Unit)
    }

    private suspend fun broadcastEventUpdate(event: WorldEvent) {
        val envelope = mapOf<String, Any?>(
            "type" to ResponseTypes.PUSH_WORLD_EVENT,
            "event" to mapOf(
                "id" to event.id,
                "title" to event.title,
                "description" to event.description,
                "eventType" to event.eventType.name,
                "severity" to event.severity.name,
                "isActive" to event.isActive,
                "endsAt" to event.endsAt,
                "effects" to event.effects.map { mapOf("effectType" to it.effectType.name, "value" to it.value, "durationMs" to it.durationMs) }
            )
        )
        connectionRegistry?.broadcastToChannel("events", envelope)
    }

    private suspend fun broadcastEventUpdate(update: WorldEventUpdate) {
        val envelope = mapOf<String, Any?>(
            "type" to ResponseTypes.PUSH_WORLD_EVENT,
            "update" to mapOf(
                "eventId" to update.eventId,
                "title" to update.title,
                "description" to update.description,
                "eventType" to update.eventType.name,
                "severity" to update.severity.name,
                "remainingTimeMs" to update.timeRemainingMs,
                "isActive" to update.isActive,
                "participantCount" to update.participantCount,
                "rewardPool" to update.rewardPool
            )
        )
        connectionRegistry?.broadcastToChannel("events", envelope)
    }

    private suspend fun broadcastEventExpired(eventId: String) {
        val envelope = mapOf<String, Any?>(
            "type" to ResponseTypes.PUSH_WORLD_EVENT,
            "eventId" to eventId,
            "expired" to true
        )
        connectionRegistry?.broadcastToChannel("events", envelope)
    }

    suspend fun getEventHistory(): List<WorldEvent> = mutex.withLock {
        eventHistory.reversed()
    }
}
