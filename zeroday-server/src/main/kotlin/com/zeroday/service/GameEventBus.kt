package com.zeroday.service

import com.zeroday.model.AchievementEvent
import com.zeroday.model.NotificationType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

/**
 * Lightweight in-process event bus for game-event observation.
 *
 * Services that produce interesting events (PlayerService on level
 * up, TaskService on task complete, NetworkService on node
 * discovery, …) call [emit]. Listeners — currently the achievement
 * and challenge services — register via [onEvent] and get called
 * with the player snapshot.
 *
 * The bus is fire-and-forget: listeners are best-effort and their
 * exceptions are caught and logged so a buggy listener can't take
 * down the producer.
 *
 * Concurrency:
 *   - The lock is held only while we *snapshot* the listener list
 *     and *append* new listeners during a dispatch. That means a
 *     slow or blocking listener cannot stall a `onEvent` call.
 *   - Listeners are dispatched concurrently in a child coroutine
 *     scope, so the producer is not blocked by listener latency.
 *   - Each listener gets its own coroutine, so one listener
 *     throwing or hanging does not affect the others.
 *
 * Note: this is intentionally simple (single-process, no delivery
 * guarantees beyond the current JVM). For a clustered deployment
 * swap in a real pub/sub.
 */
class GameEventBus {
    private val log = LoggerFactory.getLogger(GameEventBus::class.java)
    private val mutex = Mutex()
    private val listeners = mutableListOf<suspend (GameEvent) -> Unit>()

    fun onEvent(listener: suspend (GameEvent) -> Unit) {
        synchronized(mutex) { listeners += listener }
    }

    /**
     * Fire [event] at all registered listeners. Returns immediately;
     * listener work runs concurrently in child coroutines.
     */
    suspend fun emit(event: GameEvent) {
        val snapshot = mutex.withLock { listeners.toList() }
        coroutineScope {
            for (listener in snapshot) {
                launch {
                    try {
                        listener(event)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.warn("Listener threw for event={} player={}: {}", event.type, event.playerId, e.message)
                    }
                }
            }
        }
    }
}

data class GameEvent(
    val playerId: String,
    val type: AchievementEvent,
    val value: Long = 1L
)

/**
 * Wires the achievement service, challenge service, and
 * notification service to the [GameEventBus]. The wiring is
 * centralised here so a single method on the application
 * bootstrap attaches all the observers.
 */
class GameEventWiring(
    private val bus: GameEventBus,
    private val playerService: PlayerService,
    private val achievementService: AchievementService,
    private val challengeService: ChallengeService,
    private val notificationService: NotificationService,
    private val skillService: SkillService
) {
    /**
     * Connection registry for real-time push. Set after construction
     * by [com.zeroday.ZeroDayServer.configure] since the registry is
     * created after the service registry.
     */
    var connectionRegistry: com.zeroday.handler.ConnectionRegistry? = null

    fun install() {
        bus.onEvent { event ->
            val player = playerService.getPlayer(event.playerId) ?: return@onEvent
            // Achievements — coalesce to a single notification per burst so
            // a single command that completes several achievements does
            // not flood the inbox.
            val achUpdates = achievementService.record(player, event.type, event.value)
            val completed = achUpdates.filter { it.completed }
            if (completed.size == 1) {
                val def = achievementService.get(completed[0].achievementId) ?: return@onEvent
                notificationService.push(
                    player = player,
                    type = NotificationType.ACHIEVEMENT_UNLOCKED,
                    title = "Achievement unlocked: ${def.name}",
                    message = def.description,
                    data = mapOf("achievement_id" to def.id, "unclaimed" to "true")
                )
            } else if (completed.size > 1) {
                val names = completed.mapNotNull { achievementService.get(it.achievementId)?.name }
                // Pre-set the count to the actual number of completions
                // so the first emit shows the right total even before any
                // subsequent merge.
                val data = mapOf(
                    "ids" to completed.joinToString(",") { it.achievementId },
                    "unclaimed" to "true",
                    "count" to completed.size.toString()
                )
                notificationService.push(
                    player = player,
                    type = NotificationType.ACHIEVEMENT_UNLOCKED,
                    title = "${completed.size} achievements unlocked",
                    message = names.joinToString(", "),
                    data = data
                )
            }
            // Daily/weekly challenges — coalesce in the same way
            val chCompleted = challengeService.record(player, event.type, event.value)
            if (chCompleted.size == 1) {
                val def = challengeService.get(chCompleted[0].challengeId) ?: return@onEvent
                notificationService.push(
                    player = player,
                    type = NotificationType.CHALLENGE_COMPLETED,
                    title = "Challenge complete: ${def.name}",
                    message = "${def.description} Claim to earn ${def.rewardCredits} credits and ${def.rewardXp} XP.",
                    data = mapOf("challenge_id" to def.id, "unclaimed" to "true")
                )
            } else if (chCompleted.size > 1) {
                notificationService.push(
                    player = player,
                    type = NotificationType.CHALLENGE_COMPLETED,
                    title = "${chCompleted.size} challenges completed",
                    message = chCompleted.joinToString(", ") { challengeService.get(it.challengeId)?.name ?: "?" },
                    data = mapOf("unclaimed" to "true", "count" to chCompleted.size.toString())
                )
            }

            // Push real-time update to the client if connected.
            val reg = connectionRegistry
            if (reg != null) {
                val pushType = when (event.type) {
                    com.zeroday.model.AchievementEvent.LEVEL_REACHED -> com.zeroday.protocol.ResponseTypes.PUSH_RESOURCE_UPDATE
                    com.zeroday.model.AchievementEvent.CREDITS_EARNED -> com.zeroday.protocol.ResponseTypes.PUSH_RESOURCE_UPDATE
                    else -> if (completed.isNotEmpty() || chCompleted.isNotEmpty()) {
                        com.zeroday.protocol.ResponseTypes.PUSH_ACHIEVEMENT
                    } else null
                }
                if (pushType != null) {
                    val pushEnvelope = mapOf<String, Any?>(
                        "type" to pushType,
                        "playerId" to event.playerId,
                        "achievements" to completed.mapNotNull { achievementService.get(it.achievementId)?.name },
                        "challenges" to chCompleted.mapNotNull { challengeService.get(it.challengeId)?.name }
                    )
                    reg.sendToPlayer(event.playerId, pushEnvelope)
                }
            }
        }
    }
}
