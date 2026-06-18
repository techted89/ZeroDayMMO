using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public enum AchievementCategory
{
    Combat,
    Progression,
    Economy,
    Exploration,
    Social,
    Scripting,
    WorldEvent,
    Meta
}

public record AchievementDefinition(
    string Id,
    string Name,
    string Description,
    AchievementCategory Category,
    long TargetValue,
    long RewardCredits = 0,
    int RewardReputation = 0,
    long RewardXp = 0,
    bool Hidden = false
);

public record AchievementRewards(long Credits, int Reputation, long Xp)
{
    public static readonly AchievementRewards None = new(0, 0, 0);
}

public record AchievementUpdate(
    string AchievementId,
    long NewValue,
    bool Completed,
    AchievementRewards RewardsAwarded
);

public class GameEvent
{
    public string PlayerId { get; init; } = "";
    public AchievementEvent Type { get; init; }
    public long Value { get; init; } = 1;
}

public interface IGameEventBus
{
    Task Emit(GameEvent gameEvent);
}

public class AchievementService
{
    public List<AchievementDefinition> Definitions { get; } = new()
    {
        new("first_blood", "First Blood", "Compromise your first network node.", AchievementCategory.Combat, 1, 100, 0, 50),
        new("node_hunter", "Node Hunter", "Discover 25 unique network nodes.", AchievementCategory.Combat, 25, 500, 5),
        new("network_king", "Network King", "Discover 100 unique network nodes.", AchievementCategory.Combat, 100, 5000, 25, 1000),
        new("level_5", "Apprentice", "Reach level 5.", AchievementCategory.Progression, 5, 250, 0, 100),
        new("level_10", "Veteran", "Reach level 10.", AchievementCategory.Progression, 10, 1000, 10, 500),
        new("level_20", "Elite", "Reach level 20.", AchievementCategory.Progression, 20, 5000, 50, 2500),
        new("story_complete", "Storyteller", "Complete 7 storylines.", AchievementCategory.Progression, 7, 2000, 15, 1000),
        new("first_credits", "Money Moves", "Earn 1,000 credits in total.", AchievementCategory.Economy, 1000, 100),
        new("rich", "Crypto Rich", "Earn 100,000 credits in total.", AchievementCategory.Economy, 100000, 5000, 20),
        new("whale", "Whale", "Earn 1,000,000 credits in total.", AchievementCategory.Economy, 1000000, 50000, 100, 5000),
        new("scanner", "Scanner", "Run 50 scan commands.", AchievementCategory.Exploration, 50, 200),
        new("subnet_explorer", "Subnet Explorer", "Scan 20 unique subnets.", AchievementCategory.Exploration, 20, 800),
        new("team_player", "Team Player", "Join a faction.", AchievementCategory.Social, 1, 500, 10),
        new("script_kiddie", "Script Kiddie", "Save your first Nexus script.", AchievementCategory.Scripting, 1, 250, 0, 100),
        new("fragment_finder", "Fragment Finder", "Discover 10 knowledge fragments.", AchievementCategory.Scripting, 10, 1000, 5, 250),
        new("event_joiner", "Joiner", "Participate in 5 world events.", AchievementCategory.WorldEvent, 5, 1000, 10),
        new("event_regular", "World Shaper", "Participate in 25 world events.", AchievementCategory.WorldEvent, 25, 5000, 50),
        new("task_10", "Contractor", "Complete 10 tasks.", AchievementCategory.Progression, 10, 1000, 5, 500),
        new("task_100", "Pro Contractor", "Complete 100 tasks.", AchievementCategory.Progression, 100, 25000, 100, 5000)
    };

    private readonly Dictionary<string, AchievementDefinition> _byId;

    public AchievementService()
    {
        _byId = Definitions.ToDictionary(d => d.Id);
    }

    public AchievementDefinition? Get(string id) =>
        _byId.GetValueOrDefault(id);

    public List<AchievementDefinition> VisibleTo(Player player) =>
        Definitions.Where(d => !d.Hidden || player.UnlockedAchievements.Contains(d.Id)).ToList();

    public List<AchievementUpdate> Record(Player player, AchievementEvent eventType, long delta = 1)
    {
        var updates = new List<AchievementUpdate>();
        foreach (var def in Definitions)
        {
            if (!ListensTo(def, eventType)) continue;

            if (!player.AchievementProgress.TryGetValue(def.Id, out var progress))
            {
                progress = new AchievementProgress { AchievementId = def.Id };
                player.AchievementProgress[def.Id] = progress;
            }

            if (progress.Completed) continue;

            progress.CurrentValue = Math.Min(progress.CurrentValue + delta, def.TargetValue);

            if (progress.CurrentValue >= def.TargetValue)
            {
                progress.Completed = true;
                progress.CompletedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                player.UnlockedAchievements.Add(def.Id);
                updates.Add(new AchievementUpdate(
                    def.Id,
                    progress.CurrentValue,
                    true,
                    new AchievementRewards(def.RewardCredits, def.RewardReputation, def.RewardXp)
                ));
            }
            else
            {
                updates.Add(new AchievementUpdate(
                    def.Id,
                    progress.CurrentValue,
                    false,
                    AchievementRewards.None
                ));
            }
        }
        return updates;
    }

    private static bool ListensTo(AchievementDefinition def, AchievementEvent eventType) =>
        eventType switch
        {
            AchievementEvent.NODE_DISCOVERED => def.Id is "node_hunter" or "network_king" or "first_blood",
            AchievementEvent.NETWORK_COMPROMISED => def.Id is "first_blood" or "node_hunter" or "network_king",
            AchievementEvent.COMMAND_EXECUTED => def.Id is "scanner" or "subnet_explorer",
            AchievementEvent.TASK_COMPLETED => def.Id is "task_10" or "task_100",
            AchievementEvent.STORYLINE_COMPLETED => def.Id is "story_complete",
            AchievementEvent.LEVEL_REACHED => def.Id is "level_5" or "level_10" or "level_20",
            AchievementEvent.CREDITS_EARNED => def.Id is "first_credits" or "rich" or "whale",
            AchievementEvent.CREDITS_SPENT => false,
            AchievementEvent.FRAGMENT_GATHERED => def.Id is "fragment_finder",
            AchievementEvent.SCRIPT_SAVED => def.Id is "script_kiddie",
            AchievementEvent.FACTION_JOINED => def.Id is "team_player",
            AchievementEvent.WORLD_EVENT_PARTICIPATED => def.Id is "event_joiner" or "event_regular",
            AchievementEvent.COMMAND_UNLOCKED => def.Id is "command_master",
            AchievementEvent.CAREER_CHOSEN => false,
            AchievementEvent.HEAT_CHANGED => false,
            AchievementEvent.JUSTICE_CHANGED => false,
            AchievementEvent.ARREST_EXECUTED => false,
            AchievementEvent.BOUNTY_PLACED => false,
            AchievementEvent.ITEM_CRAFTED => false,
            _ => false
        };

    public Dictionary<string, int> SummaryFor(Player player)
    {
        var byCategory = player.UnlockedAchievements
            .Select(id => _byId.GetValueOrDefault(id))
            .Where(d => d != null)
            .GroupBy(d => d!.Category)
            .ToDictionary(g => g.Key, g => g.Count());

        return Enum.GetValues<AchievementCategory>()
            .ToDictionary(c => c.ToString(), c => byCategory.GetValueOrDefault(c, 0));
    }
}
