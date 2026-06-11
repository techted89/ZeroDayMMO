package com.zeroday.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val type: ItemType,
    val name: String,
    val description: String,
    val quantity: Int = 1,
    val rarity: ItemRarity = ItemRarity.COMMON,
    val data: Map<String, String> = emptyMap()
)

@Serializable
enum class ItemType {
    KNOWLEDGE_FRAGMENT,
    ZERO_DAY_EXPLOIT,
    CRYPT_KEY,
    ACCESS_TOKEN,
    MALWARE_PAYLOAD,
    DECOY_DATA,
    FIREWALL_PATCH
}

@Serializable
enum class ItemRarity(val displayName: String, val color: String, val multiplier: Double) {
    COMMON("Common", "#808080", 1.0),
    UNCOMMON("Uncommon", "#00FF00", 1.5),
    RARE("Rare", "#4488FF", 2.5),
    EPIC("Epic", "#AA44FF", 4.0),
    LEGENDARY("Legendary", "#FF8800", 8.0)
}

fun InventoryItem.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "type" to type.name,
    "name" to name,
    "description" to description,
    "quantity" to quantity,
    "rarity" to rarity.name,
    "data" to data
)

@Serializable
data class CraftingRecipe(
    val id: String,
    val name: String,
    val description: String,
    val resultType: ItemType,
    val resultName: String,
    val resultRarity: ItemRarity,
    val ingredients: List<Ingredient>,
    val craftingTimeMs: Long = 10_000L,
    val requiredLevel: Int = 1,
    val requiredCommands: List<String> = emptyList(),
    val xpReward: Long = 100,
    val resultDescription: String = ""
)

@Serializable
data class Ingredient(
    val itemType: ItemType,
    val quantity: Int = 1,
    val minRarity: ItemRarity = ItemRarity.COMMON,
    val creditsCost: Long = 0
)

@Serializable
data class ResearchProgress(
    val recipeId: String,
    val startedAt: Long = System.currentTimeMillis(),
    var completed: Boolean = false,
    var claimed: Boolean = false
)

object CraftingRecipes {
    val allRecipes: List<CraftingRecipe> = listOf(
        CraftingRecipe(
            "fragment_to_crypt", "Fragment Compression",
            "Compress raw knowledge fragments into a reusable cryptographic key",
            ItemType.CRYPT_KEY, "Encrypted Cipher Key", ItemRarity.UNCOMMON,
            listOf(Ingredient(ItemType.KNOWLEDGE_FRAGMENT, 3)),
            8000L, 2, xpReward = 50,
            resultDescription = "A reusable key that boosts decrypt command strength"
        ),
        CraftingRecipe(
            "fragment_to_token", "Access Token Fabrication",
            "Forge an access token from network intelligence fragments",
            ItemType.ACCESS_TOKEN, "Skeleton Admin Token", ItemRarity.RARE,
            listOf(Ingredient(ItemType.KNOWLEDGE_FRAGMENT, 5), Ingredient(ItemType.CRYPT_KEY, 1, ItemRarity.UNCOMMON)),
            15000L, 4, listOf("decrypt"), xpReward = 150,
            resultDescription = "Grants temporary admin access to any discovered node"
        ),
        CraftingRecipe(
            "zero_day_basic", "Basic Zero-Day Exploit",
            "Craft a single-use zero-day exploit from vulnerability research",
            ItemType.ZERO_DAY_EXPLOIT, "CVE-2024-Local", ItemRarity.RARE,
            listOf(Ingredient(ItemType.KNOWLEDGE_FRAGMENT, 8), Ingredient(ItemType.ACCESS_TOKEN, 1, ItemRarity.UNCOMMON)),
            30000L, 6, listOf("exploit"), xpReward = 300,
            resultDescription = "Bypasses security level ≤5 on any target (single use)"
        ),
        CraftingRecipe(
            "zero_day_advanced", "Advanced Zero-Day Exploit",
            "Combine rare fragments and crypt keys for a powerful exploit",
            ItemType.ZERO_DAY_EXPLOIT, "CVE-2024-Remote-Code-Execution", ItemRarity.EPIC,
            listOf(Ingredient(ItemType.KNOWLEDGE_FRAGMENT, 15), Ingredient(ItemType.CRYPT_KEY, 3, ItemRarity.RARE),
                   Ingredient(ItemType.MALWARE_PAYLOAD, 1, ItemRarity.UNCOMMON)),
            60000L, 10, listOf("exploit", "backdoor"), xpReward = 800,
            resultDescription = "Bypasses security level ≤10 on any target (single use)"
        ),
        CraftingRecipe(
            "zero_day_legendary", "Legendary Zero-Day Exploit",
            "The ultimate weapon - bypasses any security, leaves no trace",
            ItemType.ZERO_DAY_EXPLOIT, "CVE-2024-God-Mode", ItemRarity.LEGENDARY,
            listOf(Ingredient(ItemType.KNOWLEDGE_FRAGMENT, 50), Ingredient(ItemType.CRYPT_KEY, 5, ItemRarity.EPIC),
                   Ingredient(ItemType.ZERO_DAY_EXPLOIT, 2, ItemRarity.EPIC), Ingredient(ItemType.ACCESS_TOKEN, 3, ItemRarity.EPIC)),
            180000L, 16, listOf("exploit", "backdoor", "rootkit"), xpReward = 2500,
            resultDescription = "Bypasses ALL security. Permanent access granted. No trace."
        ),
        CraftingRecipe(
            "malware_payload", "Custom Malware Payload",
            "Engineer a custom malware payload from research data",
            ItemType.MALWARE_PAYLOAD, "RAT v3.0 Custom", ItemRarity.UNCOMMON,
            listOf(Ingredient(ItemType.KNOWLEDGE_FRAGMENT, 4)),
            12000L, 3, listOf("backdoor"), xpReward = 100,
            resultDescription = "A reusable payload for backdoor and worm commands"
        ),
        CraftingRecipe(
            "firewall_patch", "Firewall Hardening Patch",
            "Create a defensive patch that boosts firewall command effectiveness",
            ItemType.FIREWALL_PATCH, "ICE-Breaker Patch", ItemRarity.RARE,
            listOf(Ingredient(ItemType.KNOWLEDGE_FRAGMENT, 6), Ingredient(ItemType.DECOY_DATA, 1, ItemRarity.UNCOMMON)),
            20000L, 5, listOf("firewall"), xpReward = 200,
            resultDescription = "Blocks 3 additional trace attempts on your system"
        ),
        CraftingRecipe(
            "decoy_data", "Generate Decoy Data",
            "Create convincing decoy files to mislead追踪ers",
            ItemType.DECOY_DATA, "Fake Credentials Bundle", ItemRarity.COMMON,
            listOf(Ingredient(ItemType.KNOWLEDGE_FRAGMENT, 2)),
            5000L, 1, xpReward = 25,
            resultDescription = "Deploy decoy to waste attacker time and resources"
        )
    )

    fun getAvailableRecipes(level: Int, unlockedCommands: Set<String>): List<CraftingRecipe> =
        allRecipes.filter { recipe ->
            recipe.requiredLevel <= level &&
            recipe.requiredCommands.all { it in unlockedCommands }
        }
}
