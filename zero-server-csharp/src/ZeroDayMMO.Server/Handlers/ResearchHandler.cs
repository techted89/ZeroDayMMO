using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class ResearchHandler : IHandler
{
    private readonly ResearchService _researchService;
    private readonly IPlayerService _playerService;

    public string MessageType => "research_recipes";

    public ResearchHandler(ResearchService researchService, IPlayerService playerService)
    {
        _researchService = researchService;
        _playerService = playerService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        try
        {
            var recipes = await _researchService.GetRecipes(playerId);
            return new MessageResult("research_recipes", connectionId, new { recipes });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleInventory(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        try
        {
            var items = await _researchService.GetInventory(playerId);
            return new MessageResult("research_inventory", connectionId, new { inventory = items });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleGather(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        try
        {
            var item = await _researchService.GatherFragment(playerId);
            return new MessageResult("fragment_gathered", connectionId, new { item });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleStart(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        var recipeId = payload?.Str("recipe_id");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(recipeId))
            return new MessageResult("error", connectionId, new { message = "Recipe ID required" });

        try
        {
            var progress = await _researchService.StartResearch(playerId, recipeId);
            return new MessageResult("research_started", connectionId, new { progress });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleClaim(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        var recipeId = payload?.Str("recipe_id");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(recipeId))
            return new MessageResult("error", connectionId, new { message = "Recipe ID required" });

        try
        {
            var item = await _researchService.ClaimResearch(playerId, recipeId);
            return new MessageResult("research_claimed", connectionId, new { item });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleUseItem(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        var itemId = payload?.Str("item_id");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(itemId))
            return new MessageResult("error", connectionId, new { message = "Item ID required" });

        try
        {
            var message = await _researchService.UseItem(playerId, itemId);
            return new MessageResult("item_used", connectionId, new { message });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleStatus(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        try
        {
            var status = await _researchService.GetResearchStatus(playerId);
            return new MessageResult("research_status", connectionId, new { active_research = status });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }
}
