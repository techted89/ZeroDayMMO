using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public class ResearchService : IHostedService
{
    private readonly IPlayerService _playerService;
    private readonly GameEventBus? _gameEventBus;
    private readonly Dictionary<string, List<ResearchProgress>> _activeResearch = new();
    private readonly SemaphoreSlim _mutex = new(1, 1);
    private CancellationTokenSource? _cts;
    private Task? _loopTask;

    public ResearchService(IPlayerService playerService, GameEventBus? gameEventBus = null)
    {
        _playerService = playerService;
        _gameEventBus = gameEventBus;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _loopTask = ResearchCompletionLoopAsync(_cts.Token);
        return Task.CompletedTask;
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        _cts?.Cancel();
        if (_loopTask is not null)
        {
            try { await _loopTask; } catch (OperationCanceledException) { }
        }
    }

    private async Task ResearchCompletionLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay(5_000, ct);
            await ProcessResearchCompletion();
        }
    }

    public async Task<List<CraftingRecipe>> GetRecipes(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) return new List<CraftingRecipe>();

            var completedRecipes = _activeResearch.TryGetValue(playerId, out var researches)
                ? researches.Where(r => r.Completed).Select(r => r.RecipeId).ToHashSet()
                : new HashSet<string>();

            return CraftingRecipes.AllRecipes
                .Where(r => !completedRecipes.Contains(r.Id))
                .ToList();
        }
        finally { _mutex.Release(); }
    }

    public async Task<List<InventoryItem>> GetInventory(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");
            return player.Inventory.ToList();
        }
        finally { _mutex.Release(); }
    }

    public async Task<InventoryItem> GatherFragment(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");

            var cost = Math.Max(player.Level * 5, 5);
            if (player.Cpu < cost || player.Ram < cost * 2)
                throw new InvalidOperationException($"Insufficient resources. Need {cost}CPU/{cost * 2}RAM");

            player.Cpu -= cost;
            player.Ram -= cost * 2;
            _playerService.UpdatePlayer(player);

            var rarityRoll = Random.Shared.NextDouble();
            var rarity = rarityRoll switch
            {
                < 0.5 => ItemRarity.COMMON,
                < 0.8 => ItemRarity.UNCOMMON,
                < 0.95 => ItemRarity.RARE,
                < 0.99 => ItemRarity.EPIC,
                _ => ItemRarity.LEGENDARY
            };

            var fragment = new InventoryItem
            {
                Type = ItemType.KNOWLEDGE_FRAGMENT,
                Name = $"{rarity.DisplayName()} Knowledge Fragment",
                Description = $"A fragment of digital knowledge. Rarity: {rarity.DisplayName()}",
                Rarity = rarity,
                Data = new Dictionary<string, string>
                {
                    ["source"] = "network_scan",
                    ["level"] = player.Level.ToString()
                }
            };

            player.Inventory.Add(fragment);
            _playerService.UpdatePlayer(player);
            _gameEventBus?.Publish(new GameEvent { PlayerId = playerId, Type = AchievementEvent.FRAGMENT_GATHERED, Value = 1 });
            return fragment;
        }
        finally { _mutex.Release(); }
    }

    public async Task<ResearchProgress> StartResearch(string playerId, string recipeId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");

            var recipe = CraftingRecipes.AllRecipes.Find(r => r.Id == recipeId)
                ?? throw new InvalidOperationException("Recipe not found");

            if (player.Level < recipe.RequiredLevel)
                throw new InvalidOperationException($"Level {recipe.RequiredLevel} required");
            if (recipe.RequiredCommands.Any(c => !player.UnlockedCommands.Contains(c)))
                throw new InvalidOperationException($"Required commands: {string.Join(", ", recipe.RequiredCommands)}");

            if (_activeResearch.TryGetValue(playerId, out var existing) &&
                existing.Any(r => r.RecipeId == recipeId && !r.Completed))
                throw new InvalidOperationException("Already researching this recipe");

            foreach (var ingredient in recipe.Ingredients)
            {
                var remaining = ingredient.Quantity;
                var iterator = player.Inventory.ToList().GetEnumerator();
                while (iterator.MoveNext() && remaining > 0)
                {
                    var item = iterator.Current;
                    if (item.Type == ingredient.ItemType && (int)item.Rarity >= (int)ingredient.MinRarity)
                    {
                        player.Inventory.Remove(item);
                        remaining--;
                    }
                }
                if (remaining > 0)
                    throw new InvalidOperationException($"Missing ingredients: {ingredient.Quantity}x {ingredient.ItemType} (min {ingredient.MinRarity.DisplayName()})");
                if (ingredient.CreditsCost > 0 && player.Credits < ingredient.CreditsCost)
                    throw new InvalidOperationException($"Need {ingredient.CreditsCost} credits");
                if (ingredient.CreditsCost > 0)
                {
                    player.Credits -= ingredient.CreditsCost;
                }
            }
            _playerService.UpdatePlayer(player);

            var progress = new ResearchProgress { RecipeId = recipeId };
            if (!_activeResearch.ContainsKey(playerId))
                _activeResearch[playerId] = new List<ResearchProgress>();
            _activeResearch[playerId].Add(progress);
            return progress;
        }
        finally { _mutex.Release(); }
    }

    public async Task<InventoryItem> ClaimResearch(string playerId, string recipeId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");

            var research = _activeResearch.TryGetValue(playerId, out var researches)
                ? researches.Find(r => r.RecipeId == recipeId && r.Completed && !r.Claimed)
                : null;
            if (research is null) throw new InvalidOperationException("No completed research to claim");

            var recipe = CraftingRecipes.AllRecipes.Find(r => r.Id == recipeId)
                ?? throw new InvalidOperationException("Recipe not found");

            research.Claimed = true;
            player.Experience += recipe.XpReward;
            CheckLevelUp(player);

            var result = new InventoryItem
            {
                Type = recipe.ResultType,
                Name = recipe.ResultName,
                Description = string.IsNullOrEmpty(recipe.ResultDescription) ? $"Crafted from {recipeId}" : recipe.ResultDescription,
                Rarity = recipe.ResultRarity,
                Data = new Dictionary<string, string>
                {
                    ["recipe"] = recipeId,
                    ["crafted_at"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds().ToString()
                }
            };

            player.Inventory.Add(result);
            _playerService.UpdatePlayer(player);
            return result;
        }
        finally { _mutex.Release(); }
    }

    public async Task<string> UseItem(string playerId, string itemId)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) throw new InvalidOperationException("Player not found");

            var item = player.Inventory.Find(i => i.Id == itemId)
                ?? throw new InvalidOperationException("Item not found in inventory");

            return item.Type switch
            {
                ItemType.ZERO_DAY_EXPLOIT => UseZeroDayExploit(player, item),
                ItemType.ACCESS_TOKEN => UseAccessToken(player, item),
                ItemType.FIREWALL_PATCH => UseFirewallPatch(player, item),
                _ => "This item cannot be used directly. It's a crafting ingredient."
            };
        }
        finally { _mutex.Release(); }
    }

    private string UseZeroDayExploit(Player player, InventoryItem item)
    {
        player.ActiveZeroDayExploits.Add(item);
        player.Inventory.Remove(item);
        _playerService.UpdatePlayer(player);
        return $"Zero-day exploit loaded. Use 'exploit <target> --use-zeroday' to deploy it. Bypasses security level {GetZeroDayPower(item)}.";
    }

    private string UseAccessToken(Player player, InventoryItem item)
    {
        player.Inventory.Remove(item);
        _playerService.UpdatePlayer(player);
        return "Access token consumed. You now have temporary admin access to all discovered nodes. Use 'connect <ip>' to access any node.";
    }

    private string UseFirewallPatch(Player player, InventoryItem item)
    {
        player.FirewallBoost = 3;
        player.Inventory.Remove(item);
        _playerService.UpdatePlayer(player);
        return "Firewall patch applied. Your firewall command will block 3 additional trace attempts.";
    }

    public async Task<List<ResearchProgress>> GetResearchStatus(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            return _activeResearch.TryGetValue(playerId, out var researches)
                ? researches.Where(r => !r.Claimed).ToList()
                : new List<ResearchProgress>();
        }
        finally { _mutex.Release(); }
    }

    private static int GetZeroDayPower(InventoryItem item) => item.Rarity switch
    {
        ItemRarity.RARE => 5,
        ItemRarity.EPIC => 10,
        ItemRarity.LEGENDARY => 99,
        _ => 3
    };

    private async Task ProcessResearchCompletion()
    {
        await _mutex.WaitAsync();
        try
        {
            var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            foreach (var (playerId, researches) in _activeResearch)
            {
                foreach (var research in researches.Where(r => !r.Completed))
                {
                    var recipe = CraftingRecipes.AllRecipes.Find(r => r.Id == research.RecipeId);
                    if (recipe is null) continue;
                    if (now - research.StartedAt >= recipe.CraftingTimeMs)
                        research.Completed = true;
                }
            }
        }
        finally { _mutex.Release(); }
    }

    public async Task<bool> AddItem(string playerId, InventoryItem item)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player is null) return false;
            player.Inventory.Add(item);
            _playerService.UpdatePlayer(player);
            return true;
        }
        finally { _mutex.Release(); }
    }

    private static void CheckLevelUp(Player player)
    {
        while (player.Experience >= player.ExperienceToNext)
        {
            player.Level += 1;
            player.Experience -= player.ExperienceToNext;
            player.ExperienceToNext = (long)(player.ExperienceToNext * 1.2);
        }
    }
}
