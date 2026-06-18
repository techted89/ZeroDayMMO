using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public class CommandResult
{
    public string Command { get; init; } = "";
    public string Output { get; init; } = "";
    public CommandStatus Status { get; init; }
}

public enum CommandStatus
{
    SUCCESS,
    ERROR,
    PENDING
}

public class CommandService
{
    private readonly PlayerService _playerService;
    private readonly GameEventBus? _gameEventBus;



    public CommandService(PlayerService playerService, GameEventBus? gameEventBus = null)
    {
        _playerService = playerService;
        _gameEventBus = gameEventBus;
    }

    public async Task<CommandResult> ExecuteCommand(string playerId, string input)
    {
        var parts = input.Trim().Split(new[] { ' ', '\t' }, StringSplitOptions.RemoveEmptyEntries);
        if (parts.Length == 0)
            return new CommandResult { Command = "", Output = "No command entered", Status = CommandStatus.ERROR };

        var commandName = parts[0].ToLowerInvariant();
        var args = parts.Skip(1).ToArray();

        var player = _playerService.GetPlayer(playerId);
        if (player == null)
            return new CommandResult { Command = commandName, Output = "Player not found", Status = CommandStatus.ERROR };

        if (!player.UnlockedCommands.Contains(commandName))
            return new CommandResult { Command = commandName, Output = "Unknown command. Type 'help' for available commands.", Status = CommandStatus.ERROR };

        if (!CommandRegistry.CommandMap.TryGetValue(commandName, out var command))
            return new CommandResult { Command = commandName, Output = "Command not found in registry", Status = CommandStatus.ERROR };

        if (player.Level < command.MinLevel)
            return new CommandResult { Command = commandName, Output = $"You need level {command.MinLevel} to use this command.", Status = CommandStatus.ERROR };

        var canConsume = await _playerService.ConsumeResources(playerId, command.CpuCost, command.RamCost);
        if (canConsume != true)
            return new CommandResult { Command = commandName, Output = $"Insufficient resources. CPU needed: {command.CpuCost}, RAM needed: {command.RamCost}", Status = CommandStatus.ERROR };

        return await ProcessCommand(player, commandName, args);
    }

    private async Task<CommandResult> ProcessCommand(Player player, string cmd, string[] args)
    {
        return cmd switch
        {
            "help" => HandleHelp(player, args),
            "scan" => HandleScan(player, args),
            "connect" => await HandleConnect(player, args),
            "whoami" => HandleWhoami(player),
            "status" => HandleStatus(player),
            "ls" => HandleLs(player, args),
            "cat" => HandleCat(player, args),
            "ifconfig" => HandleIfconfig(player),
            "ping" => HandlePing(player, args),
            "nmap" => HandleNmap(player, args),
            "traceroute" => HandleTraceroute(player, args),
            "whois" => HandleWhois(player, args),
            "ssh" => HandleSsh(player, args),
            "ftp" => HandleFtp(player, args),
            "exploit" => await HandleExploit(player, args),
            "bruteforce" => HandleBruteforce(player, args),
            "decrypt" => HandleDecrypt(player, args),
            "sqlmap" => HandleSqlmap(player, args),
            "backdoor" => HandleBackdoor(player, args),
            "sniff" => HandleSniff(player, args),
            "spoof" => HandleSpoof(player, args),
            "proxy" => HandleProxy(player, args),
            "encrypt" => HandleEncrypt(player, args),
            "worm" => HandleWorm(player, args),
            "firewall" => HandleFirewall(player, args),
            "trace" => HandleTrace(player, args),
            "honeypot" => HandleHoneypot(player, args),
            "overload" => HandleOverload(player, args),
            "crack" => HandleCrack(player, args),
            "rootkit" => HandleRootkit(player, args),
            "botnet" => HandleBotnet(player, args),
            "dnshijack" => HandleDnshijack(player, args),
            "zero-day" => HandleZeroday(player, args),
            "ai-assist" => HandleAiAssist(player, args),
            _ => new CommandResult { Command = cmd, Output = $"Unknown command: {cmd}", Status = CommandStatus.ERROR }
        };
    }

    private CommandResult HandleHelp(Player player, string[] args)
    {
        if (args.Length > 0 && player.UnlockedCommands.Contains(args[0]))
        {
            if (!CommandRegistry.CommandMap.TryGetValue(args[0], out var cmd))
                return new CommandResult
                {
                    Command = "help",
                    Output = $"Command '{args[0]}' not found in registry.",
                    Status = CommandStatus.ERROR
                };
            return new CommandResult
            {
                Command = "help",
                Output = $"{cmd.Name}: {cmd.Description}\n  Syntax: {cmd.Syntax}\n  Category: {cmd.Category.DisplayName()}\n  Level Required: {cmd.MinLevel}\n  Cost: {cmd.CpuCost} CPU, {cmd.RamCost} RAM\n  Cooldown: {cmd.CooldownMs}ms",
                Status = CommandStatus.SUCCESS
            };
        }

        var categorized = player.UnlockedCommands
            .Select(name => CommandRegistry.CommandMap.GetValueOrDefault(name))
            .Where(c => c != null)
            .GroupBy(c => c!.Category.DisplayName())
            .ToDictionary(g => g.Key, g => g.ToList());

        var output = CategorizeOutput(categorized, player);
        return new CommandResult { Command = "help", Output = output, Status = CommandStatus.SUCCESS };
    }

    private static string CategorizeOutput(Dictionary<string, List<GameCommand>> categorized, Player player)
    {
        var sb = new System.Text.StringBuilder();
        sb.AppendLine("╔══════════════════════════════════════╗");
        sb.AppendLine("║         ZERODAY TERMINAL v1.0        ║");
        sb.AppendLine("╚══════════════════════════════════════╝");
        sb.AppendLine($"Available Commands ({player.UnlockedCommands.Count}/{CommandRegistry.AllCommands.Count}):");
        sb.AppendLine();

        foreach (var kvp in categorized)
        {
            sb.AppendLine($" [{kvp.Key}]");
            foreach (var cmd in kvp.Value)
            {
                sb.AppendLine($"  ├─ {cmd.Name.PadRight(15)} {cmd.Description}");
            }
            sb.AppendLine();
        }

        sb.Append("\nType 'help <command>' for details on a specific command.");
        return sb.ToString();
    }

    private CommandResult HandleScan(Player player, string[] args)
    {
        var target = args.Length > 0 ? args[0] : "localhost";

        if (target == "localhost" || target == "127.0.0.1")
        {
            return new CommandResult
            {
                Command = "scan",
                Output = "Scanning localhost (127.0.0.1)...\n" +
                         "┌─────────────────────────────────────┐\n" +
                         "│ Hostname: localhost                 │\n" +
                         "│ OS: ZeroDay Linux 5.x              │\n" +
                         "│ Uptime: 42 days                    │\n" +
                         "├─────────────────────────────────────┤\n" +
                         "│ PORT     STATE      SERVICE        │\n" +
                         "│ 22/tcp   open       ssh            │\n" +
                         "│ 80/tcp   open       http           │\n" +
                         "│ 443/tcp  open       https          │\n" +
                         "│ 8080/tcp filtered   http-proxy     │\n" +
                         "└─────────────────────────────────────┘",
                Status = CommandStatus.SUCCESS
            };
        }

        if (target == "darknet")
        {
            return new CommandResult
            {
                Command = "scan",
                Output = "Scanning darknet...\n" +
                         "┌──────────────────────────────────────────────┐\n" +
                         "│ Available Tasks:                             │\n" +
                         "│  t1  [EASY]    Scan Corporate Network        │\n" +
                         "│  t2  [MEDIUM]  Extract Customer Database     │\n" +
                         "│  t3  [HARD]    Deploy Ransomware Simulation  │\n" +
                         "│  t4  [HARD]    Botnet Recruitment Drive      │\n" +
                         "│  t5  [EXPERT]  Crack Government Cipher       │\n" +
                         "│  t6  [LEGEND]  Zero-Day Auction Hijack       │\n" +
                         "│  t7  [MEDIUM]  Defend Against APT            │\n" +
                         "│  t8  [HARD]    Darknet Forum Heist           │\n" +
                         "│  t9  [EXPERT]  Fireside Chat Intercept       │\n" +
                         "│  t10 [EXPERT]  Worm Containment              │\n" +
                         "│  t11 [TRIVIAL] Simple Data Grab              │\n" +
                         "│  t12 [EASY]    Brute Force Challenge         │\n" +
                         "│  t13 [MEDIUM]  Phishing Campaign             │\n" +
                         "│  t14 [HARD]    DDoS Coordination             │\n" +
                         "│  t15 [MEDIUM]  Bug Bounty: E-commerce        │\n" +
                         "└──────────────────────────────────────────────┘\n" +
                         "Type 'accept <task_id>' to take a contract.",
                Status = CommandStatus.SUCCESS
            };
        }

        if (player.DiscoveredNodes.Contains(target))
        {
            return new CommandResult
            {
                Command = "scan",
                Output = $"Target {target} is in your known network. Use 'nmap {target}' for detailed scan.",
                Status = CommandStatus.SUCCESS
            };
        }

        return new CommandResult
        {
            Command = "scan",
            Output = $"Scanning {target}...\nNo hosts found. You may not have access to this network segment.",
            Status = CommandStatus.SUCCESS
        };
    }

    private async Task<CommandResult> HandleConnect(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "connect", Output = "Usage: connect <ip>", Status = CommandStatus.ERROR };

        var target = args[0];
        if (player.DiscoveredNodes.Contains(target))
        {
            await _playerService.DiscoverNode(player.Id, target);
            return new CommandResult
            {
                Command = "connect",
                Output = $"Connected to {target}\nWelcome to {GetHostname(target)}.\nType 'help' for available commands on this system.",
                Status = CommandStatus.SUCCESS
            };
        }

        return new CommandResult
        {
            Command = "connect",
            Output = $"Connection failed: {target} is unreachable. Scan the network first.",
            Status = CommandStatus.ERROR
        };
    }

    private CommandResult HandleWhoami(Player player)
    {
        return new CommandResult
        {
            Command = "whoami",
            Output = $"User: {player.Username}\nID: {player.Id}\nLevel: {player.Level}\nReputation: {player.Reputation}\nLast Login: {player.LastLoginIp}",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleStatus(Player player)
    {
        return new CommandResult
        {
            Command = "status",
            Output = $"╔══════════════════════════════════════╗\n" +
                     $"║         SYSTEM STATUS               ║\n" +
                     $"╠══════════════════════════════════════╣\n" +
                     $"║ Player: {player.Username.PadRight(20)} ║\n" +
                     $"║ Level: {player.Level.ToString().PadRight(21)} ║\n" +
                     $"║ XP: {$"{player.Experience}/{player.ExperienceToNext}".PadRight(19)} ║\n" +
                     $"╠══════════════════════════════════════╣\n" +
                     $"║ Resources:                          ║\n" +
                     $"║  CPU: {$"{player.Cpu}/{player.MaxCpu} MHz".PadRight(20)} ║\n" +
                     $"║  RAM: {$"{player.Ram}/{player.MaxRam} MB".PadRight(20)} ║\n" +
                     $"║  BW:  {$"{player.Bandwidth}/{player.MaxBandwidth} Mbps".PadRight(20)} ║\n" +
                     $"║  Credits: {player.Credits.ToString().PadRight(16)} ║\n" +
                     $"║  Reputation: {player.Reputation.ToString().PadRight(13)} ║\n" +
                     $"╠══════════════════════════════════════╣\n" +
                     $"║ Storyline: {(player.CurrentStoryline ?? "None").PadRight(16)} ║\n" +
                     $"║ Nodes Discovered: {player.DiscoveredNodes.Count.ToString().PadRight(9)} ║\n" +
                     $"║ Commands Unlocked: {player.UnlockedCommands.Count.ToString().PadRight(10)} ║\n" +
                     $"║ Active Tasks: {player.ActiveTasks.Count.ToString().PadRight(14)} ║\n" +
                     $"╚══════════════════════════════════════╝",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleLs(Player player, string[] args)
    {
        var path = args.Length > 0 ? args[0] : ".";
        return new CommandResult
        {
            Command = "ls",
            Output = $"Directory listing for {path}:\n" +
                     "drwxr-xr-x  2 root root 4096 Jan 01 00:00 .\n" +
                     "drwxr-xr-x  2 root root 4096 Jan 01 00:00 ..\n" +
                     "-rw-r--r--  1 root root  256 Jan 01 00:00 notes.txt\n" +
                     "-rw-r--r--  1 root root 1024 Jan 01 00:00 config.json\n" +
                     "-rw-r--r--  1 root root   64 Jan 01 00:00 .bash_history\n" +
                     "drwxr-xr-x  2 root root 4096 Jan 01 00:00 data\n" +
                     "drwxr-xr-x  2 root root 4096 Jan 01 00:00 logs",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleCat(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "cat", Output = "Usage: cat <file>", Status = CommandStatus.ERROR };

        var file = args[0];
        var contents = FileContents.GetValueOrDefault(file,
            $"[File: {file}]\nACCESS DENIED: Insufficient permissions or file not found.");

        return new CommandResult { Command = "cat", Output = contents, Status = CommandStatus.SUCCESS };
    }

    private CommandResult HandleIfconfig(Player player)
    {
        return new CommandResult
        {
            Command = "ifconfig",
            Output = "eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500\n" +
                     "    inet 127.0.0.1  netmask 255.0.0.0  broadcast 127.255.255.255\n" +
                     "    inet6 ::1  prefixlen 128  scopeid 0x10<host>\n" +
                     "    ether 00:1a:2b:3c:4d:5e  txqueuelen 1000  (Ethernet)\n" +
                     "    RX packets 14253  bytes 15420341 (14.7 MB)\n" +
                     "    TX packets 8921  bytes 892143 (871.2 KB)\n\n" +
                     "wlan0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500\n" +
                     "    inet 10.0.0.42  netmask 255.255.255.0  broadcast 10.0.0.255\n" +
                     "    inet6 fe80::214:51ff:fe2a:3b1c  prefixlen 64  scopeid 0x20<link>\n" +
                     "    ether 00:14:51:2a:3b:1c  txqueuelen 1000  (Ethernet)\n" +
                     "    RX packets 38921  bytes 42153921 (40.2 MB)\n" +
                     "    TX packets 45212  bytes 5214321 (4.97 MB)\n\n" +
                     "Network ranges accessible:\n" +
                     "  - 10.0.0.0/24 (Local subnet)\n" +
                     "  - 10.0.1.0/24 (Corporate network)\n" +
                     "  - 172.16.0.0/16 (Darknet)",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandlePing(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "ping", Output = "Usage: ping <ip>", Status = CommandStatus.ERROR };

        var target = args[0];
        if (player.DiscoveredNodes.Contains(target))
        {
            return new CommandResult
            {
                Command = "ping",
                Output = $"PING {target} ({target}) 56(84) bytes of data.\n" +
                         $"64 bytes from {target}: icmp_seq=1 ttl=64 time=12.3 ms\n" +
                         $"64 bytes from {target}: icmp_seq=2 ttl=64 time=11.8 ms\n" +
                         $"64 bytes from {target}: icmp_seq=3 ttl=64 time=13.1 ms\n" +
                         $"64 bytes from {target}: icmp_seq=4 ttl=64 time=11.5 ms\n\n" +
                         $"--- {target} ping statistics ---\n" +
                         "4 packets transmitted, 4 received, 0% packet loss, time 3004ms\n" +
                         "rtt min/avg/max/mdev = 11.5/12.175/13.1/0.622 ms",
                Status = CommandStatus.SUCCESS
            };
        }

        return new CommandResult
        {
            Command = "ping",
            Output = $"ping: {target}: Temporary failure in name resolution",
            Status = CommandStatus.ERROR
        };
    }

    private CommandResult HandleNmap(Player player, string[] args)
    {
        var target = args.FirstOrDefault(a => !a.StartsWith("-"));
        if (target == null)
            return new CommandResult { Command = "nmap", Output = "Usage: nmap [-sS|-sV] <target>", Status = CommandStatus.ERROR };

        string body;
        if (player.DiscoveredNodes.Contains(target))
        {
            body = "PORT     STATE    SERVICE      VERSION\n" +
                   "22/tcp   open     ssh          OpenSSH 8.9p1\n" +
                   "80/tcp   open     http         Apache httpd 2.4.57\n" +
                   "443/tcp  open     https        nginx 1.24.0\n" +
                   "3306/tcp filtered mysql\n" +
                   "8080/tcp open     http-proxy   Squid proxy 6.0\n\n" +
                   "Host script results:\n" +
                   $"|_nbstat: NetBIOS name: {GetHostname(target)}, NetBIOS user: <unknown>\n" +
                   "| smb-os-discovery:\n" +
                   "|   OS: Linux 5.15\n" +
                   $"|   Computer name: {GetHostname(target)}\n" +
                   "|   Domain name: zeroday.local\n\n" +
                   "Nmap done: 1 IP address (1 host up) scanned in 3.42s";
        }
        else
        {
            body = $"PORT     STATE    SERVICE\nAll 1000 scanned ports on {target} are filtered\n\n" +
                   "Nmap done: 1 IP address (1 host up) scanned in 15.23s\n" +
                   "Note: Host seems to be behind a firewall. Try a different subnet.";
        }

        return new CommandResult
        {
            Command = "nmap",
            Output = $"Starting Nmap 7.94 ( https://nmap.org )\nScanning {target}\n{body}",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleTraceroute(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "traceroute", Output = "Usage: traceroute <ip>", Status = CommandStatus.ERROR };

        var target = args[0];
        return new CommandResult
        {
            Command = "traceroute",
            Output = $"traceroute to {target}, 30 hops max, 60 byte packets\n" +
                     " 1  10.0.0.1 (10.0.0.1)  0.512 ms  0.498 ms  0.512 ms\n" +
                     " 2  10.0.1.1 (10.0.1.1)  1.234 ms  1.198 ms  1.211 ms\n" +
                     " 3  172.16.0.1 (172.16.0.1)  3.456 ms  3.421 ms  3.389 ms\n" +
                     $" 4  {target} ({target})  5.678 ms  5.621 ms  5.599 ms",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleWhois(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "whois", Output = "Usage: whois <ip>", Status = CommandStatus.ERROR };

        var target = args[0];
        var subnet = target.Contains('.') ? target[..target.LastIndexOf('.')] : target;
        return new CommandResult
        {
            Command = "whois",
            Output = $"Whois lookup for {target}:\n" +
                     "┌──────────────────────────────────────────┐\n" +
                     "│ Organization: ZeroDay Industries         │\n" +
                     $"│ Network Range: {subnet}.0/24 │\n" +
                     "│ Country: US                              │\n" +
                     "│ Admin: admin@zeroday.local               │\n" +
                     "│ ISP: ZeroDay Communications              │\n" +
                     "│ Abuse: abuse@zeroday.local               │\n" +
                     "├──────────────────────────────────────────┤\n" +
                     "│ DNS Records:                             │\n" +
                     $"│   A: {target.PadRight(38)}│\n" +
                     $"│   PTR: {GetHostname(target)}.zeroday.local".PadRight(42) + "│\n" +
                     "│   MX: mail.zeroday.local                 │\n" +
                     "└──────────────────────────────────────────┘",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleSsh(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "ssh", Output = "Usage: ssh <user>@<ip>", Status = CommandStatus.ERROR };

        var target = args[0].Contains('@') ? args[0].Split('@')[1] : args[0];
        var now = DateTimeOffset.UtcNow.ToString("yyyy-MM-dd HH:mm:ss");
        return new CommandResult
        {
            Command = "ssh",
            Output = $"SSH connection established to {target}\n" +
                     "Authenticating...\n" +
                     $"Welcome {player.Username}@{GetHostname(target)}\n\n" +
                     $"Last login: {now}\n" +
                     $"[{player.Username}@{GetHostname(target)} ~]$",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleFtp(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "ftp", Output = "Usage: ftp <ip>", Status = CommandStatus.ERROR };

        var target = args[0];
        return new CommandResult
        {
            Command = "ftp",
            Output = $"Connected to {target}.\n" +
                     "220 ProFTPD 1.3.5 Server ready.\n" +
                     $"Name ({player.Username}): anonymous\n" +
                     "331 Anonymous login ok, send your email as password.\n" +
                     "Password:\n" +
                     "230 Anonymous access granted.\n" +
                     "Remote system type is UNIX.\n" +
                     "ftp> ls\n" +
                     "-rw-r--r--   1 root     root         1024 Jan 01 00:00 welcome.txt\n" +
                     "-rw-r--r--   1 root     root        16384 Jan 01 00:00 shares.zip\n" +
                     "ftp>",
            Status = CommandStatus.SUCCESS
        };
    }

    private async Task<CommandResult> HandleExploit(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "exploit", Output = "Usage: exploit <target> [--payload=<name>] [--use-zeroday]", Status = CommandStatus.ERROR };

        var target = args[0];
        var payload = args.FirstOrDefault(a => a.StartsWith("--payload="))?.Split('=')[1] ?? "reverse_shell";
        var useZeroday = args.Any(a => a == "--use-zeroday");

        if (useZeroday)
        {
            if (player.ActiveZeroDayExploits.Count == 0)
                return new CommandResult { Command = "exploit", Output = "No zero-day exploits loaded. Craft one in the Research Lab.", Status = CommandStatus.ERROR };

            var exploit = await _playerService.ConsumeZeroDayExploit(player.Id);
            if (exploit == null)
                return new CommandResult { Command = "exploit", Output = "Failed to load zero-day exploit.", Status = CommandStatus.ERROR };

            return new CommandResult
            {
                Command = "exploit",
                Output = $"  ███████╗██████╗  ██████╗ ██████╗ ██████╗  █████╗ ██╗   ██╗\n" +
                         $"  ╚══███╔╝██╔══██╗██╔════╝██╔═══██╗██╔══██╗██╔══██╗╚██╗ ██╔╝\n" +
                         $"    ███╔╝ ██║  ██║██║     ██║   ██║██║  ██║███████║ ╚████╔╝ \n" +
                         $"   ███╔╝  ██║  ██║██║     ██║   ██║██║  ██║██╔══██║  ╚██╔╝  \n" +
                         $"  ███████╗██████╔╝╚██████╗╚██████╔╝██████╔╝██║  ██║   ██║   \n" +
                         $"  ╚══════╝╚═════╝  ╚═════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝   \n\n" +
                         $"Deploying ZERO-DAY exploit against {target}...\n" +
                         $"Payload: {exploit.Name} ({exploit.Rarity.DisplayName()})\n\n" +
                         $"[+] Target identified: {GetHostname(target)}\n" +
                         "[+] No security measures detected - exploit bypassed all defenses\n" +
                         $"[+] Using zero-day: {exploit.Name} ({exploit.Description})\n" +
                         $"[+] Exploit power: {GetZeroDayPower(exploit)}\n" +
                         "[+] Session opened with FULL SYSTEM CONTROL\n" +
                         "[+] No traces left behind - system logs show normal activity\n\n" +
                         "Zero-day deployed successfully. Use remaining for critical targets only.",
                Status = CommandStatus.SUCCESS
            };
        }

        return new CommandResult
        {
            Command = "exploit",
            Output = $"Running exploit against {target}...\n" +
                     $"Payload: {payload}\n\n" +
                     $"[+] Target identified: {GetHostname(target)}\n" +
                     "[+] OS: Linux 5.15 - x86_64\n" +
                     $"[+] Vulnerability detected: CVE-2024-{Random.Shared.Next(1000, 10000)}\n" +
                     "[+] Exploiting...\n" +
                     "[+] Payload delivered successfully!\n" +
                     $"[+] Session opened: {player.Username}@{GetHostname(target)} (uid=0)\n" +
                     "[+] Root access granted!\n\n" +
                     $"System compromised. Type commands on {GetHostname(target)} or 'exit' to return.",
            Status = CommandStatus.SUCCESS
        };
    }

    private static int GetZeroDayPower(InventoryItem item) => item.Rarity switch
    {
        ItemRarity.RARE => 5,
        ItemRarity.EPIC => 10,
        ItemRarity.LEGENDARY => 99,
        _ => 3
    };

    private CommandResult HandleBruteforce(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "bruteforce", Output = "Usage: bruteforce <target> [--threads=N]", Status = CommandStatus.ERROR };

        var target = args[0];
        var threads = args.FirstOrDefault(a => a.StartsWith("--threads="))?.Split('=')[1];
        int.TryParse(threads, out var t);
        if (t <= 0) t = 4;

        return new CommandResult
        {
            Command = "bruteforce",
            Output = $"Brute forcing credentials for {target}\n" +
                     $"Using {t} threads...\n\n" +
                     "[+] Trying admin:admin... Failed\n" +
                     "[+] Trying root:toor... Failed\n" +
                     "[+] Trying admin:password123... Failed\n" +
                     "[+] Trying administrator:admin... Failed\n" +
                     "[+] Trying root:root... SUCCESS!\n\n" +
                     "Credentials found: root / root\n\n" +
                     $"Access granted to {target}",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleDecrypt(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "decrypt", Output = "Usage: decrypt <file> [--key=<key>]", Status = CommandStatus.ERROR };

        var file = args[0];
        var key = args.FirstOrDefault(a => a.StartsWith("--key="))?.Split('=')[1] ?? "bruteforce";
        var method = key == "bruteforce" ? "Brute force + Dictionary" : "AES-256-CBC with provided key";
        var encKey = string.Concat(Enumerable.Range(0, 16).Select(_ => "0123456789abcdef"[Random.Shared.Next(16)]));

        return new CommandResult
        {
            Command = "decrypt",
            Output = $"Decrypting {file}...\n" +
                     $"Method: {method}\n\n" +
                     "[+] Analyzing encryption...\n" +
                     "[+] Detected: AES-256-CBC\n" +
                     "[+] Cracking...\n" +
                     "[#] 10%\n" +
                     "[#####] 50%\n" +
                     "[##########] 100%\n" +
                     "[+] Decryption successful!\n\n" +
                     "Decrypted content:\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                     "CLASSIFIED ACCESS CODES\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                     "Server: 10.0.2.50\n" +
                     "User: db_admin\n" +
                     "Pass: D4t4b@s3S3cur3!\n\n" +
                     $"Encryption key: {encKey}\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleSqlmap(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "sqlmap", Output = "Usage: sqlmap <url>", Status = CommandStatus.ERROR };

        var url = args[0];
        return new CommandResult
        {
            Command = "sqlmap",
            Output = $"sqlmap (1.7.12) - Automatic SQL injection tool\n\n" +
                     $"[*] targeting: {url}\n" +
                     "[+] GET parameter 'id' appears to be injectable\n" +
                     "[+] Testing MySQL...\n" +
                     "[+] Technique: UNION query SQLi\n" +
                     "[+] Payload: id=1 UNION SELECT 1,2,3,group_concat(table_name),5 FROM information_schema.tables\n\n" +
                     "[+] Extracted database tables:\n" +
                     "  - users (42 records)\n" +
                     "  - products (156 records)\n" +
                     "  - orders (891 records)\n" +
                     "  - credentials (3 records)\n\n" +
                     "[+] Dumping 'credentials' table:\n" +
                     "  1 | admin    | 5f4dcc3b5aa765d61d8327deb882cf99\n" +
                     "  2 | operator | 482c811da5d5b4bc6d497ffa98491e38\n" +
                     "  3 | root     | $2y$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy\n\n" +
                     "[+] Table dumped! Hash cracking recommended.",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleBackdoor(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "backdoor", Output = "Usage: backdoor <ip> [--persistent]", Status = CommandStatus.ERROR };

        var target = args[0];
        var persistent = args.Any(a => a == "--persistent");
        var pid = Random.Shared.Next(1000, 10000);
        var port = Random.Shared.Next(40000, 60001);

        return new CommandResult
        {
            Command = "backdoor",
            Output = $"Installing backdoor on {target}...\n" +
                     $"{(persistent ? "[+] Persistent mode enabled" : "[+] Standard backdoor")}\n\n" +
                     "[+] Establishing foothold...\n" +
                     $"[+] Creating hidden process: [kworker/3:1+events] (PID: {pid})\n" +
                     $"[+] Opening reverse port: {port}\n" +
                     "[+] Backdoor installed successfully!\n" +
                     $"[+] Persistence: {(persistent ? "System reboot resistant (cron job + init.d script)" : "Non-persistent (will be removed on reboot)")}\n\n" +
                     $"Use 'connect {target}' to access the backdoor shell.",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleSniff(Player player, string[] args)
    {
        var iface = args.Length > 0 ? args[0] : "eth0";
        return new CommandResult
        {
            Command = "sniff",
            Output = $"Packet capture on {iface}...\n" +
                     "Capturing 20 packets...\n\n" +
                     "┌────────────────────────────────────────────────────────────────┐\n" +
                     "│  #  TIME        SOURCE           DESTINATION      PROTO  SIZE │\n" +
                     "├────────────────────────────────────────────────────────────────┤\n" +
                     "│  1  0.000123    10.0.0.42        10.0.0.1          TCP     64 │\n" +
                     "│  2  0.000456    10.0.0.1         10.0.0.42         TCP     64 │\n" +
                     "│  3  0.001234    10.0.0.42        203.0.113.1       TCP   1280 │\n" +
                     "│  4  0.001567    10.0.0.42        10.0.1.5          SSH    256 │\n" +
                     "│  5  0.002345    10.0.1.5         10.0.0.42         SSH    256 │\n" +
                     "│  6  0.003456    10.0.0.42        10.0.1.200        HTTP   512 │\n" +
                     "│  7  0.004567    10.0.1.200       10.0.0.42         HTTP   512 │\n" +
                     "│  8  0.005678    10.0.0.42        172.16.0.1        TLS   1024 │\n" +
                     "│  9  0.006789    172.16.0.1       10.0.0.42         TLS   1024 │\n" +
                     "│ 10  0.007890    10.0.0.42        10.0.2.10         SMB    316 │\n" +
                     "└────────────────────────────────────────────────────────────────┘\n\n" +
                     "[!] Sensitive data detected in packet #6: POST credentials to /login\n" +
                     "[!] Session cookie captured from packet #8",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleSpoof(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "spoof", Output = "Usage: spoof <ip> [--mac=<addr>]", Status = CommandStatus.ERROR };

        var ip = args[0];
        var mac = args.FirstOrDefault(a => a.StartsWith("--mac="))?.Split('=')[1] ?? "00:11:22:33:44:55";

        return new CommandResult
        {
            Command = "spoof",
            Output = $"Spoofing identity as {ip}...\n" +
                     $"MAC: {mac}\n\n" +
                     "[+] ARP cache poisoned on local network\n" +
                     $"[+] IP identity spoofed: {player.Username}@{ip}\n" +
                     $"[+] MAC address changed: {mac}\n" +
                     $"[+] All outgoing traffic will now appear to originate from {ip}\n\n" +
                     "Use 'status' to verify your spoofed identity.\n" +
                     "Use 'proxy <ip>:<port>' to chain through a proxy for additional anonymity.",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleProxy(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "proxy", Output = "Usage: proxy <ip>:<port>", Status = CommandStatus.ERROR };

        var proxyAddr = args[0];
        return new CommandResult
        {
            Command = "proxy",
            Output = $"Configuring proxy chain...\n" +
                     $"Proxy: {proxyAddr}\n\n" +
                     "[+] SOCKS5 proxy configured\n" +
                     $"[+] Traffic routing: {player.Username} -> {proxyAddr} -> internet\n" +
                     "[+] Anonymity level: Medium (single hop)\n\n" +
                     "Available proxy chains:\n" +
                     $"  1. {proxyAddr} (current)\n" +
                     "  2. Tor network (requires darknet access)\n" +
                     "  3. VPN tunnel (requires level 12+)\n\n" +
                     "Use 'proxy --chain <n>' to switch chains.",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleEncrypt(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "encrypt", Output = "Usage: encrypt <data> [--method=<alg>]", Status = CommandStatus.ERROR };

        var data = args[0];
        var method = args.FirstOrDefault(a => a.StartsWith("--method="))?.Split('=')[1] ?? "aes-256";
        var encrypted = string.Concat(Enumerable.Range(0, 64).Select(_ => "0123456789abcdef"[Random.Shared.Next(16)]));
        var key = string.Concat(Enumerable.Range(0, 32).Select(_ => "0123456789abcdef"[Random.Shared.Next(16)]));

        return new CommandResult
        {
            Command = "encrypt",
            Output = $"Encrypting data using {method}...\n" +
                     $"Data: '{data}'\n\n" +
                     "[+] Generating key...\n" +
                     "[+] Encryption complete!\n\n" +
                     $"Encrypted output: {encrypted}\n" +
                     $"Method: {method}\n" +
                     $"Key: {key} (store securely)",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleWorm(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "worm", Output = "Usage: worm <target> [--spread]", Status = CommandStatus.ERROR };

        var target = args[0];
        var spread = args.Any(a => a == "--spread");

        var output = $"Deploying worm to {target}...\n" +
                     "[+] Payload: zeroday_worm_v1.0\n" +
                     $"{(spread ? "[+] Auto-spread mode ENABLED" : "[+] Targeted deployment")}\n\n" +
                     $"[+] Infecting {target}...\n" +
                     $"[+] Worm installed on {GetHostname(target)}\n" +
                     "[+] Payload: keylogger, backdoor, cryptominer\n";

        if (spread)
        {
            output += "\n[+] Spreading to adjacent nodes...\n" +
                      "[+] 10.0.0.5 - INFECTED\n" +
                      "[+] 10.0.0.3 - INFECTED\n" +
                      "[+] 10.0.1.5 - INFECTED\n" +
                      "[+] 10.0.1.10 - INFECTED\n" +
                      "[!] Worm propagation detected! Firewalls may alert.\n";
        }

        output += "\nWorm active. Use 'botnet status' to check infected hosts.";
        return new CommandResult { Command = "worm", Output = output, Status = CommandStatus.SUCCESS };
    }

    private CommandResult HandleFirewall(Player player, string[] args)
    {
        var allow = args.FirstOrDefault(a => a.StartsWith("--allow="))?.Split('=')[1];
        var deny = args.FirstOrDefault(a => a.StartsWith("--deny="))?.Split('=')[1];

        var output = "Configuring firewall...\n\n";
        if (allow != null) output += $"[+] ALLOW rule added: {allow}\n";
        if (deny != null) output += $"[+] DENY rule added: {deny}\n";
        if (allow == null && deny == null) output += "[+] No rules specified. Showing current rules.\n";

        output += "\nCurrent firewall rules:\n" +
                  "┌──────┬──────────────────┬──────────┬──────────┐\n" +
                  "│  #   │  SOURCE          │  PORT    │  ACTION  │\n" +
                  "├──────┼──────────────────┼──────────┼──────────┤\n" +
                  "│  1   │  0.0.0.0/0       │  22      │  ALLOW   │\n" +
                  "│  2   │  0.0.0.0/0       │  80      │  ALLOW   │\n" +
                  "│  3   │  0.0.0.0/0       │  443     │  ALLOW   │\n" +
                  "│  4   │  127.0.0.0/8     │  *       │  ALLOW   │\n";

        if (deny != null)
            output += $"│  5   │  {deny.PadRight(16)}│  *       │  DENY    │\n";

        output += "└──────┴──────────────────┴──────────┴──────────┘";

        return new CommandResult { Command = "firewall", Output = output, Status = CommandStatus.SUCCESS };
    }

    private CommandResult HandleTrace(Player player, string[] args)
    {
        var session = args.Length > 0 ? args[0] : "all";
        var suspicious = Random.Shared.Next(1, 6);

        return new CommandResult
        {
            Command = "trace",
            Output = $"Tracing {(session == "all" ? "all active sessions" : $"session {session}")}...\n\n" +
                     "[+] Scanning for intrusion attempts...\n" +
                     $"[+] {suspicious} suspicious connections found\n\n" +
                     "┌──────────────────────────────────────────────────────────┐\n" +
                     "│  SESSION  SOURCE IP       COUNTRY   STATUS               │\n" +
                     "├──────────────────────────────────────────────────────────┤\n" +
                     $"│  0x{RandHex(4)}   {RandIp()}    RU      Active - data exfiltration │\n" +
                     $"│  0x{RandHex(4)}   {RandIp()}    CN      Active - port scanning     │\n" +
                     $"│  0x{RandHex(4)}   {RandIp()}    US      Blocked - firewall rule    │\n" +
                     "└──────────────────────────────────────────────────────────┘\n\n" +
                     $"[!] Trace route to 185.{Random.Shared.Next(10, 100)}.{Random.Shared.Next(1, 255)}.{Random.Shared.Next(1, 255)}\n" +
                     "[!] Attacker likely using VPN/proxy chain",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleHoneypot(Player player, string[] args)
    {
        var port = args.FirstOrDefault(a => a.StartsWith("--port="))?.Split('=')[1] ?? "8080";
        return new CommandResult
        {
            Command = "honeypot",
            Output = $"Deploying honeypot on port {port}...\n\n" +
                     "[+] Honeypot type: SSH + HTTP (emulated services)\n" +
                     $"[+] Listening on 0.0.0.0:{port}\n" +
                     "[+] Fake files planted: admin_credentials.txt, database_backup.sql\n" +
                     "[+] Alerting configured: notification on compromise attempt\n\n" +
                     "Honeypot active. Captured interactions will appear in logs.\n" +
                     "Use 'cat logs/honeypot.log' to review captures.",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleOverload(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "overload", Output = "Usage: overload <ip> [--duration=N]", Status = CommandStatus.ERROR };

        var target = args[0];
        var durationStr = args.FirstOrDefault(a => a.StartsWith("--duration="))?.Split('=')[1];
        int.TryParse(durationStr, out var duration);
        if (duration <= 0) duration = 30;

        return new CommandResult
        {
            Command = "overload",
            Output = $"Launching DDoS attack on {target}...\n" +
                     $"Duration: {duration}s\n" +
                     $"[+] Botnet nodes engaged: {Random.Shared.Next(5, 51)}\n" +
                     "[+] Attack vector: UDP flood + SYN flood + HTTP flood\n\n" +
                     $"Bandwidth usage: {Random.Shared.Next(50, 501)} Mbps\n" +
                     $"Packets sent: {Random.Shared.Next(10000, 100001)}\n\n" +
                     $"[!] Target {target} is experiencing service degradation\n" +
                     $"[!] Estimated downtime: {duration + Random.Shared.Next(10, 31)} seconds\n\n" +
                     "Attack in progress... Type 'status' to monitor.",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleCrack(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "crack", Output = "Usage: crack <hash> [--wordlist=<file>]", Status = CommandStatus.ERROR };

        var hash = args[0];
        var wordlist = args.FirstOrDefault(a => a.StartsWith("--wordlist="))?.Split('=')[1] ?? "rockyou.txt";

        return new CommandResult
        {
            Command = "crack",
            Output = $"Cracking hash: {hash[..Math.Min(32, hash.Length)]}...\n" +
                     $"Wordlist: {wordlist}\n\n" +
                     "[+] Hash type detected: SHA-256\n" +
                     "[+] Starting crack...\n" +
                     "[+] Trying passwords from dictionary...\n\n" +
                     "[#] 1% - Trying 'password'...\n" +
                     "[#####] 12% - Trying '123456'...\n" +
                     "[########] 25% - Trying 'qwerty'...\n" +
                     "[###########] 35% - Trying 'letmein'...\n" +
                     "[###############] 50% - Trying 'admin'...\n" +
                     "[##################] 65% - Trying 'welcome'...\n" +
                     "[######################] 80% - Trying 'monkey'...\n" +
                     "[########################] 95% - Trying 'dragon'...\n\n" +
                     $"[+] CRACKED! Password: P@ssw0rd!{Random.Shared.Next(10, 100)}\n\n" +
                     $"Time elapsed: {Random.Shared.Next(2, 31)}s\n" +
                     $"Attempts: {Random.Shared.Next(10000, 1000001)}",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleRootkit(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "rootkit", Output = "Usage: rootkit <ip> [--stealth]", Status = CommandStatus.ERROR };

        var target = args[0];
        var stealth = args.Any(a => a == "--stealth");
        var port = Random.Shared.Next(30000, 65001);

        return new CommandResult
        {
            Command = "rootkit",
            Output = $"Installing rootkit on {target}...\n" +
                     $"{(stealth ? "[+] Stealth mode: kernel module will hide from detection" : "[+] Standard installation")}\n\n" +
                     "[+] Kernel module loaded: hideme.ko\n" +
                     "[+] Files hidden: /var/log/auth.log entries cleaned\n" +
                     "[+] Process hidden: /usr/sbin/sshd - modified binary\n" +
                     $"[+] Backdoor port: {port} (UDP)\n" +
                     $"[+] Rootkit installed {(stealth ? "with zero detections" : "(may be detected by rkhunter)")}\n\n" +
                     "System fully compromised. You have persistent root access.",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleBotnet(Player player, string[] args)
    {
        var subcmd = args.Length > 0 ? args[0] : "status";

        return subcmd switch
        {
            "status" => new CommandResult
            {
                Command = "botnet",
                Output = "Botnet Status\n" +
                         "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                         $"Active nodes: {Random.Shared.Next(3, 51)}\n" +
                         $"Total bandwidth: {Random.Shared.Next(100, 1001)} Mbps\n" +
                         $"Total CPU: {Random.Shared.Next(500, 5001)} MHz\n" +
                         $"Total RAM: {Random.Shared.Next(1024, 16385)} MB\n\n" +
                         "Node distribution:\n" +
                         $"  ├─ Servers: {Random.Shared.Next(1, 11)}\n" +
                         $"  ├─ Workstations: {Random.Shared.Next(1, 21)}\n" +
                         $"  ├─ IoT devices: {Random.Shared.Next(1, 16)}\n" +
                         $"  └─ Mobile: {Random.Shared.Next(0, 6)}\n\n" +
                         "Commands: botnet scan, botnet attack <target>, botnet update",
                Status = CommandStatus.SUCCESS
            },
            "scan" => new CommandResult
            {
                Command = "botnet",
                Output = $"Scanning for vulnerable hosts to add to botnet... Found {Random.Shared.Next(2, 11)} new targets.",
                Status = CommandStatus.SUCCESS
            },
            "attack" => new CommandResult
            {
                Command = "botnet",
                Output = $"Botnet attack initiated on {(args.Length > 1 ? args[1] : "unknown")}. {Random.Shared.Next(10, 101)} nodes participating.",
                Status = CommandStatus.SUCCESS
            },
            "update" => new CommandResult
            {
                Command = "botnet",
                Output = $"Updating botnet payload across all nodes... {Random.Shared.Next(3, 51)} nodes updated successfully.",
                Status = CommandStatus.SUCCESS
            },
            _ => new CommandResult
            {
                Command = "botnet",
                Output = $"Unknown botnet command: {subcmd}",
                Status = CommandStatus.ERROR
            }
        };
    }

    private CommandResult HandleDnshijack(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "dnshijack", Output = "Usage: dnshijack <domain> --redirect=<ip>", Status = CommandStatus.ERROR };

        var domain = args[0];
        var redirect = args.FirstOrDefault(a => a.StartsWith("--redirect="))?.Split('=')[1];
        if (redirect == null)
            return new CommandResult { Command = "dnshijack", Output = "Usage: dnshijack <domain> --redirect=<ip>", Status = CommandStatus.ERROR };

        return new CommandResult
        {
            Command = "dnshijack",
            Output = $"DNS Hijack initiated on {domain}...\n" +
                     $"Redirecting to {redirect}\n\n" +
                     "[+] DNS cache poisoned for {domain}\n" +
                     "[+] TTL set to 0 (immediate propagation)\n" +
                     $"[+] All queries for {domain} will resolve to {redirect}\n" +
                     "[+] SSL certificates will show warning (users may ignore)\n\n" +
                     $"Hijack active. Traffic from {domain} is now routed through your proxy.",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleZeroday(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "zero-day", Output = "Usage: zero-day <target> [--cve=<id>]", Status = CommandStatus.ERROR };

        var target = args[0];
        var cve = args.FirstOrDefault(a => a.StartsWith("--cve="))?.Split('=')[1] ?? $"CVE-2024-{Random.Shared.Next(1000, 10000)}";

        return new CommandResult
        {
            Command = "zero-day",
            Output = $"Deploying zero-day exploit against {target}...\n" +
                     $"CVE: {cve}\n" +
                     "Type: Remote Code Execution\n" +
                     "Severity: CRITICAL (10.0)\n\n" +
                     $"[+] Vulnerability confirmed: {cve}\n" +
                     "[+] No known signature - bypassing all AV/IDS\n" +
                     "[+] Shellcode injected...\n" +
                     "[+] SYSTEM privilege escalation successful!\n\n" +
                     "  ███████╗██████╗  ██████╗ ██████╗ ██████╗  █████╗ ██╗   ██╗\n" +
                     "  ╚══███╔╝██╔══██╗██╔════╝██╔═══██╗██╔══██╗██╔══██╗╚██╗ ██╔╝\n" +
                     "    ███╔╝ ██║  ██║██║     ██║   ██║██║  ██║███████║ ╚████╔╝ \n" +
                     "   ███╔╝  ██║  ██║██║     ██║   ██║██║  ██║██╔══██║  ╚██╔╝  \n" +
                     "  ███████╗██████╔╝╚██████╗╚██████╔╝██████╔╝██║  ██║   ██║   \n" +
                     "  ╚══════╝╚═════╝  ╚═════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝   \n\n" +
                     "Zero-day successfully deployed. Full system control achieved.",
            Status = CommandStatus.SUCCESS
        };
    }

    private CommandResult HandleAiAssist(Player player, string[] args)
    {
        if (args.Length == 0)
            return new CommandResult { Command = "ai-assist", Output = "Usage: ai-assist <query>", Status = CommandStatus.ERROR };

        var query = string.Join(" ", args);
        return new CommandResult
        {
            Command = "ai-assist",
            Output = $"AI Assist (ZeroDay Neural Engine v3.0)\n" +
                     $"Query: {query}\n\n" +
                     "[+] Processing...\n" +
                     "[+] Analyzing network topology...\n" +
                     "[+] Cross-referencing vulnerability databases...\n\n" +
                     "Analysis Results:\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                     "• Recommended targets: 10.0.1.200 (webapp - SQLi)\n" +
                     "• Vulnerable services: Apache 2.4.49 (path traversal)\n" +
                     "• Suggested exploit: exploit 10.0.1.200 --payload=shell_exec\n" +
                     "• Estimated success rate: 87.3%\n" +
                     "• Risk level: MEDIUM\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                     $"AI processing complete. {Random.Shared.Next(100, 501)} CPU cycles consumed.",
            Status = CommandStatus.SUCCESS
        };
    }

    private static string GetHostname(string ip)
    {
        return ip switch
        {
            "0.0.0.0/0" => "global",
            "127.0.0.1" => "localhost",
            "10.0.0.1" => "gateway",
            "10.0.0.2" => "fileserver",
            "10.0.0.3" => "mailserver",
            "10.0.0.5" => "printer",
            "10.0.0.42" => "client-42",
            "10.0.1.1" => "corp-router",
            "10.0.1.5" => "vpn-server",
            "10.0.1.10" => "dev-db",
            "10.0.1.200" => "webapp",
            "10.0.2.10" => "internal-fs",
            "10.0.2.50" => "hr-database",
            "172.16.0.1" => "darknet-gateway",
            "172.16.0.50" => "marketplace",
            "172.16.0.100" => "forum",
            "203.0.113.1" => "backbone-router",
            "203.0.113.50" => "gov-server",
            "198.51.100.1" => "isp-router",
            "10.0.3.1" => "cloud-gateway",
            "10.0.3.100" => "cloud-server",
            "10.0.3.200" => "ecommerce",
            "10.0.4.1" => "mil-router",
            "10.0.4.20" => "mil-server",
            "10.0.5.1" => "sat-uplink",
            "10.0.5.100" => "corp-hq",
            _ => $"host-{ip.Replace('.', '-')}"
        };
    }

    private string RandHex(int len) =>
        string.Concat(Enumerable.Range(0, len).Select(_ => "0123456789abcdef"[Random.Shared.Next(16)]));

    private string RandIp() =>
        string.Join(".", Enumerable.Range(0, 4).Select(_ => Random.Shared.Next(10, 256)));

    private static readonly Dictionary<string, string> FileContents = new()
    {
        ["notes.txt"] = "=== Personal Notes ===\n" +
                        "TODO:\n" +
                        "- Patch SSH vulnerability on gateway\n" +
                        "- Update firewall rules\n" +
                        "- Check darknet for new contracts\n" +
                        "Credentials for backup server: admin / b4ckup!23",
        ["config.json"] = "{\n" +
                          "  \"server\": \"10.0.1.5\",\n" +
                          "  \"port\": 8080,\n" +
                          "  \"debug\": false,\n" +
                          "  \"admin_users\": [\"root\", \"admin\"],\n" +
                          "  \"features\": {\n" +
                          "    \"remote_access\": true,\n" +
                          "    \"auto_update\": false\n" +
                          "  }\n" +
                          "}",
        ["credentials.xlsx"] = "[Extracted from credentials.xlsx]\n" +
                               "USER          | PASSWORD          | ACCESS LEVEL\n" +
                               "admin         | P@ssw0rd!42       | ADMIN\n" +
                               "john.smith    | Welcome1!         | USER\n" +
                               "jane.doe      | Summer2024!       | USER\n" +
                               "root          | R00tP@ss!         | ROOT",
        ["network_map.pdf"] = "Network Topology:\n" +
                              "  Internet <-> ISP Router (198.51.100.1)\n" +
                              "    -> Corporate Gateway (10.0.1.1)\n" +
                              "      -> Web Server (10.0.1.200)\n" +
                              "      -> Database (10.0.1.10)\n" +
                              "      -> VPN Server (10.0.1.5)\n" +
                              "    -> Darknet Gateway (172.16.0.1)\n" +
                              "      -> Marketplace (172.16.0.50)\n" +
                              "      -> Forum (172.16.0.100)\n" +
                              "    -> Military Network (10.0.4.0/24)\n" +
                              "      -> Command Server (10.0.4.20)\n" +
                              "    -> Satellite Uplink (10.0.5.1)",
        ["flag.txt"] = "Congratulations!\nFlag: ZERODAY{you_found_the_flag}",
        ["access_codes.txt"] = "ACCESS CODES\n" +
                               "============\n" +
                               "Primary Server: 10.0.2.10\n" +
                               "  User: operator\n" +
                               "  Pass: 0p3r4t0r!@#\n\n" +
                               "Database: 10.0.2.50\n" +
                               "  User: db_admin\n" +
                               "  Pass: D4t4b@s3S3cur3!"
    };
}
