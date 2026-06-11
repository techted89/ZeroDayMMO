package com.zeroday.model

import kotlinx.serialization.Serializable

@Serializable
data class StoryEvent(
    val id: String,
    val title: String,
    val description: String,
    val requiredLevel: Int,
    val requiredCommands: List<String> = emptyList(),
    val rewards: EventRewards,
    val stages: List<EventStage>,
    val nextEventId: String? = null,
    val networkAccessGranted: List<String> = emptyList(),
    val newCommandsUnlocked: List<String> = emptyList(),
    val isRepeatable: Boolean = false
)

@Serializable
data class EventStage(
    val description: String,
    val objective: String,
    val hints: List<String> = emptyList(),
    val targetIp: String? = null,
    val requiredOutput: String? = null
)

@Serializable
data class EventRewards(
    val experience: Long = 0,
    val credits: Long = 0,
    val reputation: Int = 0,
    val cpuUpgrade: Int = 0,
    val ramUpgrade: Int = 0,
    val bandwidthUpgrade: Int = 0
)

object StorylineRegistry {
    val storylines: Map<String, StoryEvent> = listOf(
        StoryEvent(
            id = "intro_welcome",
            title = "Welcome to the Matrix",
            description = "You've just gained access to a basic terminal. Learn the ropes and discover your first network node.",
            requiredLevel = 1,
            rewards = EventRewards(experience = 50, credits = 100, reputation = 5, cpuUpgrade = 10, ramUpgrade = 20),
            stages = listOf(
                EventStage("A blinking cursor awaits your command.", "Use the 'help' command to see what you can do.",
                    listOf("Type: help", "Try: help scan")),
                EventStage("Good. Now let's see what's on your local network.", "Run 'scan localhost' to probe your first machine.",
                    listOf("Type: scan localhost", "Look at the open ports")),
                EventStage("Excellent! You found something. Try to connect.", "Run 'connect 127.0.0.1' to access the local shell.",
                    listOf("Type: connect 127.0.0.1"))
            ),
            nextEventId = "intro_network",
            networkAccessGranted = listOf("10.0.0.0/24"),
            newCommandsUnlocked = listOf("ls", "cat")
        ),

        StoryEvent(
            id = "intro_network",
            title = "Network Horizons",
            description = "You've proven basic competence. Time to explore the wider network and learn reconnaissance.",
            requiredLevel = 2,
            requiredCommands = listOf("ls", "cat"),
            rewards = EventRewards(experience = 120, credits = 250, reputation = 10, cpuUpgrade = 15, ramUpgrade = 30, bandwidthUpgrade = 10),
            stages = listOf(
                EventStage("There's a world beyond your local machine.", "Use 'ifconfig' to see your network interfaces.",
                    listOf("Type: ifconfig", "Note the network ranges")),
                EventStage("Now use 'nmap' to scan a remote subnet.", "Run: nmap 10.0.0.0/28",
                    listOf("Use -sS for a stealth scan", "Look for open ports")),
                EventStage("You found a server. Access it remotely.", "Use 'ssh admin@10.0.0.2' to log in.",
                    listOf("Try default credentials", "The password might be 'admin' or 'password'"))
            ),
            nextEventId = "exploit_basics",
            networkAccessGranted = listOf("10.0.1.0/24", "192.168.1.0/24"),
            newCommandsUnlocked = listOf("ifconfig", "nmap", "ping")
        ),

        StoryEvent(
            id = "exploit_basics",
            title = "First Blood",
            description = "You can access systems, but can you break into them? Learn your first exploitation techniques.",
            requiredLevel = 4,
            requiredCommands = listOf("nmap", "ssh"),
            rewards = EventRewards(experience = 300, credits = 500, reputation = 20, cpuUpgrade = 25, ramUpgrade = 40, bandwidthUpgrade = 15),
            stages = listOf(
                EventStage("A server on 10.0.1.5 has a vulnerable service.", "Use 'exploit 10.0.1.5 --payload=reverse_shell'",
                    listOf("Scan the target first", "Port 8080 might be vulnerable")),
                EventStage("You're in! Now find the data file.", "Use 'ls' then 'cat' to find the access codes.",
                    listOf("Look in /var/data", "The file is named access_codes.txt")),
                EventStage("Use the access codes to infiltrate deeper.", "Connect to the next node: 10.0.2.10",
                    listOf("Use the credentials you found", "The user is 'operator'"))
            ),
            nextEventId = "darknet_access",
            networkAccessGranted = listOf("10.0.2.0/24", "172.16.0.0/16"),
            newCommandsUnlocked = listOf("exploit", "traceroute", "whois")
        ),

        StoryEvent(
            id = "darknet_access",
            title = "The Darknet",
            description = "You've caught the attention of underground hackers. They've granted you access to the darknet marketplace where tasks and contracts await.",
            requiredLevel = 6,
            requiredCommands = listOf("exploit", "nmap"),
            rewards = EventRewards(experience = 600, credits = 1500, reputation = 40, cpuUpgrade = 30, ramUpgrade = 50, bandwidthUpgrade = 20),
            stages = listOf(
                EventStage("A hidden service at 172.16.0.1 hosts the darknet.", "Use 'connect 172.16.0.1' to access it.",
                    listOf("This is a .onion equivalent", "You'll need to be careful")),
                EventStage("The marketplace requires a reputation check.", "Check your 'status' and ensure you have enough rep.",
                    listOf("Reputation 50+ is needed", "Complete tasks to earn rep")),
                EventStage("Welcome to the underground. Tasks are now available.", "Type 'scan darknet' to see available contracts.",
                    listOf("Use 'scan darknet'", "Look for tasks matching your level"))
            ),
            nextEventId = "crypto_mastery",
            networkAccessGranted = listOf("darknet", "10.0.3.0/24", "203.0.113.0/24"),
            newCommandsUnlocked = listOf("bruteforce", "decrypt", "sqlmap")
        ),

        StoryEvent(
            id = "crypto_mastery",
            title = "Cryptographic Shadows",
            description = "The darknet deals demand encrypted communications. Master cryptography to handle sensitive contracts.",
            requiredLevel = 8,
            requiredCommands = listOf("decrypt", "bruteforce"),
            rewards = EventRewards(experience = 1000, credits = 3000, reputation = 60, cpuUpgrade = 40, ramUpgrade = 60, bandwidthUpgrade = 25),
            stages = listOf(
                EventStage("You intercepted an encrypted message.", "Use 'decrypt intercepted.enc' to crack it.",
                    listOf("It uses AES-256", "Try --method=aes")),
                EventStage("The message reveals a secure server address.", "Use 'sniff' to capture traffic on 10.0.3.0/24",
                    listOf("Run: sniff eth0", "Look for handshake patterns")),
                EventStage("Spoof your identity to access the secure server.", "Use 'spoof 10.0.3.50 --mac=de:ad:be:ef:00:00'",
                    listOf("MAC spoofing bypasses MAC filters", "Then connect to the server"))
            ),
            nextEventId = "advanced_exploitation",
            networkAccessGranted = listOf("10.0.4.0/24", "198.51.100.0/24"),
            newCommandsUnlocked = listOf("sniff", "spoof", "proxy", "encrypt")
        ),

        StoryEvent(
            id = "advanced_exploitation",
            title = "Advanced Persistent Threat",
            description = "You're becoming known in the underground. Time to leave your mark with advanced persistent access.",
            requiredLevel = 10,
            requiredCommands = listOf("sniff", "spoof", "exploit"),
            rewards = EventRewards(experience = 2000, credits = 5000, reputation = 100, cpuUpgrade = 50, ramUpgrade = 80, bandwidthUpgrade = 30),
            stages = listOf(
                EventStage("Install persistent access on target 10.0.4.20.", "Use 'backdoor 10.0.4.20 --persistent'",
                    listOf("Scan for open ports first", "Port 22 is likely open")),
                EventStage("Deploy a worm to spread through the subnet.", "Use 'worm 10.0.4.0/24 --spread'",
                    listOf("Worms need bandwidth", "They spread to vulnerable hosts")),
                EventStage("The authorities are tracing you! Deploy defenses.", "Use 'firewall --deny=10.0.0.0/8'",
                    listOf("Block incoming connections", "Then use 'trace' to see who's after you"))
            ),
            nextEventId = "elite_status",
            networkAccessGranted = listOf("10.0.5.0/24", "internet_backbone"),
            newCommandsUnlocked = listOf("backdoor", "worm", "firewall", "trace")
        ),

        StoryEvent(
            id = "elite_status",
            title = "Elite Status",
            description = "You've reached the upper echelons of the hacking world. Elite tools and lucrative contracts await.",
            requiredLevel = 14,
            requiredCommands = listOf("backdoor", "firewall", "worm"),
            rewards = EventRewards(experience = 5000, credits = 15000, reputation = 250, cpuUpgrade = 80, ramUpgrade = 120, bandwidthUpgrade = 50),
            stages = listOf(
                EventStage("Build your botnet army.", "Use 'botnet deploy --target=198.51.100.0/24'",
                    listOf("Recruit 10+ nodes", "Use backdoor to infect them first")),
                EventStage("Hijack a major DNS server.", "Use 'dnshijack zeroday.com --redirect=10.0.5.1'",
                    listOf("This requires rootkit access", "Use 'rootkit' on the DNS server first")),
                EventStage("Use your AI assistant for a complex task.", "Use 'ai-assist find vulnerable targets in 203.0.113.0/24'",
                    listOf("AI costs CPU and RAM", "It can automate complex scans"))
            ),
            nextEventId = null,
            networkAccessGranted = listOf("0.0.0.0/0"),
            newCommandsUnlocked = listOf("honeypot", "overload", "crack", "rootkit", "botnet", "dnshijack", "zero-day", "ai-assist")
        )
    ).associateBy { it.id }

    val storylineOrder: List<String> = listOf(
        "intro_welcome",
        "intro_network",
        "exploit_basics",
        "darknet_access",
        "crypto_mastery",
        "advanced_exploitation",
        "elite_status"
    )
}
