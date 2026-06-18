using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum CommandCategory
{
    [JsonPropertyName("basic")]
    BASIC,
    [JsonPropertyName("recon")]
    RECON,
    [JsonPropertyName("exploit")]
    EXPLOIT,
    [JsonPropertyName("defense")]
    DEFENSE,
    [JsonPropertyName("network")]
    NETWORK,
    [JsonPropertyName("system")]
    SYSTEM,
    [JsonPropertyName("stealth")]
    STEALTH,
    [JsonPropertyName("crypto")]
    CRYPTO
}

public static class CommandCategoryExtensions
{
    public static string DisplayName(this CommandCategory category) => category switch
    {
        CommandCategory.BASIC => "Basic",
        CommandCategory.RECON => "Reconnaissance",
        CommandCategory.EXPLOIT => "Exploitation",
        CommandCategory.DEFENSE => "Defense",
        CommandCategory.NETWORK => "Networking",
        CommandCategory.SYSTEM => "System",
        CommandCategory.STEALTH => "Stealth",
        CommandCategory.CRYPTO => "Cryptography",
        _ => "Basic"
    };
}

public class GameCommand
{
    [JsonPropertyName("name")]
    public string Name { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("syntax")]
    public string Syntax { get; set; } = "";

    [JsonPropertyName("category")]
    public CommandCategory Category { get; set; }

    [JsonPropertyName("min_level")]
    public int MinLevel { get; set; }

    [JsonPropertyName("cpu_cost")]
    public int CpuCost { get; set; }

    [JsonPropertyName("ram_cost")]
    public int RamCost { get; set; }

    [JsonPropertyName("cooldown_ms")]
    public long CooldownMs { get; set; } = 0;

    [JsonPropertyName("is_passive")]
    public bool IsPassive { get; set; } = false;
}

public static class CommandRegistry
{
    public static List<GameCommand> AllCommands { get; } = new()
    {
        new GameCommand { Name = "help", Description = "Show available commands", Syntax = "help [command]", Category = CommandCategory.BASIC, MinLevel = 1, CpuCost = 0, RamCost = 0 },
        new GameCommand { Name = "scan", Description = "Scan local network for hosts", Syntax = "scan [target]", Category = CommandCategory.BASIC, MinLevel = 1, CpuCost = 5, RamCost = 10, CooldownMs = 1000 },
        new GameCommand { Name = "connect", Description = "Connect to a remote host", Syntax = "connect <ip>", Category = CommandCategory.BASIC, MinLevel = 1, CpuCost = 5, RamCost = 5, CooldownMs = 500 },
        new GameCommand { Name = "whoami", Description = "Display current user info", Syntax = "whoami", Category = CommandCategory.BASIC, MinLevel = 1, CpuCost = 0, RamCost = 0 },
        new GameCommand { Name = "status", Description = "Show system status", Syntax = "status", Category = CommandCategory.BASIC, MinLevel = 1, CpuCost = 0, RamCost = 0 },
        new GameCommand { Name = "ls", Description = "List directory contents", Syntax = "ls [path]", Category = CommandCategory.SYSTEM, MinLevel = 1, CpuCost = 2, RamCost = 5 },
        new GameCommand { Name = "cat", Description = "Read file contents", Syntax = "cat <file>", Category = CommandCategory.SYSTEM, MinLevel = 2, CpuCost = 2, RamCost = 5 },
        new GameCommand { Name = "ifconfig", Description = "Show network interfaces", Syntax = "ifconfig", Category = CommandCategory.NETWORK, MinLevel = 2, CpuCost = 3, RamCost = 8 },
        new GameCommand { Name = "ping", Description = "Ping a remote host", Syntax = "ping <ip>", Category = CommandCategory.NETWORK, MinLevel = 2, CpuCost = 3, RamCost = 5, CooldownMs = 2000 },
        new GameCommand { Name = "nmap", Description = "Port scan a target", Syntax = "nmap [-sS|-sV] <ip>", Category = CommandCategory.RECON, MinLevel = 3, CpuCost = 15, RamCost = 30, CooldownMs = 5000 },
        new GameCommand { Name = "traceroute", Description = "Trace route to host", Syntax = "traceroute <ip>", Category = CommandCategory.RECON, MinLevel = 3, CpuCost = 10, RamCost = 20, CooldownMs = 4000 },
        new GameCommand { Name = "whois", Description = "Lookup IP information", Syntax = "whois <ip>", Category = CommandCategory.RECON, MinLevel = 4, CpuCost = 8, RamCost = 15, CooldownMs = 3000 },
        new GameCommand { Name = "ssh", Description = "SSH into a remote host", Syntax = "ssh <user>@<ip>", Category = CommandCategory.NETWORK, MinLevel = 4, CpuCost = 10, RamCost = 20, CooldownMs = 3000 },
        new GameCommand { Name = "ftp", Description = "Connect via FTP", Syntax = "ftp <ip>", Category = CommandCategory.NETWORK, MinLevel = 4, CpuCost = 8, RamCost = 15, CooldownMs = 2500 },
        new GameCommand { Name = "exploit", Description = "Run exploit against target", Syntax = "exploit <target> [--payload=<name>]", Category = CommandCategory.EXPLOIT, MinLevel = 5, CpuCost = 25, RamCost = 40, CooldownMs = 8000 },
        new GameCommand { Name = "bruteforce", Description = "Brute force credentials", Syntax = "bruteforce <target> [--threads=N]", Category = CommandCategory.EXPLOIT, MinLevel = 6, CpuCost = 30, RamCost = 50, CooldownMs = 10000 },
        new GameCommand { Name = "decrypt", Description = "Decrypt captured data", Syntax = "decrypt <file> [--key=<key>]", Category = CommandCategory.CRYPTO, MinLevel = 6, CpuCost = 20, RamCost = 35, CooldownMs = 6000 },
        new GameCommand { Name = "sqlmap", Description = "SQL injection scanner", Syntax = "sqlmap <url>", Category = CommandCategory.EXPLOIT, MinLevel = 7, CpuCost = 25, RamCost = 45, CooldownMs = 8000 },
        new GameCommand { Name = "backdoor", Description = "Install backdoor on host", Syntax = "backdoor <ip> [--persistent]", Category = CommandCategory.EXPLOIT, MinLevel = 7, CpuCost = 20, RamCost = 30, CooldownMs = 12000 },
        new GameCommand { Name = "sniff", Description = "Capture network packets", Syntax = "sniff [interface]", Category = CommandCategory.RECON, MinLevel = 8, CpuCost = 15, RamCost = 40, CooldownMs = 5000 },
        new GameCommand { Name = "spoof", Description = "Spoof network identity", Syntax = "spoof <ip> [--mac=<addr>]", Category = CommandCategory.STEALTH, MinLevel = 8, CpuCost = 20, RamCost = 25, CooldownMs = 8000 },
        new GameCommand { Name = "proxy", Description = "Route traffic through proxy", Syntax = "proxy <ip>:<port>", Category = CommandCategory.STEALTH, MinLevel = 9, CpuCost = 15, RamCost = 20, CooldownMs = 4000 },
        new GameCommand { Name = "encrypt", Description = "Encrypt files/traffic", Syntax = "encrypt <data> [--method=<alg>]", Category = CommandCategory.CRYPTO, MinLevel = 9, CpuCost = 15, RamCost = 25, CooldownMs = 3000 },
        new GameCommand { Name = "worm", Description = "Deploy self-replicating worm", Syntax = "worm <target> [--spread]", Category = CommandCategory.EXPLOIT, MinLevel = 10, CpuCost = 40, RamCost = 60, CooldownMs = 20000 },
        new GameCommand { Name = "firewall", Description = "Deploy firewall rules", Syntax = "firewall [--allow=<ip>] [--deny=<ip>]", Category = CommandCategory.DEFENSE, MinLevel = 10, CpuCost = 25, RamCost = 35, CooldownMs = 8000 },
        new GameCommand { Name = "trace", Description = "Trace attacker origin", Syntax = "trace [session]", Category = CommandCategory.DEFENSE, MinLevel = 11, CpuCost = 20, RamCost = 30, CooldownMs = 6000 },
        new GameCommand { Name = "honeypot", Description = "Deploy honeypot trap", Syntax = "honeypot [--port=N]", Category = CommandCategory.DEFENSE, MinLevel = 12, CpuCost = 30, RamCost = 40, CooldownMs = 15000 },
        new GameCommand { Name = "overload", Description = "Overload target with traffic", Syntax = "overload <ip> [--duration=N]", Category = CommandCategory.NETWORK, MinLevel = 12, CpuCost = 50, RamCost = 30, CooldownMs = 25000 },
        new GameCommand { Name = "crack", Description = "Crack password hash", Syntax = "crack <hash> [--wordlist=<file>]", Category = CommandCategory.CRYPTO, MinLevel = 13, CpuCost = 35, RamCost = 55, CooldownMs = 15000 },
        new GameCommand { Name = "rootkit", Description = "Install rootkit on target", Syntax = "rootkit <ip> [--stealth]", Category = CommandCategory.EXPLOIT, MinLevel = 14, CpuCost = 45, RamCost = 50, CooldownMs = 20000 },
        new GameCommand { Name = "botnet", Description = "Control botnet nodes", Syntax = "botnet [command] [--target=<ip>]", Category = CommandCategory.NETWORK, MinLevel = 15, CpuCost = 30, RamCost = 40, CooldownMs = 10000 },
        new GameCommand { Name = "dnshijack", Description = "Hijack DNS records", Syntax = "dnshijack <domain> --redirect=<ip>", Category = CommandCategory.EXPLOIT, MinLevel = 16, CpuCost = 35, RamCost = 45, CooldownMs = 18000 },
        new GameCommand { Name = "zero-day", Description = "Deploy zero-day exploit", Syntax = "zero-day <target> [--cve=<id>]", Category = CommandCategory.EXPLOIT, MinLevel = 18, CpuCost = 60, RamCost = 70, CooldownMs = 30000 },
        new GameCommand { Name = "ai-assist", Description = "AI-assisted hacking", Syntax = "ai-assist <query>", Category = CommandCategory.SYSTEM, MinLevel = 20, CpuCost = 40, RamCost = 80, CooldownMs = 15000 }
    };

    public static Dictionary<string, GameCommand> CommandMap { get; } = AllCommands.ToDictionary(c => c.Name);
}
