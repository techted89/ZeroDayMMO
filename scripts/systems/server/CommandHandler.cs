using System;
using System.Collections.Generic;
using Godot;

/// <summary>
/// Handles all server-side command logic.
/// Each command method returns a Dictionary that will be sent back to the client.
/// </summary>
public class CommandHandler
{
    // Player data store (in-memory for this demo; replace with DB in production)
    private static Player CurrentPlayer => PlayerStore.GetOrCreatePlayer("h4ck3r");

    // Map of command name to handler method
    private readonly Dictionary<string, Action<string>> _commands = new Dictionary<string, Action<string>>
    {
        ["build_base"] = ExecuteBuildBase,
        ["claim"] = ExecuteClaim,
        ["base_status"] = ExecuteBaseStatus,
        ["scan_ip"] = ExecuteScanIP,
        ["attack"] = ExecuteAttack,
        ["join_event"] = ExecuteJoinEvent,
        ["upgrade_base"] = ExecuteUpgradeBase,
        ["skip_tutorial"] = ExecuteSkipTutorial,
        ["status"] = ExecuteStatus,
        ["help"] = ExecuteHelp,
        ["travel"] = ExecuteTravel,
        ["router"] = ExecuteRouter,
        ["map"] = ExecuteMap,
        ["flee"] = ExecuteFlee,
        ["tutorial"] = ExecuteTutorial
    };

    /// <summary>
    /// Entry point called by ServerInterface with the raw command string.
    /// Returns the response Dictionary to be sent back to the client.
    /// </summary>
    public Dictionary ProcessCommand(string rawCommand)
    {
        var parts = rawCommand.Split(' ', 2);
        var command = parts.Length > 0 ? parts[0].ToLower() : "";
        var args = parts.Length > 1 ? parts[1] : "";

        if (_commands.TryGetValue(command, out var handler))
        {
            handler(args);
            // After handler execution, the relevant response is already stored in
            // CurrentPlayer or other server state. The ServerInterface will query
            // that state or we rely on explicit return values from handlers.
            // For commands that need to return data (e.g., status), we capture it
            // in a context variable and return it.
            // For this demo, we return an empty dict; actual data is sent via events.
            return new Dictionary();
        }
        else
        {
            return new Dictionary
            {
                ["output"] = "",
                ["error"] = $"Unknown command: {command}"
            };
        }
    }

    // -------------------------------------------------
    // Command Implementations
    // -------------------------------------------------

    private void ExecuteBuildBase(string args)
    {
        // args may contain the desired IP; default to current IP if not provided.
        var targetIp = string.IsNullOrWhiteSpace(args) ? CurrentPlayer.CurrentIP : args;

        // Validation: cannot build inside localhost.
        if (targetIp.StartsWith("127."))
        {
            // Send error to client via ServerInterface (simulate by setting response)
            // In a real server this would send via WebSocket.
            return;
        }

        // Credits check
        const int BUILD_COST = 500;
        if (CurrentPlayer.Credits < BUILD_COST)
        {
            // Error response should be handled by ServerInterface caller.
            return;
        }

        // Deduct credits
        CurrentPlayer.Credits -= BUILD_COST;
        CurrentPlayer.BaseLocation = targetIp;
        CurrentPlayer.ControlledNodes.Clear(); // Start with empty set of controlled nodes.
        // Save base IP as a pref for persistence
        SavePlayerPref("base_location", targetIp);
        SavePlayerPref("base_subnet", "24");

        // Success response will be handled by ServerInterface after this method returns.
    }

    private void ExecuteClaim(string args)
    {
        var targetIp = args?.Trim();

        if (string.IsNullOrEmpty(targetIp))
            return;

        // Must have a base first
        if (string.IsNullOrEmpty(CurrentPlayer.BaseLocation))
        {
            // Error: no base
            return;
        }

        // Distance check against base subnet
        var baseIp = CurrentPlayer.BaseLocation;
        var distance = CalculateIpDistance(baseIp, targetIp);
        if (distance > 256)
        {
            // Error: out of range
            return;
        }

        // Load control node list
        var controlStr = LoadPlayerPref("control_nodes", "[]");
        var controlNodes = ParseJsonArray(controlStr);
        if (controlNodes == null) controlNodes = new List<string>();

        // Check if node already claimed
        if (controlNodes.Contains(targetIp))
        {
            // Already claimed
            return;
        }

        // Check for hostile ownership (simplified: if node type is hostile)
        var nodeIpInfo = GetNodeInfo(targetIp);
        if (nodeIpInfo != null && nodeIpInfo.IsHostile())
        {
            // Error if hostile
            return;
        }

        // Claim the node
        controlNodes.Add(targetIp);
        SavePlayerPref("control_nodes", JSONStringify(controlNodes));

        // Update current player's controlled nodes list
        CurrentPlayer.ControlledNodes = controlNodes;
        SavePlayerPref("control_nodes", JSONStringify(CurrentPlayer.ControlledNodes));

        // Success - handled by client UI updates
    }

    private void ExecuteBaseStatus()
    {
        // Build the response data for the client UI
        var response = new Dictionary();
        var baseIp = LoadPlayerPref("base_location", "");
        if (string.IsNullOrEmpty(baseIp))
        {
            response["output"] = "No base established.";
            return;
        }

        var subnet = LoadPlayerPref("base_subnet", "24");
        var controlStr = LoadPlayerPref("control_nodes", "[]");
        var bonusesStr = LoadPlayerPref("base_bonuses", "{}");

        var controlNodes = ParseJsonArray(controlStr);
        var bonuses = ParseJsonObject(bonusesStr);

        // Calculate total bonuses (example: each controlled node adds +1 to each stat, up to caps)
        var totalBonuses = new Dictionary<string, int>();
        foreach (var kv in bonuses)
        {
            totalBonuses[kv.Key] = kv.Value;
        }

        // Attach bonuses to response for UI consumption
        response["output"] = GenerateBaseStatusUi(baseIp, int.Parse(subnet), controlNodes, bonuses);
    }

    private void ExecuteScanIP(string args)
    {
        // Scan logic is client‑side; server just returns a placeholder response.
        // In a full implementation we'd query the node database and return details.
        // For now:
        var response = new Dictionary
        {
            ["output"] = "Scanned IP: " + args,
            ["status"] = "success"
        };
        // This output would be forwarded to the client.
    }

    private void ExecuteAttack(string args)
    {
        // Turn-based combat handled on the client; server validates range and initiates combat.
    }

    private void ExecuteJoinEvent(string args)
    {
        // Event join logic; not fully implemented in this snippet.
    }

    private void ExecuteUpgradeBase(string args)
    {
        // Base upgrade logic (cost, level, etc.) – left as future work.
    }

    private void ExecuteSkipTutorial(string _)
    {
        // Mark tutorial as completed
        SavePlayerPref("tutorial_completed", true);
        // Persist tutorial step so UI knows it's done
        SavePlayerPref("tutorial_step", 10);
    }

    private void ExecuteStatus()
    {
        // Populate a global "status_text" that UI can read
        var p = CurrentPlayer;
        var statusText = $"User: {p.Username} (Lv.{p.Level})\n" +
                         $"IP: {p.CurrentIP}\n" +
                         $"CPU: {p.CPU}/{p.MaxCPU} MHz\n" +
                         $"RAM: {p.RAM}/{p.MaxRAM} MB\n" +
                         $"Credits: ${p.Credits}\n" +
                         $"Reputation: {p.Reputation}\n" +
                         $"Base: {(string.IsNullOrEmpty(p.BaseLocation) ? "None" : p.BaseLocation)}\n";
        // Store this text somewhere accessible to UI (e.g., set a global variable)
        Godot.Core.SetCustom singleton("GlobalStatusText", statusText);
    }

    private void ExecuteHelp()
    {
        // List all commands in a help string
        var helpText = "Available commands:\n" +
                       "  build_base <ip>\n" +
                       "  claim <ip>\n" +
                       "  base_status\n" +
                       "  scan_ip <ip>\n" +
                       "  attack <ip>\n" +
                       "  join_event <event_name>\n" +
                       "  upgrade_base\n" +
                       "  skip_tutorial\n" +
                       "  status\n" +
                       "  tutorial <start|skip>\n" +
                       "  travel <ip>\n" +
                       "  router [list|discover]\n" +
                       "  map [show|travel <ip>]\n" +
                       "  flee\n" +
                       "  tutorial <command>\n";
        // Store help text somewhere UI can access
        Godot.Core.SetCustom singleton("GlobalHelpText", helpText);
    }

    private void ExecuteTravel(string args)
    {
        // Simple validation – ensure args is an IP-like string.
        if (!args.Contains(".")) return;
        // In a full implementation this would update Player.CurrentIP and notify server.
    }

    private void ExecuteRouter(string args)
    {
        // Router handling (list or discover) – left for future expansion.
    }

    private void ExecuteMap(string args)
    {
        // Map UI toggle or travel – left for future expansion.
    }

    private void ExecuteFlee()
    {
        // Retreat logic: lose some credits as penalty.
        var penalty = (int)(CurrentPlayer.Credits * 0.05);
        CurrentPlayer.Credits -= penalty;
        // Persist updated credits
        SavePlayerPref("credits", CurrentPlayer.Credits);
    }

    private void ExecuteTutorial(string subcmd)
    {
        // Tutorial management: start, skip, or status
        if (subcmd.Equals("start", StringComparison.OrdinalIgnoreCase))
        {
            SavePlayerPref("tutorial_active", true);
        }
        else if (subcmd.Equals("skip", StringComparison.OrdinalIgnoreCase))
        {
            SavePlayerPref("tutorial_completed", true);
            SavePlayerPref("tutorial_step", 10);
        }
        else
        {
            // Return current step
            var step = LoadPlayerPref("tutorial_step", "0");
            // UI would read this value.
        }
    }

    // -------------------------------------------------
    // Helper Methods
    // -------------------------------------------------

    private void SavePlayerPref(string key, string value)
    {
        // Store in Godot's ConfigFile or PlayerPrefs system; here we just set a singleton.
        Godot.Core.SetCustom("PlayerPref_" + key, value);
    }

    private string LoadPlayerPref(string key, string defaultValue = "")
    {
        return Godot.Core.GetCustom("PlayerPref_" + key) as string ?? defaultValue;
    }

    private string JSONStringify(object obj)
    {
        return JSON.Print(obj);
    }

    private List<string> ParseJsonArray(string json)
    {
        if (string.IsNullOrWhiteSpace(json) || json == "[]")
            return new List<string>();

        var result = new List<string>();
        // Simplified parsing – in a full impl use JSON.ParseArray.
        var trimmed = json.Trim('[', ']');
        var items = trimmed.Split(',');
        foreach (var item in items)
        {
            result.Add(item.Trim('\"'));
        }
        return result;
    }

    private Dictionary ParseJsonObject(string json)
    {
        if (string.IsNullOrWhiteSpace(json) || json == "{}" )
            return new Dictionary();

        // Simplified parsing – in a full impl use JSON.Parse.
        var trimmed = json.Trim('{', '}');
        var dict = new Dictionary<string, object>();
        var pairs = trimmed.Split(',');
        foreach (var pair in pairs)
        {
            var kv = pair.Split(':');
            if (kv.Length == 2)
            {
                var key = kv[0].Trim('\"');
                var valueStr = kv[1].Trim('\"');
                // Try to parse numbers
                if (int.TryParse(valueStr, out int ival) || long.TryParse(valueStr, out long lval))
                {
                    dict[key] = ival;
                }
                else if (valueStr.Equals("true", StringComparison.OrdinalIgnoreCase))
                {
                    dict[key] = true;
                }
                else if (valueStr.Equals("false", StringComparison.OrdinalIgnoreCase))
                {
                    dict[key] = false;
                }
                else
                {
                    dict[key] = valueStr.Trim('\"');
                }
            }
        }
        return dict;
    }

    // Utility helpers omitted for brevity
    private int CalculateIpDistance(string ip1, string ip2)
    {
        // Simple metric: number of differing octets
        var o1 = ip1.Split('.');
        var o2 = ip2.Split('.');
        int diff = 0;
        for (int i = 0; i < 4; i++)
        {
            if (o1[i] != o2[i]) diff++;
        }
        return diff;
    }
}

// Simple Player data class
public class Player
{
    public string Username { get; set; } = "h4ck3r";
    public int Level { get; set; } = 1;
    public int Experience { get; set; } = 0;
    public int ExperienceToNext => Level * 100;
    public int Credits { get; set; } = 0;
    public int Reputation { get; set; } = 0;
    public int CPU { get; set; } = 30;
    public int MaxCPU { get; set; } = 30;
    public int RAM { get; set; } = 256;
    public int MaxRAM { get; set; } = 256;
    public int Bandwidth { get; set; } = 10;
    public int MaxBandwidth { get; set; } = 10;
    public string CurrentIP { get; set; } = "127.0.0.1";
    public string BaseLocation { get; set; } = "";
    public List<string> ControlledNodes { get; set; } = new List<string>();
    public Dictionary<string, int> BaseBonuses { get; set; } = new Dictionary<string, int>();
    public bool TutorialCompleted { get; set; } = false;
}

// In‑memory store for player data (replace with DB in production)
public static class PlayerStore
{
    private static readonly Dictionary<string, Player> _players = new Dictionary<string, Player>();

    public static Player GetOrCreatePlayer(string username)
    {
        if (!_players.TryGetValue(username, out var player))
        {
            player = new Player { Username = username };
            _players[username] = player;
        }
        return player;
    }

    public static Player GetPlayer(string username) => _players.TryGetValue(username, out var p) ? p : null;
}

// Helper extensions (simplified)
public static class ExtensionMethods
{
    public static bool IsHostile(this NodeInfo nodeInfo) => false; // Placeholder for hostile detection logic
}

// Placeholder for node info / data model
public class NodeInfo
{
    public string Ip { get; set; }
    public string Type { get; set; }
    public string Owner { get; set; }
    public bool IsHostile()
    {
        // Simplified: certain types are considered hostile
        return new[] { "virus", "worm", "devourer", "ai_overlord" }.Contains(Type);
    }
}