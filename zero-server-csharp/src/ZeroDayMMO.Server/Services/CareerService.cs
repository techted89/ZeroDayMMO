using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public class CareerService
{
    private readonly IPlayerService _playerService;
    private readonly GameEventBus _gameEventBus;
    private readonly SemaphoreSlim _careerMutex = new(1, 1);

    public record CareerActionResult(
        bool Success,
        string Message,
        int HeatDelta = 0,
        int JusticeDelta = 0
    );

    public CareerService(IPlayerService playerService, GameEventBus gameEventBus)
    {
        _playerService = playerService;
        _gameEventBus = gameEventBus;
    }

    public CareerActionResult ChooseCareer(string playerId, string career)
    {
        var player = _playerService.GetPlayer(playerId);
        if (player is null) return new CareerActionResult(false, "Player not found");

        if (player.CareerPath != "undecided")
            return new CareerActionResult(false, $"Career already chosen as {player.CareerPath}");

        switch (career.ToLowerInvariant())
        {
            case "whitehat":
                player.CareerPath = "whitehat";
                player.JusticePoints = 10;
                _gameEventBus.Publish(new GameEvent { PlayerId = playerId, Type = AchievementEvent.CAREER_CHOSEN, Value = 10 });
                _playerService.UpdatePlayer(player);
                return new CareerActionResult(true, "You chose the White Hat path. Justice requires vigilance.", 0, 10);

            case "blackhat":
                player.CareerPath = "blackhat";
                player.Notoriety = 10;
                _gameEventBus.Publish(new GameEvent { PlayerId = playerId, Type = AchievementEvent.CAREER_CHOSEN, Value = 10 });
                _playerService.UpdatePlayer(player);
                return new CareerActionResult(true, "You chose the Black Hat path. The shadows welcome you.", 0, 0);

            default:
                return new CareerActionResult(false, $"Invalid career: {career}. Choose 'whitehat' or 'blackhat'.");
        }
    }

    public CareerActionResult AddHeat(string playerId, int amount, string reason = "")
    {
        var player = _playerService.GetPlayer(playerId);
        if (player is null) return new CareerActionResult(false, "Player not found");

        if (player.CareerPath != "blackhat")
            return new CareerActionResult(false, "Only Black Hats generate heat", 0, 0);

        player.HeatLevel = Math.Min(player.HeatLevel + amount, player.MaxHeatLevel);
        player.Notoriety += amount / 2;

        if (player.HeatLevel >= 80 && player.BountyPrice == 0L)
        {
            player.BountyPrice = player.HeatLevel * 50L;
            _gameEventBus.Publish(new GameEvent { PlayerId = playerId, Type = AchievementEvent.BOUNTY_PLACED, Value = player.BountyPrice });
        }

        _gameEventBus.Publish(new GameEvent { PlayerId = playerId, Type = AchievementEvent.HEAT_CHANGED, Value = amount });
        _playerService.UpdatePlayer(player);

        return new CareerActionResult(true, $"Heat +{amount}: {reason}", amount, 0);
    }

    public CareerActionResult AddJusticePoints(string playerId, int amount, string reason = "")
    {
        var player = _playerService.GetPlayer(playerId);
        if (player is null) return new CareerActionResult(false, "Player not found");

        if (player.CareerPath != "whitehat")
            return new CareerActionResult(false, "Only White Hats earn justice points", 0, 0);

        player.JusticePoints += amount;
        var newRank = Math.Min(player.JusticePoints / 50, 6);
        player.WhiteHatRank = newRank;

        _gameEventBus.Publish(new GameEvent { PlayerId = playerId, Type = AchievementEvent.JUSTICE_CHANGED, Value = amount });
        _playerService.UpdatePlayer(player);

        return new CareerActionResult(true, $"Justice +{amount}: {reason}", 0, amount);
    }

    public CareerActionResult AttemptArrest(string arrestingPlayerId, string targetPlayerId)
    {
        var arrester = _playerService.GetPlayer(arrestingPlayerId);
        if (arrester is null) return new CareerActionResult(false, "Arrester not found");

        var target = _playerService.GetPlayer(targetPlayerId);
        if (target is null) return new CareerActionResult(false, "Target not found");

        if (arrester.CareerPath != "whitehat")
            return new CareerActionResult(false, "Only White Hats can make arrests");

        if (target.CareerPath != "blackhat")
            return new CareerActionResult(false, "Target is not a Black Hat");

        var successChance = 0.3 + (arrester.JusticePoints * 0.001) - (target.HeatLevel * 0.005);
        var success = Random.Shared.NextDouble() < Math.Clamp(successChance, 0.05, 0.95);

        if (success)
        {
            var justiceEarned = 10 + target.HeatLevel / 5;
            var jailHours = target.HeatLevel * 0.5f;

            arrester.JusticePoints += justiceEarned;
            arrester.WhiteHatRank = Math.Min(arrester.JusticePoints / 50, 6);
            _playerService.UpdatePlayer(arrester);

            target.TimesArrested++;
            target.JailTimeRemaining = jailHours * 3600f;
            target.IsInJail = true;
            target.HeatLevel = Math.Min(target.HeatLevel / 2, target.MaxHeatLevel);
            target.BountyPrice = 0L;
            _playerService.UpdatePlayer(target);

            _gameEventBus.Publish(new GameEvent { PlayerId = arrestingPlayerId, Type = AchievementEvent.ARREST_EXECUTED, Value = justiceEarned });

            return new CareerActionResult(true, "Arrested! Justice points earned.", 0, justiceEarned);
        }
        else
        {
            return new CareerActionResult(false, $"Target evaded capture. Success chance was {(int)(successChance * 100)}%");
        }
    }

    public int DecayHeat()
    {
        var totalDecayed = 0;
        foreach (var player in _playerService.GetAllPlayers())
        {
            if (player.CareerPath != "blackhat" || player.HeatLevel <= 0 || player.IsInJail) continue;

            var decayAmount = Math.Min(1, player.HeatLevel);
            player.HeatLevel = Math.Max(player.HeatLevel - decayAmount, 0);
            _playerService.UpdatePlayer(player);
            totalDecayed += decayAmount;
        }
        return totalDecayed;
    }

    public Dictionary<string, object?> GetCareerReport(string playerId)
    {
        var player = _playerService.GetPlayer(playerId);
        if (player is null) return new Dictionary<string, object?>();

        return new Dictionary<string, object?>
        {
            ["careerPath"] = player.CareerPath,
            ["heatLevel"] = player.HeatLevel,
            ["maxHeatLevel"] = player.MaxHeatLevel,
            ["notoriety"] = player.Notoriety,
            ["justicePoints"] = player.JusticePoints,
            ["bountyPrice"] = player.BountyPrice,
            ["timesArrested"] = player.TimesArrested,
            ["isInJail"] = player.IsInJail,
            ["jailTimeRemaining"] = player.JailTimeRemaining,
            ["whiteHatRank"] = player.WhiteHatRank,
            ["blackHatRank"] = player.BlackHatRank
        };
    }
}
