package com.zeroday.service

import com.zeroday.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EventService(
    private val playerService: PlayerService,
    private val gameEventBus: GameEventBus? = null
) {
    private val playerEventProgress = mutableMapOf<String, MutableMap<String, Int>>()
    private val mutex = Mutex()

    suspend fun getAvailableStorylines(playerId: String): List<StoryEvent> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock emptyList()
        val storyOrder = StorylineRegistry.storylineOrder

        if (player.completedStorylines.isEmpty() && player.currentStoryline == null) {
            val firstEvent = StorylineRegistry.storylines["intro_welcome"] ?: return@withLock emptyList()
            playerService.setStoryline(playerId, firstEvent.id)
            return@withLock listOf(firstEvent)
        }

        storyOrder.mapNotNull { id -> StorylineRegistry.storylines[id] }
            .filter { event ->
                event.id !in player.completedStorylines &&
                event.requiredLevel <= player.level &&
                event.requiredCommands.all { it in player.unlockedCommands }
            }
    }

    suspend fun getCurrentStoryline(playerId: String): StoryEvent? = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock null
        val storylineId = player.currentStoryline ?: return@withLock null
        StorylineRegistry.storylines[storylineId]
    }

    suspend fun startStoryline(playerId: String, storylineId: String): Result<StoryEvent> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val story = StorylineRegistry.storylines[storylineId]
            ?: return@withLock Result.failure(Exception("Storyline not found"))

        if (player.level < story.requiredLevel) {
            return@withLock Result.failure(Exception("Level ${story.requiredLevel} required"))
        }

        if (story.requiredCommands.any { it !in player.unlockedCommands }) {
            return@withLock Result.failure(Exception("Required commands: ${story.requiredCommands.joinToString(", ")}"))
        }

        if (storylineId in player.completedStorylines) {
            return@withLock Result.failure(Exception("Storyline already completed"))
        }

        playerService.setStoryline(playerId, storylineId)
        playerEventProgress.getOrPut(playerId) { mutableMapOf() }[storylineId] = 0
        Result.success(story)
    }

    suspend fun advanceStoryStage(playerId: String): Result<StoryEvent> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val storyId = player.currentStoryline ?: return@withLock Result.failure(Exception("No active storyline"))
        val story = StorylineRegistry.storylines[storyId] ?: return@withLock Result.failure(Exception("Storyline not found"))

        val progress = playerEventProgress.getOrPut(playerId) { mutableMapOf() }.getOrPut(storyId) { 0 }
        val nextStage = progress + 1

        if (nextStage >= story.stages.size) {
            playerService.completeStoryline(playerId, storyId)
            applyRewards(playerId, story.rewards)
            story.networkAccessGranted.forEach { network ->
                playerService.grantNetworkAccess(playerId, listOf(network))
            }
            story.newCommandsUnlocked.forEach { cmd ->
                playerService.unlockCommand(playerId, cmd)
            }
            if (gameEventBus != null) {
                gameEventBus.emit(GameEvent(playerId, com.zeroday.model.AchievementEvent.STORYLINE_COMPLETED, 1L))
            }

            val nextId = story.nextEventId
            if (nextId != null) {
                val nextStory = StorylineRegistry.storylines[nextId]
                if (nextStory != null && player.level >= nextStory.requiredLevel) {
                    playerService.setStoryline(playerId, nextId)
                    playerEventProgress[playerId]?.set(nextId, 0)
                    return@withLock Result.success(nextStory)
                }
            }

            playerEventProgress[playerId]?.remove(storyId)
            return@withLock Result.success(story)
        }

        playerEventProgress[playerId]?.set(storyId, nextStage)
        playerService.advanceStoryline(playerId)
        story.stages.getOrNull(nextStage)?.let { stage ->
        }
        Result.success(story)
    }

    suspend fun getCurrentStage(playerId: String): EventStage? = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock null
        val storyId = player.currentStoryline ?: return@withLock null
        val story = StorylineRegistry.storylines[storyId] ?: return@withLock null
        val progress = playerEventProgress[playerId]?.get(storyId) ?: 0
        story.stages.getOrNull(progress)
    }

    private suspend fun applyRewards(playerId: String, rewards: EventRewards) {
        playerService.addExperience(playerId, rewards.experience)
        playerService.addCredits(playerId, rewards.credits)
        playerService.addReputation(playerId, rewards.reputation)
        playerService.upgradeResources(playerId, rewards.cpuUpgrade, rewards.ramUpgrade, rewards.bandwidthUpgrade)
    }

    suspend fun generateDynamicEvent(playerId: String): DynamicEvent? {
        val player = playerService.getPlayer(playerId) ?: return null
        if (player.level < 3) return null

        val eventTemplates = listOf(
            DynamicEvent(
                "intrusion_alert",
                "INTRUSION DETECTED",
                "An unauthorized access attempt has been detected on your network!",
                DynamicEventType.ALERT,
                player.level.coerceAtMost(10),
                listOf("firewall", "trace"),
                mapOf("source_ip" to "${(1..223).random()}.${(0..255).random()}.${(0..255).random()}.${(1..254).random()}"),
                mapOf("experience" to 50L * player.level, "credits" to 100L * player.level)
            ),
            DynamicEvent(
                "data_windfall",
                "DATA WINDFALL",
                "You intercepted a data packet containing valuable information! Decrypt it to earn rewards.",
                DynamicEventType.OPPORTUNITY,
                player.level.coerceAtMost(10),
                listOf("decrypt"),
                mapOf("encryption" to "AES-256", "size" to "${(1..50).random()}MB"),
                mapOf("experience" to 100L * player.level, "credits" to 200L * player.level)
            ),
            DynamicEvent(
                "contract_offer",
                "CONTRACT OFFER",
                "A shadowy figure offers you a contract. High risk, high reward.",
                DynamicEventType.CONTRACT,
                player.level.coerceAtMost(12),
                listOf("exploit", "backdoor"),
                mapOf("payout" to "${(1000..10000).random()}", "target" to "${(1..223).random()}.${(0..255).random()}.${(0..255).random()}.${(1..254).random()}"),
                mapOf("experience" to 200L * player.level, "credits" to 500L * player.level, "reputation" to 10)
            ),
            DynamicEvent(
                "rival_hacker",
                "RIVAL HACKER",
                "A rival hacker is competing for the same target! Race them to the objective.",
                DynamicEventType.COMPETITION,
                player.level.coerceAtMost(10),
                listOf("nmap", "exploit"),
                mapOf("rival" to "Ghost_${(100..999).random()}", "target" to "${(1..223).random()}.${(0..255).random()}.${(0..255).random()}.${(1..254).random()}"),
                mapOf("experience" to 150L * player.level, "credits" to 300L * player.level, "reputation" to 15)
            ),
            DynamicEvent(
                "system_breach",
                "SYSTEM BREACH",
                "Your system has been breached! A worm is spreading through your files. Contain it!",
                DynamicEventType.THREAT,
                player.level.coerceAtMost(12),
                listOf("firewall", "trace", "worm"),
                mapOf("worm" to "Worm.${('A'..'Z').random()}${(1000..9999).random()}", "infected" to "${(2..8).random()}"),
                mapOf("experience" to 250L * player.level, "credits" to 0L)
            )
        )

        return eventTemplates.randomOrNull()
    }
}

data class DynamicEvent(
    val id: String,
    val title: String,
    val description: String,
    val type: DynamicEventType,
    val level: Int,
    val relevantCommands: List<String>,
    val parameters: Map<String, String>,
    val rewards: Map<String, Any>
)

enum class DynamicEventType {
    ALERT,
    OPPORTUNITY,
    CONTRACT,
    COMPETITION,
    THREAT
}

data class EventMessage(
    val type: String,
    val payload: String
)
