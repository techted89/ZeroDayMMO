using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;
using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Handlers;

public class SkillHandler : IHandler
{
    private readonly IPlayerService _playerService;
    private readonly SkillService _skillService;

    public string MessageType => "get_skill_tree";

    public SkillHandler(IPlayerService playerService, SkillService skillService)
    {
        _playerService = playerService;
        _skillService = skillService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        if (payload is null)
            return new MessageResult("error", connectionId, new { message = "Missing payload" });

        var type = payload.Value.Str("type") ?? "get_skill_tree";

        return type switch
        {
            "get_skill_tree" => await Tree(connectionId),
            "unlock_skill" => await Unlock(connectionId, payload.Value),
            _ => new MessageResult("error", connectionId, new { message = $"Unsupported skill request: {type}" })
        };
    }

    private async Task<IActionResult> Tree(string connectionId)
    {
        var player = _playerService.GetPlayer(connectionId);
        if (player is null)
            return new MessageResult("error", connectionId, new { message = "Player not found" });

        var m = _skillService.GetEffectiveMultipliers(player);
        var grouped = Enum.GetValues<SkillTree>().ToDictionary(tree => tree, tree => _skillService.ByTree(tree));

        var trees = grouped.Select(kvp =>
        {
            var tree = kvp.Key;
            var defs = kvp.Value;
            return new
            {
                tree = tree.ToString(),
                skills = defs.Select(def =>
                {
                    var unlocked = player.UnlockedSkills.Contains(def.Id);
                    var canUnlock = !unlocked &&
                        player.SkillPoints >= def.Cost &&
                        def.Prerequisites.All(p => player.UnlockedSkills.Contains(p));

                    return new
                    {
                        id = def.Id,
                        name = def.Name,
                        description = def.Description,
                        tier = def.Tier,
                        cost = def.Cost,
                        prerequisites = def.Prerequisites,
                        unlocked,
                        can_unlock = canUnlock,
                        effects = def.Effects.Select(EffectParams).ToList()
                    };
                }).ToList()
            };
        }).ToList();

        return new MessageResult("skill_tree", connectionId, new
        {
            trees,
            skill_points = player.SkillPoints,
            unlocked_count = player.UnlockedSkills.Count,
            multipliers = new
            {
                credits_earned = m.CreditsEarned,
                xp_earned = m.XpEarned,
                resource_cost = m.ResourceCost,
                regen_rate = m.RegenRate,
                bonus_cpu = m.BonusCpu,
                bonus_ram = m.BonusRam,
                bonus_bandwidth = m.BonusBandwidth
            }
        });
    }

    private async Task<IActionResult> Unlock(string connectionId, JsonElement payload)
    {
        string skillId;
        try
        {
            skillId = payload.ReqStr("skill_id");
        }
        catch
        {
            return new MessageResult("error", connectionId, new { message = "Skill ID required" });
        }

        var player = _playerService.GetPlayer(connectionId);
        if (player is null)
            return new MessageResult("error", connectionId, new { message = "Player not found" });

        var result = _skillService.Unlock(player, skillId);

        return result switch
        {
            UnlockResult.Success s => new MessageResult("skill_unlocked", connectionId, new
            {
                skill_id = s.Skill.Id,
                name = s.Skill.Name,
                skill_points_remaining = player.SkillPoints
            }),
            UnlockResult.AlreadyOwned a => new MessageResult("skill_unlocked", connectionId, new
            {
                skill_id = a.Skill.Id,
                name = a.Skill.Name,
                already_owned = true,
                skill_points_remaining = player.SkillPoints
            }),
            UnlockResult.InsufficientPoints i => new MessageResult("error", connectionId, new
            {
                message = $"Need {i.Cost} skill points (have {i.Available})"
            }),
            UnlockResult.MissingPrereqs m => new MessageResult("error", connectionId, new
            {
                message = $"Missing prerequisite skills: {string.Join(", ", m.Missing)}"
            }),
            UnlockResult.Unknown u => new MessageResult("error", connectionId, new
            {
                message = $"Unknown skill: {u.Id}"
            }),
            _ => new MessageResult("error", connectionId, new { message = "Unknown error" })
        };
    }

    private static object EffectParams(SkillEffect effect) => effect switch
    {
        MultiplyCreditsEarned mc => new { kind = "multiply_credits_earned", factor = mc.Factor },
        MultiplyXpEarned mx => new { kind = "multiply_xp_earned", factor = mx.Factor },
        AddMaxResource ar => new { kind = "add_max_resource", cpu = ar.Cpu, ram = ar.Ram, bandwidth = ar.Bandwidth },
        ReduceResourceCost rc => new { kind = "reduce_resource_cost", factor = rc.Factor },
        IncreaseRegenRate ir => new { kind = "increase_regen_rate", factor = ir.Factor },
        _ => new { kind = "unknown" }
    };
}
