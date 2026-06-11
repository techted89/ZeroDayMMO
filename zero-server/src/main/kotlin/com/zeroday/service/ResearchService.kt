package com.zeroday.service

import com.zeroday.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class ResearchService(
    private val playerService: PlayerService,
    private val gameEventBus: GameEventBus? = null
) {
    private val activeResearch = mutableMapOf<String, MutableList<ResearchProgress>>()
    private val mutex = Mutex()
    private var researchJob: Job? = null

    fun start(scope: CoroutineScope) {
        researchJob = scope.launch {
            while (isActive) {
                delay(5_000L)
                processResearchCompletion()
            }
        }
    }

    fun stop() {
        researchJob?.cancel()
    }

    suspend fun getRecipes(playerId: String): List<CraftingRecipe> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock emptyList()
        val completedRecipes = activeResearch[playerId]?.filter { it.completed }?.map { it.recipeId }?.toSet() ?: emptySet()
        CraftingRecipes.allRecipes.filter { it.id !in completedRecipes }
    }

    suspend fun getInventory(playerId: String): Result<List<InventoryItem>> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        Result.success(player.inventory.toList())
    }

    suspend fun gatherFragment(playerId: String): Result<InventoryItem> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val cost = (player.level * 5).coerceAtLeast(5)
        if (player.cpu < cost || player.ram < cost * 2) {
            return@withLock Result.failure(Exception("Insufficient resources. Need ${cost}CPU/${cost*2}RAM"))
        }
        playerService.consumeResources(playerId, cost, cost * 2)

        val rarityRoll = Math.random()
        val rarity = when {
            rarityRoll < 0.5 -> ItemRarity.COMMON
            rarityRoll < 0.8 -> ItemRarity.UNCOMMON
            rarityRoll < 0.95 -> ItemRarity.RARE
            rarityRoll < 0.99 -> ItemRarity.EPIC
            else -> ItemRarity.LEGENDARY
        }

        val fragment = InventoryItem(
            type = ItemType.KNOWLEDGE_FRAGMENT,
            name = "${rarity.displayName} Knowledge Fragment",
            description = "A fragment of digital knowledge. Rarity: ${rarity.displayName}",
            rarity = rarity,
            data = mapOf("source" to "network_scan", "level" to player.level.toString())
        )

        player.inventory.add(fragment)
        if (gameEventBus != null) {
            gameEventBus.emit(GameEvent(playerId, com.zeroday.model.AchievementEvent.FRAGMENT_GATHERED, 1L))
        }
        Result.success(fragment)
    }

    suspend fun startResearch(playerId: String, recipeId: String): Result<ResearchProgress> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val recipe = CraftingRecipes.allRecipes.find { it.id == recipeId }
            ?: return@withLock Result.failure(Exception("Recipe not found"))

        if (player.level < recipe.requiredLevel)
            return@withLock Result.failure(Exception("Level ${recipe.requiredLevel} required"))
        if (recipe.requiredCommands.any { it !in player.unlockedCommands })
            return@withLock Result.failure(Exception("Required commands: ${recipe.requiredCommands.joinToString(", ")}"))

        if (activeResearch[playerId]?.any { it.recipeId == recipeId && !it.completed } == true)
            return@withLock Result.failure(Exception("Already researching this recipe"))

        for (ingredient in recipe.ingredients) {
            var remaining = ingredient.quantity
            val iterator = player.inventory.iterator()
            while (iterator.hasNext() && remaining > 0) {
                val item = iterator.next()
                if (item.type == ingredient.itemType && item.rarity.ordinal >= ingredient.minRarity.ordinal) {
                    iterator.remove()
                    remaining--
                }
            }
            if (remaining > 0) {
                return@withLock Result.failure(Exception("Missing ingredients: ${ingredient.quantity}x ${ingredient.itemType} (min ${ingredient.minRarity.displayName})"))
            }
            if (ingredient.creditsCost > 0 && player.credits < ingredient.creditsCost) {
                return@withLock Result.failure(Exception("Need ${ingredient.creditsCost} credits"))
            }
            if (ingredient.creditsCost > 0) {
                playerService.addCredits(playerId, -ingredient.creditsCost)
            }
        }

        val progress = ResearchProgress(recipeId = recipeId)
        activeResearch.getOrPut(playerId) { mutableListOf() }.add(progress)
        Result.success(progress)
    }

    suspend fun claimResearch(playerId: String, recipeId: String): Result<InventoryItem> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val research = activeResearch[playerId]?.find { it.recipeId == recipeId && it.completed && !it.claimed }
            ?: return@withLock Result.failure(Exception("No completed research to claim"))

        val recipe = CraftingRecipes.allRecipes.find { it.id == recipeId }
            ?: return@withLock Result.failure(Exception("Recipe not found"))

        research.claimed = true
        playerService.addExperience(playerId, recipe.xpReward)

        val result = InventoryItem(
            type = recipe.resultType,
            name = recipe.resultName,
            description = recipe.resultDescription.ifEmpty { "Crafted from $recipeId" },
            rarity = recipe.resultRarity,
            data = mapOf("recipe" to recipeId, "crafted_at" to System.currentTimeMillis().toString())
        )

        player.inventory.add(result)
        Result.success(result)
    }

    suspend fun useItem(playerId: String, itemId: String): Result<String> = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock Result.failure(Exception("Player not found"))
        val item = player.inventory.find { it.id == itemId }
            ?: return@withLock Result.failure(Exception("Item not found in inventory"))

        return when (item.type) {
            ItemType.ZERO_DAY_EXPLOIT -> {
                player.activeZeroDayExploits.add(item)
                player.inventory.remove(item)
                Result.success("Zero-day exploit loaded. Use 'exploit <target> --use-zeroday' to deploy it. Bypasses security level ${getZeroDayPower(item)}.")
            }
            ItemType.ACCESS_TOKEN -> {
                player.inventory.remove(item)
                Result.success("Access token consumed. You now have temporary admin access to all discovered nodes. Use 'connect <ip>' to access any node.")
            }
            ItemType.FIREWALL_PATCH -> {
                player.firewallBoost = 3
                player.inventory.remove(item)
                Result.success("Firewall patch applied. Your firewall command will block 3 additional trace attempts.")
            }
            else -> Result.success("This item cannot be used directly. It's a crafting ingredient.")
        }
    }

    suspend fun getResearchStatus(playerId: String): List<ResearchProgress> = mutex.withLock {
        activeResearch[playerId]?.filter { !it.claimed }?.toList() ?: emptyList()
    }

    private fun getZeroDayPower(item: InventoryItem): Int = when (item.rarity) {
        ItemRarity.RARE -> 5
        ItemRarity.EPIC -> 10
        ItemRarity.LEGENDARY -> 99
        else -> 3
    }

    private suspend fun processResearchCompletion() = mutex.withLock {
        val now = System.currentTimeMillis()
        for ((playerId, researches) in activeResearch) {
            for (research in researches.filter { !it.completed }) {
                val recipe = CraftingRecipes.allRecipes.find { it.id == research.recipeId } ?: continue
                if (now - research.startedAt >= recipe.craftingTimeMs) {
                    research.completed = true
                }
            }
        }
    }

    suspend fun addItem(playerId: String, item: InventoryItem): Boolean = mutex.withLock {
        val player = playerService.getPlayer(playerId) ?: return@withLock false
        player.inventory.add(item)
        true
    }
}
