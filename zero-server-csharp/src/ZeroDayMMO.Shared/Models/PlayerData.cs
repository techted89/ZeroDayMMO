using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

public class PlayerData
{
    public string Id { get; set; } = string.Empty;
    public string Username { get; set; } = string.Empty;
    public string PasswordHash { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public int Level { get; set; } = 1;
    public long Experience { get; set; } = 0;
    public double Cpu { get; set; } = 10.0;
    public double MaxCpu { get; set; } = 10.0;
    public double Ram { get; set; } = 100.0;
    public double MaxRam { get; set; } = 100.0;
    public double Network { get; set; } = 50.0;
    public double MaxNetwork { get; set; } = 50.0;
    public long Credits { get; set; } = 0;
    public List<InventoryItem> Inventory { get; set; } = [];
    public List<string> UnlockedCommands { get; set; } = [];
    public long LastLogin { get; set; } = 0;
    public long CreatedAt { get; set; } = 0;
}

public class InventoryItem
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public int Quantity { get; set; } = 1;

    [JsonPropertyName("item_type")]
    public string ItemType { get; set; } = "generic";
}
