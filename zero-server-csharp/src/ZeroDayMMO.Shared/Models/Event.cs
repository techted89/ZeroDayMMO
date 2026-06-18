using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

public class StoryEvent
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("title")]
    public string Title { get; set; } = "";

    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("required_level")]
    public int RequiredLevel { get; set; }

    [JsonPropertyName("required_commands")]
    public List<string> RequiredCommands { get; set; } = new();

    [JsonPropertyName("rewards")]
    public EventRewards Rewards { get; set; } = new();

    [JsonPropertyName("stages")]
    public List<EventStage> Stages { get; set; } = new();

    [JsonPropertyName("next_event_id")]
    public string? NextEventId { get; set; }

    [JsonPropertyName("network_access_granted")]
    public List<string> NetworkAccessGranted { get; set; } = new();

    [JsonPropertyName("new_commands_unlocked")]
    public List<string> NewCommandsUnlocked { get; set; } = new();

    [JsonPropertyName("is_repeatable")]
    public bool IsRepeatable { get; set; } = false;
}

public class EventStage
{
    [JsonPropertyName("description")]
    public string Description { get; set; } = "";

    [JsonPropertyName("objective")]
    public string Objective { get; set; } = "";

    [JsonPropertyName("hints")]
    public List<string> Hints { get; set; } = new();

    [JsonPropertyName("target_ip")]
    public string? TargetIp { get; set; }

    [JsonPropertyName("required_output")]
    public string? RequiredOutput { get; set; }
}

public class EventRewards
{
    [JsonPropertyName("experience")]
    public long Experience { get; set; } = 0;

    [JsonPropertyName("credits")]
    public long Credits { get; set; } = 0;

    [JsonPropertyName("reputation")]
    public int Reputation { get; set; } = 0;

    [JsonPropertyName("cpu_upgrade")]
    public int CpuUpgrade { get; set; } = 0;

    [JsonPropertyName("ram_upgrade")]
    public int RamUpgrade { get; set; } = 0;

    [JsonPropertyName("bandwidth_upgrade")]
    public int BandwidthUpgrade { get; set; } = 0;
}

public static class StorylineRegistry
{
    public static Dictionary<string, StoryEvent> Storylines { get; } = new List<StoryEvent>
    {
        new()
        {
            Id = "intro_welcome",
            Title = "Welcome to the Matrix",
            Description = "You've just gained access to a basic terminal. Learn the ropes and discover your first network node.",
            RequiredLevel = 1,
            Rewards = new EventRewards { Experience = 50, Credits = 100, Reputation = 5, CpuUpgrade = 10, RamUpgrade = 20 },
            Stages = new()
            {
                new EventStage { Description = "A blinking cursor awaits your command.", Objective = "Use the 'help' command to see what you can do.", Hints = new() { "Type: help", "Try: help scan" } },
                new EventStage { Description = "Good. Now let's see what's on your local network.", Objective = "Run 'scan localhost' to probe your first machine.", Hints = new() { "Type: scan localhost", "Look at the open ports" } },
                new EventStage { Description = "Excellent! You found something. Try to connect.", Objective = "Run 'connect 127.0.0.1' to access the local shell.", Hints = new() { "Type: connect 127.0.0.1" } }
            },
            NextEventId = "intro_network",
            NetworkAccessGranted = new() { "10.0.0.0/24" },
            NewCommandsUnlocked = new() { "ls", "cat" }
        },
        new()
        {
            Id = "intro_network",
            Title = "Network Horizons",
            Description = "You've proven basic competence. Time to explore the wider network and learn reconnaissance.",
            RequiredLevel = 2,
            RequiredCommands = new() { "ls", "cat" },
            Rewards = new EventRewards { Experience = 120, Credits = 250, Reputation = 10, CpuUpgrade = 15, RamUpgrade = 30, BandwidthUpgrade = 10 },
            Stages = new()
            {
                new EventStage { Description = "There's a world beyond your local machine.", Objective = "Use 'ifconfig' to see your network interfaces.", Hints = new() { "Type: ifconfig", "Note the network ranges" } },
                new EventStage { Description = "Now use 'nmap' to scan a remote subnet.", Objective = "Run: nmap 10.0.0.0/28", Hints = new() { "Use -sS for a stealth scan", "Look for open ports" } },
                new EventStage { Description = "You found a server. Access it remotely.", Objective = "Use 'ssh admin@10.0.0.2' to log in.", Hints = new() { "Try default credentials", "The password might be 'admin' or 'password'" } }
            },
            NextEventId = "exploit_basics",
            NetworkAccessGranted = new() { "10.0.1.0/24", "192.168.1.0/24" },
            NewCommandsUnlocked = new() { "ifconfig", "nmap", "ping" }
        },
        new()
        {
            Id = "exploit_basics",
            Title = "First Blood",
            Description = "You can access systems, but can you break into them? Learn your first exploitation techniques.",
            RequiredLevel = 4,
            RequiredCommands = new() { "nmap", "ssh" },
            Rewards = new EventRewards { Experience = 300, Credits = 500, Reputation = 20, CpuUpgrade = 25, RamUpgrade = 40, BandwidthUpgrade = 15 },
            Stages = new()
            {
                new EventStage { Description = "A server on 10.0.1.5 has a vulnerable service.", Objective = "Use 'exploit 10.0.1.5 --payload=reverse_shell'", Hints = new() { "Scan the target first", "Port 8080 might be vulnerable" } },
                new EventStage { Description = "You're in! Now find the data file.", Objective = "Use 'ls' then 'cat' to find the access codes.", Hints = new() { "Look in /var/data", "The file is named access_codes.txt" } },
                new EventStage { Description = "Use the access codes to infiltrate deeper.", Objective = "Connect to the next node: 10.0.2.10", Hints = new() { "Use the credentials you found", "The user is 'operator'" } }
            },
            NextEventId = "darknet_access",
            NetworkAccessGranted = new() { "10.0.2.0/24", "172.16.0.0/16" },
            NewCommandsUnlocked = new() { "exploit", "traceroute", "whois" }
        },
        new()
        {
            Id = "darknet_access",
            Title = "The Darknet",
            Description = "You've caught the attention of underground hackers. They've granted you access to the darknet marketplace where tasks and contracts await.",
            RequiredLevel = 6,
            RequiredCommands = new() { "exploit", "nmap" },
            Rewards = new EventRewards { Experience = 600, Credits = 1500, Reputation = 40, CpuUpgrade = 30, RamUpgrade = 50, BandwidthUpgrade = 20 },
            Stages = new()
            {
                new EventStage { Description = "A hidden service at 172.16.0.1 hosts the darknet.", Objective = "Use 'connect 172.16.0.1' to access it.", Hints = new() { "This is a .onion equivalent", "You'll need to be careful" } },
                new EventStage { Description = "The marketplace requires a reputation check.", Objective = "Check your 'status' and ensure you have enough rep.", Hints = new() { "Reputation 50+ is needed", "Complete tasks to earn rep" } },
                new EventStage { Description = "Welcome to the underground. Tasks are now available.", Objective = "Type 'scan darknet' to see available contracts.", Hints = new() { "Use 'scan darknet'", "Look for tasks matching your level" } }
            },
            NextEventId = "crypto_mastery",
            NetworkAccessGranted = new() { "darknet", "10.0.3.0/24", "203.0.113.0/24" },
            NewCommandsUnlocked = new() { "bruteforce", "decrypt", "sqlmap" }
        },
        new()
        {
            Id = "crypto_mastery",
            Title = "Cryptographic Shadows",
            Description = "The darknet deals demand encrypted communications. Master cryptography to handle sensitive contracts.",
            RequiredLevel = 8,
            RequiredCommands = new() { "decrypt", "bruteforce" },
            Rewards = new EventRewards { Experience = 1000, Credits = 3000, Reputation = 60, CpuUpgrade = 40, RamUpgrade = 60, BandwidthUpgrade = 25 },
            Stages = new()
            {
                new EventStage { Description = "You intercepted an encrypted message.", Objective = "Use 'decrypt intercepted.enc' to crack it.", Hints = new() { "It uses AES-256", "Try --method=aes" } },
                new EventStage { Description = "The message reveals a secure server address.", Objective = "Use 'sniff' to capture traffic on 10.0.3.0/24", Hints = new() { "Run: sniff eth0", "Look for handshake patterns" } },
                new EventStage { Description = "Spoof your identity to access the secure server.", Objective = "Use 'spoof 10.0.3.50 --mac=de:ad:be:ef:00:00'", Hints = new() { "MAC spoofing bypasses MAC filters", "Then connect to the server" } }
            },
            NextEventId = "advanced_exploitation",
            NetworkAccessGranted = new() { "10.0.4.0/24", "198.51.100.0/24" },
            NewCommandsUnlocked = new() { "sniff", "spoof", "proxy", "encrypt" }
        },
        new()
        {
            Id = "advanced_exploitation",
            Title = "Advanced Persistent Threat",
            Description = "You're becoming known in the underground. Time to leave your mark with advanced persistent access.",
            RequiredLevel = 10,
            RequiredCommands = new() { "sniff", "spoof", "exploit" },
            Rewards = new EventRewards { Experience = 2000, Credits = 5000, Reputation = 100, CpuUpgrade = 50, RamUpgrade = 80, BandwidthUpgrade = 30 },
            Stages = new()
            {
                new EventStage { Description = "Install persistent access on target 10.0.4.20.", Objective = "Use 'backdoor 10.0.4.20 --persistent'", Hints = new() { "Scan for open ports first", "Port 22 is likely open" } },
                new EventStage { Description = "Deploy a worm to spread through the subnet.", Objective = "Use 'worm 10.0.4.0/24 --spread'", Hints = new() { "Worms need bandwidth", "They spread to vulnerable hosts" } },
                new EventStage { Description = "The authorities are tracing you! Deploy defenses.", Objective = "Use 'firewall --deny=10.0.0.0/8'", Hints = new() { "Block incoming connections", "Then use 'trace' to see who's after you" } }
            },
            NextEventId = "elite_status",
            NetworkAccessGranted = new() { "10.0.5.0/24", "internet_backbone" },
            NewCommandsUnlocked = new() { "backdoor", "worm", "firewall", "trace" }
        },
        new()
        {
            Id = "elite_status",
            Title = "Elite Status",
            Description = "You've reached the upper echelons of the hacking world. Elite tools and lucrative contracts await.",
            RequiredLevel = 14,
            RequiredCommands = new() { "backdoor", "firewall", "worm" },
            Rewards = new EventRewards { Experience = 5000, Credits = 15000, Reputation = 250, CpuUpgrade = 80, RamUpgrade = 120, BandwidthUpgrade = 50 },
            Stages = new()
            {
                new EventStage { Description = "Build your botnet army.", Objective = "Use 'botnet deploy --target=198.51.100.0/24'", Hints = new() { "Recruit 10+ nodes", "Use backdoor to infect them first" } },
                new EventStage { Description = "Hijack a major DNS server.", Objective = "Use 'dnshijack zeroday.com --redirect=10.0.5.1'", Hints = new() { "This requires rootkit access", "Use 'rootkit' on the DNS server first" } },
                new EventStage { Description = "Use your AI assistant for a complex task.", Objective = "Use 'ai-assist find vulnerable targets in 203.0.113.0/24'", Hints = new() { "AI costs CPU and RAM", "It can automate complex scans" } }
            },
            NextEventId = null,
            NetworkAccessGranted = new() { "0.0.0.0/0" },
            NewCommandsUnlocked = new() { "honeypot", "overload", "crack", "rootkit", "botnet", "dnshijack", "zero-day", "ai-assist" }
        }
    }.ToDictionary(e => e.Id);

    public static List<string> StorylineOrder { get; } = new()
    {
        "intro_welcome", "intro_network", "exploit_basics", "darknet_access",
        "crypto_mastery", "advanced_exploitation", "elite_status"
    };
}
