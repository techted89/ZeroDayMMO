using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public class SkillService
{
    public List<SkillDefinition> Trees { get; } = new()
    {
        // Offense
        new SkillDefinition
        {
            Id = "offense_t1_fast_scan",
            Tree = SkillTree.OFFENSE,
            Tier = 1,
            Name = "Fast Scan",
            Description = "Reduce CPU cost of `nmap` and `scan` by 15%.",
            Cost = 1,
            Effects = new List<SkillEffect> { new ReduceResourceCost { Factor = 0.85 } }
        },
        new SkillDefinition
        {
            Id = "offense_t2_crit_exploit",
            Tree = SkillTree.OFFENSE,
            Tier = 2,
            Name = "Critical Exploit",
            Description = "+25% XP from successful exploits.",
            Cost = 2,
            Prerequisites = new List<string> { "offense_t1_fast_scan" },
            Effects = new List<SkillEffect> { new MultiplyXpEarned { Factor = 1.25 } }
        },
        new SkillDefinition
        {
            Id = "offense_t3_master_hacker",
            Tree = SkillTree.OFFENSE,
            Tier = 3,
            Name = "Master Hacker",
            Description = "+50% credits from tasks; +10 CPU cap.",
            Cost = 3,
            Prerequisites = new List<string> { "offense_t2_crit_exploit" },
            Effects = new List<SkillEffect>
            {
                new MultiplyCreditsEarned { Factor = 1.5 },
                new AddMaxResource { Cpu = 10 }
            }
        },
        // Defense
        new SkillDefinition
        {
            Id = "defense_t1_thick_firewall",
            Tree = SkillTree.DEFENSE,
            Tier = 1,
            Name = "Thick Firewall",
            Description = "+20 RAM and +10 Bandwidth caps.",
            Cost = 1,
            Effects = new List<SkillEffect> { new AddMaxResource { Ram = 20, Bandwidth = 10 } }
        },
        new SkillDefinition
        {
            Id = "defense_t2_quick_repair",
            Tree = SkillTree.DEFENSE,
            Tier = 2,
            Name = "Quick Repair",
            Description = "Resource regeneration 50% faster.",
            Cost = 2,
            Prerequisites = new List<string> { "defense_t1_thick_firewall" },
            Effects = new List<SkillEffect> { new IncreaseRegenRate { Factor = 1.5 } }
        },
        new SkillDefinition
        {
            Id = "defense_t3_stealth_ops",
            Tree = SkillTree.DEFENSE,
            Tier = 3,
            Name = "Stealth Ops",
            Description = "All command resource costs reduced by 20%; +20 CPU cap.",
            Cost = 3,
            Prerequisites = new List<string> { "defense_t2_quick_repair" },
            Effects = new List<SkillEffect>
            {
                new ReduceResourceCost { Factor = 0.80 },
                new AddMaxResource { Cpu = 20 }
            }
        },
        // Intel
        new SkillDefinition
        {
            Id = "intel_t1_scholar",
            Tree = SkillTree.INTEL,
            Tier = 1,
            Name = "Scholar",
            Description = "+10% XP from research.",
            Cost = 1,
            Effects = new List<SkillEffect> { new MultiplyXpEarned { Factor = 1.10 } }
        },
        new SkillDefinition
        {
            Id = "intel_t2_linguist",
            Tree = SkillTree.INTEL,
            Tier = 2,
            Name = "Linguist",
            Description = "+20% XP from research and +1 skill point on level-up.",
            Cost = 2,
            Prerequisites = new List<string> { "intel_t1_scholar" },
            Effects = new List<SkillEffect> { new MultiplyXpEarned { Factor = 1.20 } }
        },
        new SkillDefinition
        {
            Id = "intel_t3_oracle",
            Tree = SkillTree.INTEL,
            Tier = 3,
            Name = "Oracle",
            Description = "+50% credits from research, +20 RAM cap.",
            Cost = 3,
            Prerequisites = new List<string> { "intel_t2_linguist" },
            Effects = new List<SkillEffect>
            {
                new MultiplyCreditsEarned { Factor = 1.5 },
                new AddMaxResource { Ram = 20 }
            }
        },
        // Income
        new SkillDefinition
        {
            Id = "income_t1_deal_maker",
            Tree = SkillTree.INCOME,
            Tier = 1,
            Name = "Deal Maker",
            Description = "+15% credits from tasks and ad rewards.",
            Cost = 1,
            Effects = new List<SkillEffect> { new MultiplyCreditsEarned { Factor = 1.15 } }
        },
        new SkillDefinition
        {
            Id = "income_t2_investor",
            Tree = SkillTree.INCOME,
            Tier = 2,
            Name = "Investor",
            Description = "+30% credits from tasks; +10 Bandwidth cap.",
            Cost = 2,
            Prerequisites = new List<string> { "income_t1_deal_maker" },
            Effects = new List<SkillEffect>
            {
                new MultiplyCreditsEarned { Factor = 1.30 },
                new AddMaxResource { Bandwidth = 10 }
            }
        },
        new SkillDefinition
        {
            Id = "income_t3_tycoon",
            Tree = SkillTree.INCOME,
            Tier = 3,
            Name = "Tycoon",
            Description = "+75% credits; +20 CPU, +20 RAM, +20 Bandwidth caps.",
            Cost = 3,
            Prerequisites = new List<string> { "income_t2_investor" },
            Effects = new List<SkillEffect>
            {
                new MultiplyCreditsEarned { Factor = 1.75 },
                new AddMaxResource { Cpu = 20, Ram = 20, Bandwidth = 20 }
            }
        }
    };

    private readonly Dictionary<string, SkillDefinition> _byId;

    public SkillService()
    {
        _byId = Trees.ToDictionary(s => s.Id);
    }

    public SkillDefinition? Get(string id) =>
        _byId.GetValueOrDefault(id);

    public List<SkillDefinition> ByTree(SkillTree tree) =>
        Trees.Where(s => s.Tree == tree).ToList();

    public UnlockResult Unlock(Player player, string skillId)
    {
        if (!_byId.TryGetValue(skillId, out var def))
            return new UnlockResult.Unknown(skillId);

        if (player.UnlockedSkills.Contains(skillId))
            return new UnlockResult.AlreadyOwned(def);

        if (player.SkillPoints < def.Cost)
            return new UnlockResult.InsufficientPoints(def, def.Cost, player.SkillPoints);

        var missing = def.Prerequisites.Where(p => !player.UnlockedSkills.Contains(p)).ToList();
        if (missing.Count > 0)
            return new UnlockResult.MissingPrereqs(def, missing);

        player.SkillPoints -= def.Cost;
        player.UnlockedSkills.Add(skillId);

        foreach (var effect in def.Effects)
        {
            if (effect is AddMaxResource addRes)
            {
                player.MaxCpu += addRes.Cpu;
                player.Cpu += addRes.Cpu;
                player.MaxRam += addRes.Ram;
                player.Ram += addRes.Ram;
                player.MaxBandwidth += addRes.Bandwidth;
                player.Bandwidth += addRes.Bandwidth;
            }
        }

        return new UnlockResult.Success(def);
    }

    public void GrantPoints(Player player, int points)
    {
        player.SkillPoints += points;
    }

    public SkillMultipliers GetEffectiveMultipliers(Player player)
    {
        var m = new SkillMultipliers();

        foreach (var id in player.UnlockedSkills)
        {
            if (!_byId.TryGetValue(id, out var def)) continue;

            foreach (var effect in def.Effects)
            {
                m = effect switch
                {
                    MultiplyCreditsEarned mc => new SkillMultipliers
                    {
                        CreditsEarned = m.CreditsEarned * mc.Factor,
                        XpEarned = m.XpEarned,
                        ResourceCost = m.ResourceCost,
                        RegenRate = m.RegenRate,
                        BonusCpu = m.BonusCpu,
                        BonusRam = m.BonusRam,
                        BonusBandwidth = m.BonusBandwidth
                    },
                    MultiplyXpEarned mx => new SkillMultipliers
                    {
                        CreditsEarned = m.CreditsEarned,
                        XpEarned = m.XpEarned * mx.Factor,
                        ResourceCost = m.ResourceCost,
                        RegenRate = m.RegenRate,
                        BonusCpu = m.BonusCpu,
                        BonusRam = m.BonusRam,
                        BonusBandwidth = m.BonusBandwidth
                    },
                    ReduceResourceCost rr => new SkillMultipliers
                    {
                        CreditsEarned = m.CreditsEarned,
                        XpEarned = m.XpEarned,
                        ResourceCost = m.ResourceCost * rr.Factor,
                        RegenRate = m.RegenRate,
                        BonusCpu = m.BonusCpu,
                        BonusRam = m.BonusRam,
                        BonusBandwidth = m.BonusBandwidth
                    },
                    IncreaseRegenRate ir => new SkillMultipliers
                    {
                        CreditsEarned = m.CreditsEarned,
                        XpEarned = m.XpEarned,
                        ResourceCost = m.ResourceCost,
                        RegenRate = m.RegenRate * ir.Factor,
                        BonusCpu = m.BonusCpu,
                        BonusRam = m.BonusRam,
                        BonusBandwidth = m.BonusBandwidth
                    },
                    AddMaxResource => m,
                    _ => m
                };
            }
        }

        return m;
    }
}

public abstract class UnlockResult
{
    public class Success : UnlockResult
    {
        public SkillDefinition Skill { get; }
        public Success(SkillDefinition skill) => Skill = skill;
    }

    public class AlreadyOwned : UnlockResult
    {
        public SkillDefinition Skill { get; }
        public AlreadyOwned(SkillDefinition skill) => Skill = skill;
    }

    public class InsufficientPoints : UnlockResult
    {
        public SkillDefinition Skill { get; }
        public int Cost { get; }
        public int Available { get; }
        public InsufficientPoints(SkillDefinition skill, int cost, int available)
        {
            Skill = skill;
            Cost = cost;
            Available = available;
        }
    }

    public class MissingPrereqs : UnlockResult
    {
        public SkillDefinition Skill { get; }
        public List<string> Missing { get; }
        public MissingPrereqs(SkillDefinition skill, List<string> missing)
        {
            Skill = skill;
            Missing = missing;
        }
    }

    public class Unknown : UnlockResult
    {
        public string Id { get; }
        public Unknown(string id) => Id = id;
    }
}
