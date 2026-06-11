package com.zeroday.service

import com.zeroday.config.ServerConfig
import com.zeroday.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class TaskService(
    private val playerService: PlayerService,
    private val contractGenerator: ContractGenerator? = null,
    private val gameEventBus: GameEventBus? = null
) {
    private val availableTasks = mutableListOf<TaskInstance>()
    private val mutex = Mutex()
    private var taskGenJob: Job? = null
    private var taskCheckJob: Job? = null

    fun start(scope: CoroutineScope) {
        taskGenJob = scope.launch {
            while (isActive) {
                generateTasks()
                delay(ServerConfig.TASK_GEN_INTERVAL)
            }
        }
        taskCheckJob = scope.launch {
            while (isActive) {
                checkExpiredTasks()
                delay(10_000L)
            }
        }
        seedInitialTasks()
    }

    fun stop() {
        taskGenJob?.cancel()
        taskCheckJob?.cancel()
    }

    private fun seedInitialTasks() {
        val initialTasks = TaskTemplates.taskPool.filter { it.difficulty in listOf(TaskDifficulty.TRIVIAL, TaskDifficulty.EASY) }
        initialTasks.forEach { template ->
            availableTasks.add(template.toInstance())
        }
    }

    private suspend fun generateTasks() = mutex.withLock {
        val totalAvailable = availableTasks.count { it.status == TaskStatus.AVAILABLE }
        if (totalAvailable >= 25) return@withLock

        val onlinePlayers = playerService.getOnlinePlayers()
        val avgLevel: Int = if (onlinePlayers.isNotEmpty()) {
            onlinePlayers.map { it.level }.average().toInt()
        } else 1

        val newTaskTemplates = TaskTemplates.taskPool
            .filter { template ->
                val diffLevel: IntRange = when (template.difficulty) {
                    TaskDifficulty.TRIVIAL -> 1..3
                    TaskDifficulty.EASY -> 1..5
                    TaskDifficulty.MEDIUM -> 3..8
                    TaskDifficulty.HARD -> 6..12
                    TaskDifficulty.EXPERT -> 10..16
                    TaskDifficulty.LEGENDARY -> 15..20
                }
                avgLevel in diffLevel && availableTasks.none { existing -> existing.taskId == template.id }
            }
            .shuffled()
            .take(2)

        newTaskTemplates.forEach { template ->
            availableTasks.add(template.toInstance())
        }

        if (contractGenerator != null && availableTasks.size < 20) {
            repeat(2) {
                if (onlinePlayers.isNotEmpty()) {
                    val samplePlayer = onlinePlayers.random()
                    val contract = contractGenerator.generateContract(samplePlayer.id)
                    if (contract != null) {
                        val instance = TaskInstance(
                            taskId = "contract_${contract.id}",
                            instanceId = "task_${contract.id.take(8)}",
                            title = contract.title,
                            description = contract.description,
                            difficulty = contract.difficulty,
                            targetType = contract.targetType,
                            targetIp = contract.targetIp,
                            objective = "Complete contract: ${contract.title}",
                            timeLimitMs = contract.timeLimitMs,
                            rewards = TaskRewards(
                                experience = contract.finalXp,
                                credits = contract.finalCredits,
                                reputation = ((contract.finalCredits / 100)).toInt().coerceAtMost(50)
                            )
                        )
                        if (availableTasks.none { it.taskId == instance.taskId }) {
                            availableTasks.add(instance)
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkExpiredTasks() = mutex.withLock {
        val now = System.currentTimeMillis()
        availableTasks.removeAll { it.createdAt + it.timeLimitMs < now }
    }

    suspend fun getAvailableTasks(playerId: String): List<TaskInstance> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock emptyList()
        availableTasks
            .filter { task -> task.status == TaskStatus.AVAILABLE }
            .filter { task ->
                if (task.taskId.startsWith("contract_")) {
                    true
                } else {
                    val template = TaskTemplates.taskPool.find { it.id == task.taskId }
                    template != null && player.level >= template.requiredLevel &&
                    template.requiredCommands.all { it in player.unlockedCommands }
                }
            }
    }

    suspend fun acceptTask(playerId: String, taskInstanceId: String): Result<TaskInstance> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val task = availableTasks.find { it.instanceId == taskInstanceId }
            ?: return@withLock Result.failure(Exception("Task not found or already claimed"))

        if (task.status != TaskStatus.AVAILABLE) {
            return@withLock Result.failure(Exception("Task is no longer available"))
        }

        val template = TaskTemplates.taskPool.find { it.id == task.taskId }
            ?: return@withLock Result.failure(Exception("Task template not found"))

        if (player.level < template.requiredLevel) {
            return@withLock Result.failure(Exception("Level ${template.requiredLevel} required"))
        }

        if (template.requiredCommands.any { it !in player.unlockedCommands }) {
            return@withLock Result.failure(Exception("Required commands: ${template.requiredCommands.joinToString(", ")}"))
        }

        task.claimedBy = listOf(playerId)
        task.status = TaskStatus.IN_PROGRESS
        playerService.assignTask(playerId, task)
        Result.success(task)
    }

    suspend fun acceptTaskParty(playerIds: List<String>, taskInstanceId: String): Result<TaskInstance> = mutex.withLock {
        if (playerIds.size < 2) {
            return@withLock Result.failure(Exception("Party requires at least 2 players"))
        }

        val task = availableTasks.find { it.instanceId == taskInstanceId }
            ?: return@withLock Result.failure(Exception("Task not found"))

        val template = TaskTemplates.taskPool.find { it.id == task.taskId }
            ?: return@withLock Result.failure(Exception("Task template not found"))

        if (playerIds.size > template.maxPartySize) {
            return@withLock Result.failure(Exception("Maximum party size is ${template.maxPartySize}"))
        }

        for (playerId in playerIds) {
            val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player $playerId not found"))
            if (player.level < template.requiredLevel) {
                return@withLock Result.failure(Exception("Player ${player.username} is below level ${template.requiredLevel}"))
            }
        }

        task.claimedBy = playerIds
        task.status = TaskStatus.IN_PROGRESS
        playerIds.forEach { pid ->
            playerService.assignTask(pid, task)
            playerService.setParty(pid, taskInstanceId)
        }
        Result.success(task)
    }

    suspend fun completeTask(playerId: String, taskInstanceId: String): Result<TaskInstance> = mutex.withLock {
        val task = availableTasks.find { it.instanceId == taskInstanceId }
            ?: return@withLock Result.failure(Exception("Task not found"))

        val template = TaskTemplates.taskPool.find { it.id == task.taskId }
            ?: return@withLock Result.failure(Exception("Task template not found"))

        val isPartyLeader = task.claimedBy.firstOrNull() == playerId
        if (!isPartyLeader && task.claimedBy.size > 1) {
            return@withLock Result.failure(Exception("Only the party leader can submit completion"))
        }

        val isSolo = task.claimedBy.size <= 1
        val xpMultiplier = if (isSolo) 1.0 else ServerConfig.PARTY_EXP_BONUS

        for (memberId in task.claimedBy) {
            val member = playerService.getPlayer(memberId) ?: continue
            val levelDiff = member.level - template.requiredLevel
            val levelMultiplier = if (levelDiff <= 0) 1.0 else kotlin.math.max(0.5, 1.0 - levelDiff * 0.1)

            val finalXp = (template.rewards.experience * xpMultiplier * levelMultiplier).toLong()
            val finalCredits = template.rewards.credits

            playerService.addExperience(memberId, finalXp)
            playerService.addCredits(memberId, finalCredits)
            playerService.addReputation(memberId, template.rewards.reputation)
            playerService.upgradeResources(memberId, template.rewards.cpuUpgrade, template.rewards.ramUpgrade, template.rewards.bandwidthUpgrade)

            if (template.rewards.commandUnlock != null) {
                playerService.unlockCommand(memberId, template.rewards.commandUnlock)
            }

            playerService.completeTask(memberId, taskInstanceId)
            playerService.setParty(memberId, null)
            if (gameEventBus != null) {
                gameEventBus.emit(GameEvent(memberId, com.zeroday.model.AchievementEvent.TASK_COMPLETED, 1L))
                gameEventBus.emit(GameEvent(memberId, com.zeroday.model.AchievementEvent.CREDITS_EARNED, finalCredits))
            }
        }

        task.status = TaskStatus.COMPLETED
        task.completedBy = playerId
        availableTasks.removeAll { it.instanceId == taskInstanceId }
        Result.success(task)
    }

    suspend fun failTask(playerId: String, taskInstanceId: String): Result<TaskInstance> = mutex.withLock {
        val task = availableTasks.find { it.instanceId == taskInstanceId }
            ?: return@withLock Result.failure(Exception("Task not found"))

        task.status = TaskStatus.FAILED
        for (memberId in task.claimedBy) {
            playerService.completeTask(memberId, taskInstanceId)
            playerService.setParty(memberId, null)
        }
        availableTasks.removeAll { it.instanceId == taskInstanceId }
        Result.success(task)
    }

    suspend fun generateDynamicTask(playerId: String): TaskInstance? {
        val player = playerService.getPlayer(playerId) ?: return null
        val maxDifficulty = when {
            player.level < 3 -> TaskDifficulty.TRIVIAL
            player.level < 5 -> TaskDifficulty.EASY
            player.level < 8 -> TaskDifficulty.MEDIUM
            player.level < 12 -> TaskDifficulty.HARD
            player.level < 16 -> TaskDifficulty.EXPERT
            else -> TaskDifficulty.LEGENDARY
        }

        val suitableTemplates = TaskTemplates.taskPool.filter {
            it.difficulty.ordinal <= maxDifficulty.ordinal &&
            it.requiredLevel <= player.level &&
            it.requiredCommands.all { cmd -> cmd in player.unlockedCommands }
        }

        if (suitableTemplates.isEmpty()) return null
        val template = suitableTemplates.random()

        val instanceId = "dyn_${UUID.randomUUID().toString().take(8)}"
        val diff = when {
            player.level >= 18 && Math.random() < 0.1 -> TaskDifficulty.LEGENDARY
            player.level >= 12 && Math.random() < 0.2 -> TaskDifficulty.EXPERT
            player.level >= 8 && Math.random() < 0.3 -> TaskDifficulty.HARD
            player.level >= 4 && Math.random() < 0.3 -> TaskDifficulty.MEDIUM
            else -> TaskDifficulty.EASY
        }

        return TaskInstance(
            taskId = template.id,
            instanceId = instanceId,
            title = "${if (Math.random() < 0.3) "[PARTY] " else ""}${template.title}",
            description = template.description,
            difficulty = diff,
            targetType = template.targetType,
            targetIp = template.targetIp,
            objective = template.objective,
            timeLimitMs = (template.timeLimitMs * (1.0 + Math.random() * 0.5)).toLong(),
            rewards = template.rewards
        )
    }
}

private fun GameTask.toInstance(): TaskInstance = TaskInstance(
    taskId = this.id,
    instanceId = "task_${UUID.randomUUID().toString().take(8)}",
    title = this.title,
    description = this.description,
    difficulty = this.difficulty,
    targetType = this.targetType,
    targetIp = this.targetIp,
    objective = this.objective,
    timeLimitMs = this.timeLimitMs,
    rewards = this.rewards
)
