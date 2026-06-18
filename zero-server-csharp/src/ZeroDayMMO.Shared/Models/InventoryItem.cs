using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum ItemType
{
    [JsonPropertyName("knowlege_fragment")]
    KNOWLEDGE_FRAGMENT,
    [JsonPropertyName("zero_day_exploit")]
    ZERO_DAY_EXPLOIT,
    [JsonPropertyName("crypt_key")]
    CRYPT_KEY,
    [JsonPropertyName("access_token")]
    ACCESS_TOKEN,
    [JsonPropertyName("malware_payload")]
    MALWARE_PAYLOAD,
    [JsonPropertyName("decoy_data")]
    DECOY_DATA,
    [JsonPropertyName("firewall_patch")]
    FIREWALL_PATCH
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum ItemRarity
{
    [JsonPropertyName("common")]
    COMMON,
    [JsonPropertyName("uncommon")]
    UNCOMMON,
    [JsonPropertyName("rare")]
    RARE,
    [JsonPropertyName("epic")]
    EPIC,
    [JsonPropertyName("legendary")]
    LEGENDARY
}

public static class ItemRarityExtensions
{
    public static string DisplayName(this ItemRarity rarity) => rarity switch
    {
        ItemRarity.COMMON => "Common",
        ItemRarity.UNCOMMON => "Uncommon",
        ItemRarity.RARE => "Rare",
        ItemRarity.EPIC => "Epic",
        ItemRarity.LEGENDARY => "Legendary",
        _ => "Common"
    };

    public static string Color(this ItemRarity rarity) => rarity switch
    {
        ItemRarity.COMMON => "#808080",
        ItemRarity.UNCOMMON => "#00FF00",
        ItemRarity.RARE => "#4488FF",
        ItemRarity.EPIC => "#AA44FF",
        ItemRarity.LEGENDARY => "#FF8800",
        _ => "#808080"
    };

    public static double Multiplier(this ItemRarity rarity) => rarity switch
    {
        ItemRarity.COMMON => 1.0,
        ItemRarity.UNCOMMON => 1.5,
        ItemRarity.RARE => 2.5,
        ItemRarity.EPIC => 4.0,
        ItemRarity.LEGENDARY => 8.0,
        _ => 1.0
    };
}

public class InventoryItem
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString();

    [JsonPropertyName("type")]
    public ItemType Type { get; set; }

    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("quantity")]
    public int Quantity { get; set; } = 1;

    [JsonPropertyName("rarity")]
    public ItemRarity Rarity { get; set; } = ItemRarity.COMMON;

    [JsonPropertyName("data")]
    public Dictionary<string, string> Data { get; set; } = new();

    public Dictionary<string, object?> ToMap() => new()
    {
        ["id"] = Id,
        ["type"] = Type.ToString(),
        ["name"] = Name,
        ["description"] = Description,
        ["quantity"] = Quantity,
        ["rarity"] = Rarity.ToString(),
        ["data"] = Data
    };
}
