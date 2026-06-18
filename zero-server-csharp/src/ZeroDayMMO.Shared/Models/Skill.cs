using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum SkillTree
{
    [JsonPropertyName("offense")]
    OFFENSE,
    [JsonPropertyName("defense")]
    DEFENSE,
    [JsonPropertyName("intel")]
    INTEL,
    [JsonPropertyName("income")]
    INCOME
}

public class SkillDefinition
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("tree")]
    public SkillTree Tree { get; set; }

    [JsonPropertyName("tier")]
    public int Tier { get; set; }

    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("cost")]
    public int Cost { get; set; }

    [JsonPropertyName("prerequisites")]
    public List<string> Prerequisites { get; set; } = new();

    [JsonPropertyName("effects")]
    public List<SkillEffect> Effects { get; set; } = new();
}

[JsonPolymorphic(TypeDiscriminatorPropertyName = "kind")]
[JsonDerivedType(typeof(MultiplyCreditsEarned), typeDiscriminator: "multiply_credits_earned")]
[JsonDerivedType(typeof(MultiplyXpEarned), typeDiscriminator: "multiply_xp_earned")]
[JsonDerivedType(typeof(AddMaxResource), typeDiscriminator: "add_max_resource")]
[JsonDerivedType(typeof(ReduceResourceCost), typeDiscriminator: "reduce_resource_cost")]
[JsonDerivedType(typeof(IncreaseRegenRate), typeDiscriminator: "increase_regen_rate")]
public abstract class SkillEffect
{
    [JsonPropertyName("kind")]
    public abstract string Kind { get; }
}

public class MultiplyCreditsEarned : SkillEffect
{
    [JsonPropertyName("factor")]
    public double Factor { get; set; }

    [JsonPropertyName("kind")]
    public override string Kind => "multiply_credits_earned";
}

public class MultiplyXpEarned : SkillEffect
{
    [JsonPropertyName("factor")]
    public double Factor { get; set; }

    [JsonPropertyName("kind")]
    public override string Kind => "multiply_xp_earned";
}

public class AddMaxResource : SkillEffect
{
    [JsonPropertyName("cpu")]
    public int Cpu { get; set; } = 0;

    [JsonPropertyName("ram")]
    public int Ram { get; set; } = 0;

    [JsonPropertyName("bandwidth")]
    public int Bandwidth { get; set; } = 0;

    [JsonPropertyName("kind")]
    public override string Kind => "add_max_resource";
}

public class ReduceResourceCost : SkillEffect
{
    [JsonPropertyName("factor")]
    public double Factor { get; set; }

    [JsonPropertyName("kind")]
    public override string Kind => "reduce_resource_cost";
}

public class IncreaseRegenRate : SkillEffect
{
    [JsonPropertyName("factor")]
    public double Factor { get; set; }

    [JsonPropertyName("kind")]
    public override string Kind => "increase_regen_rate";
}

public class SkillMultipliers
{
    [JsonPropertyName("credits_earned")]
    public double CreditsEarned { get; set; } = 1.0;

    [JsonPropertyName("xp_earned")]
    public double XpEarned { get; set; } = 1.0;

    [JsonPropertyName("resource_cost")]
    public double ResourceCost { get; set; } = 1.0;

    [JsonPropertyName("regen_rate")]
    public double RegenRate { get; set; } = 1.0;

    [JsonPropertyName("bonus_cpu")]
    public int BonusCpu { get; set; } = 0;

    [JsonPropertyName("bonus_ram")]
    public int BonusRam { get; set; } = 0;

    [JsonPropertyName("bonus_bandwidth")]
    public int BonusBandwidth { get; set; } = 0;

    public SkillMultipliers Combine(SkillMultipliers other) => new()
    {
        CreditsEarned = CreditsEarned * other.CreditsEarned,
        XpEarned = XpEarned * other.XpEarned,
        ResourceCost = ResourceCost * other.ResourceCost,
        RegenRate = RegenRate * other.RegenRate,
        BonusCpu = BonusCpu + other.BonusCpu,
        BonusRam = BonusRam + other.BonusRam,
        BonusBandwidth = BonusBandwidth + other.BonusBandwidth
    };
}
