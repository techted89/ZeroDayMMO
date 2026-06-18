using System.Text.Json;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared;

namespace ZeroDayMMO.Server.Handlers;

public class NetworkHandler : IHandler
{
    private readonly NetworkService _networkService;

    public string MessageType => "get_network";

    public NetworkHandler(NetworkService networkService)
    {
        _networkService = networkService;
    }

    public async Task<IActionResult> HandleAsync(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });

        try
        {
            var discovered = await _networkService.GetDiscoveredNodes(playerId);
            var allNodes = await _networkService.GetAllNodes();
            var stats = await _networkService.GetNetworkStats();
            return new MessageResult("network", connectionId, new
            {
                discovered_nodes = discovered,
                all_nodes_count = allNodes.Count,
                stats
            });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }

    public async Task<IActionResult> HandleDiscoverNode(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        var ip = payload?.Str("ip");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(ip))
            return new MessageResult("error", connectionId, new { message = "IP required" });

        var result = await _networkService.DiscoverNode(playerId, ip);
        if (result.IsSuccess)
            return new MessageResult("node_discovered", connectionId, new { node = result.Value });
        return new MessageResult("error", connectionId, new { message = result.Error ?? "Discovery failed" });
    }

    public async Task<IActionResult> HandleScanSubnet(string connectionId, JsonElement? payload, string? requestId)
    {
        var playerId = payload?.Str("playerId");
        var subnet = payload?.Str("subnet");
        if (string.IsNullOrEmpty(playerId))
            return new MessageResult("error", connectionId, new { message = "playerId required" });
        if (string.IsNullOrEmpty(subnet))
            return new MessageResult("error", connectionId, new { message = "Subnet required" });

        try
        {
            var found = await _networkService.ScanSubnet(playerId, subnet);
            return new MessageResult("subnet_scanned", connectionId, new
            {
                subnet,
                found_nodes = found,
                count = found.Count
            });
        }
        catch (Exception ex)
        {
            return new MessageResult("error", connectionId, new { message = ex.Message });
        }
    }
}
