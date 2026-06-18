using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

public class CraftingRecipe
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("result_type")]
    public ItemType ResultType { get; set; }

    [JsonPropertyName("result_name")]
    public string ResultName { get; set; } = "";

    [JsonPropertyName("result_rarity")]
    public ItemRarity ResultRarity { get; set; }

    [JsonPropertyName("ingredients")]
    public List<Ingredient> Ingredients { get; set; } = new();

    [JsonPropertyName("crafting_time_ms")]
    public long CraftingTimeMs { get; set; } = 10000;

    [JsonPropertyName("required_level")]
    public int RequiredLevel { get; set; } = 1;

    [JsonPropertyName("required_commands")]
    public List<string> RequiredCommands { get; set; } = new();

    [JsonPropertyName("xp_reward")]
    public long XpReward { get; set; } = 100;

    [JsonPropertyName("result_description")]
    public string ResultDescription { get; set; } = "";
}

public class Ingredient
{
    [JsonPropertyName("item_type")]
    public ItemType ItemType { get; set; }

    [JsonPropertyName("quantity")]
    public int Quantity { get; set; } = 1;

    [JsonPropertyName("min_rarity")]
    public ItemRarity MinRarity { get; set; } = ItemRarity.COMMON;

    [JsonPropertyName("credits_cost")]
    public long CreditsCost { get; set; } = 0;
}

public class ResearchProgress
{
    [JsonPropertyName("recipe_id")]
    public string RecipeId { get; set; } = "";

    [JsonPropertyName("started_at")]
    public long StartedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [JsonPropertyName("completed")]
    public bool Completed { get; set; } = false;

    [JsonPropertyName("claimed")]
    public bool Claimed { get; set; } = false;
}

public static class CraftingRecipes
{
    public static List<CraftingRecipe> AllRecipes { get; } = new()
    {
        new CraftingRecipe
        {
            Id = "fragment_to_crypt", Name = "Fragment Compression",
            Description = "Compress raw knowledge fragments into a reusable cryptographic key",
            ResultType = ItemType.CRYPT_KEY, ResultName = "Encrypted Cipher Key", ResultRarity = ItemRarity.UNCOMMON,
            Ingredients = new() { new Ingredient { ItemType = ItemType.KNOWLEDGE_FRAGMENT, Quantity = 3 } },
            CraftingTimeMs = 8000, RequiredLevel = 2, XpReward = 50,
            ResultDescription = "A reusable key that boosts decrypt command strength"
        },
        new CraftingRecipe
        {
            Id = "fragment_to_token", Name = "Access Token Fabrication",
            Description = "Forge an access token from network intelligence fragments",
            ResultType = ItemType.ACCESS_TOKEN, ResultName = "Skeleton Admin Token", ResultRarity = ItemRarity.RARE,
            Ingredients = new()
            {
                new Ingredient { ItemType = ItemType.KNOWLEDGE_FRAGMENT, Quantity = 5 },
                new Ingredient { ItemType = ItemType.CRYPT_KEY, Quantity = 1, MinRarity = ItemRarity.UNCOMMON }
            },
            CraftingTimeMs = 15000, RequiredLevel = 4, RequiredCommands = new() { "decrypt" }, XpReward = 150,
            ResultDescription = "Grants temporary admin access to any discovered node"
        },
        new CraftingRecipe
        {
            Id = "zero_day_basic", Name = "Basic Zero-Day Exploit",
            Description = "Craft a single-use zero-day exploit from vulnerability research",
            ResultType = ItemType.ZERO_DAY_EXPLOIT, ResultName = "CVE-2024-Local", ResultRarity = ItemRarity.RARE,
            Ingredients = new()
            {
                new Ingredient { ItemType = ItemType.KNOWLEDGE_FRAGMENT, Quantity = 8 },
                new Ingredient { ItemType = ItemType.ACCESS_TOKEN, Quantity = 1, MinRarity = ItemRarity.UNCOMMON }
            },
            CraftingTimeMs = 30000, RequiredLevel = 6, RequiredCommands = new() { "exploit" }, XpReward = 300,
            ResultDescription = "Bypasses security level ≤5 on any target (single use)"
        },
        new CraftingRecipe
        {
            Id = "zero_day_advanced", Name = "Advanced Zero-Day Exploit",
            Description = "Combine rare fragments and crypt keys for a powerful exploit",
            ResultType = ItemType.ZERO_DAY_EXPLOIT, ResultName = "CVE-2024-Remote-Code-Execution", ResultRarity = ItemRarity.EPIC,
            Ingredients = new()
            {
                new Ingredient { ItemType = ItemType.KNOWLEDGE_FRAGMENT, Quantity = 15 },
                new Ingredient { ItemType = ItemType.CRYPT_KEY, Quantity = 3, MinRarity = ItemRarity.RARE },
                new Ingredient { ItemType = ItemType.MALWARE_PAYLOAD, Quantity = 1, MinRarity = ItemRarity.UNCOMMON }
            },
            CraftingTimeMs = 60000, RequiredLevel = 10, RequiredCommands = new() { "exploit", "backdoor" }, XpReward = 800,
            ResultDescription = "Bypasses security level ≤10 on any target (single use)"
        },
        new CraftingRecipe
        {
            Id = "zero_day_legendary", Name = "Legendary Zero-Day Exploit",
            Description = "The ultimate weapon - bypasses any security, leaves no trace",
            ResultType = ItemType.ZERO_DAY_EXPLOIT, ResultName = "CVE-2024-God-Mode", ResultRarity = ItemRarity.LEGENDARY,
            Ingredients = new()
            {
                new Ingredient { ItemType = ItemType.KNOWLEDGE_FRAGMENT, Quantity = 50 },
                new Ingredient { ItemType = ItemType.CRYPT_KEY, Quantity = 5, MinRarity = ItemRarity.EPIC },
                new Ingredient { ItemType = ItemType.ZERO_DAY_EXPLOIT, Quantity = 2, MinRarity = ItemRarity.EPIC },
                new Ingredient { ItemType = ItemType.ACCESS_TOKEN, Quantity = 3, MinRarity = ItemRarity.EPIC }
            },
            CraftingTimeMs = 180000, RequiredLevel = 16, RequiredCommands = new() { "exploit", "backdoor", "rootkit" }, XpReward = 2500,
            ResultDescription = "Bypasses ALL security. Permanent access granted. No trace."
        },
        new CraftingRecipe
        {
            Id = "malware_payload", Name = "Custom Malware Payload",
            Description = "Engineer a custom malware payload from research data",
            ResultType = ItemType.MALWARE_PAYLOAD, ResultName = "RAT v3.0 Custom", ResultRarity = ItemRarity.UNCOMMON,
            Ingredients = new() { new Ingredient { ItemType = ItemType.KNOWLEDGE_FRAGMENT, Quantity = 4 } },
            CraftingTimeMs = 12000, RequiredLevel = 3, RequiredCommands = new() { "backdoor" }, XpReward = 100,
            ResultDescription = "A reusable payload for backdoor and worm commands"
        },
        new CraftingRecipe
        {
            Id = "firewall_patch", Name = "Firewall Hardening Patch",
            Description = "Create a defensive patch that boosts firewall command effectiveness",
            ResultType = ItemType.FIREWALL_PATCH, ResultName = "ICE-Breaker Patch", ResultRarity = ItemRarity.RARE,
            Ingredients = new()
            {
                new Ingredient { ItemType = ItemType.KNOWLEDGE_FRAGMENT, Quantity = 6 },
                new Ingredient { ItemType = ItemType.DECOY_DATA, Quantity = 1, MinRarity = ItemRarity.UNCOMMON }
            },
            CraftingTimeMs = 20000, RequiredLevel = 5, RequiredCommands = new() { "firewall" }, XpReward = 200,
            ResultDescription = "Blocks 3 additional trace attempts on your system"
        },
        new CraftingRecipe
        {
            Id = "decoy_data", Name = "Generate Decoy Data",
            Description = "Create convincing decoy files to mislead追踪ers",
            ResultType = ItemType.DECOY_DATA, ResultName = "Fake Credentials Bundle", ResultRarity = ItemRarity.COMMON,
            Ingredients = new() { new Ingredient { ItemType = ItemType.KNOWLEDGE_FRAGMENT, Quantity = 2 } },
            CraftingTimeMs = 5000, RequiredLevel = 1, XpReward = 25,
            ResultDescription = "Deploy decoy to waste attacker time and resources"
        }
    };

    public static List<CraftingRecipe> GetAvailableRecipes(int level, HashSet<string> unlockedCommands) =>
        AllRecipes.Where(recipe =>
            recipe.RequiredLevel <= level &&
            recipe.RequiredCommands.All(c => unlockedCommands.Contains(c))
        ).ToList();
}
