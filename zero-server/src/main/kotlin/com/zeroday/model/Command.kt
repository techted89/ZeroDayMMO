package com.zeroday.model

import kotlinx.serialization.Serializable

@Serializable
data class GameCommand(
    val name: String,
    val description: String,
    val syntax: String,
    val category: CommandCategory,
    val minLevel: Int,
    val cpuCost: Int,
    val ramCost: Int,
    val cooldownMs: Long = 0L,
    val isPassive: Boolean = false
)

@Serializable
enum class CommandCategory(val displayName: String) {
    BASIC("Basic"),
    RECON("Reconnaissance"),
    EXPLOIT("Exploitation"),
    DEFENSE("Defense"),
    NETWORK("Networking"),
    SYSTEM("System"),
    STEALTH("Stealth"),
    CRYPTO("Cryptography")
}

object CommandRegistry {
    val allCommands: List<GameCommand> = listOf(
        GameCommand("help", "Show available commands", "help [command]", CommandCategory.BASIC, 1, 0, 0),
        GameCommand("scan", "Scan local network for hosts", "scan [target]", CommandCategory.BASIC, 1, 5, 10, 1000L),
        GameCommand("connect", "Connect to a remote host", "connect <ip>", CommandCategory.BASIC, 1, 5, 5, 500L),
        GameCommand("whoami", "Display current user info", "whoami", CommandCategory.BASIC, 1, 0, 0),
        GameCommand("status", "Show system status", "status", CommandCategory.BASIC, 1, 0, 0),
        GameCommand("ls", "List directory contents", "ls [path]", CommandCategory.SYSTEM, 1, 2, 5),
        GameCommand("cat", "Read file contents", "cat <file>", CommandCategory.SYSTEM, 2, 2, 5),
        GameCommand("ifconfig", "Show network interfaces", "ifconfig", CommandCategory.NETWORK, 2, 3, 8),
        GameCommand("ping", "Ping a remote host", "ping <ip>", CommandCategory.NETWORK, 2, 3, 5, 2000L),
        GameCommand("nmap", "Port scan a target", "nmap [-sS|-sV] <ip>", CommandCategory.RECON, 3, 15, 30, 5000L),
        GameCommand("traceroute", "Trace route to host", "traceroute <ip>", CommandCategory.RECON, 3, 10, 20, 4000L),
        GameCommand("whois", "Lookup IP information", "whois <ip>", CommandCategory.RECON, 4, 8, 15, 3000L),
        GameCommand("ssh", "SSH into a remote host", "ssh <user>@<ip>", CommandCategory.NETWORK, 4, 10, 20, 3000L),
        GameCommand("ftp", "Connect via FTP", "ftp <ip>", CommandCategory.NETWORK, 4, 8, 15, 2500L),
        GameCommand("exploit", "Run exploit against target", "exploit <target> [--payload=<name>]", CommandCategory.EXPLOIT, 5, 25, 40, 8000L),
        GameCommand("bruteforce", "Brute force credentials", "bruteforce <target> [--threads=N]", CommandCategory.EXPLOIT, 6, 30, 50, 10000L),
        GameCommand("decrypt", "Decrypt captured data", "decrypt <file> [--key=<key>]", CommandCategory.CRYPTO, 6, 20, 35, 6000L),
        GameCommand("sqlmap", "SQL injection scanner", "sqlmap <url>", CommandCategory.EXPLOIT, 7, 25, 45, 8000L),
        GameCommand("backdoor", "Install backdoor on host", "backdoor <ip> [--persistent]", CommandCategory.EXPLOIT, 7, 20, 30, 12000L),
        GameCommand("sniff", "Capture network packets", "sniff [interface]", CommandCategory.RECON, 8, 15, 40, 5000L),
        GameCommand("spoof", "Spoof network identity", "spoof <ip> [--mac=<addr>]", CommandCategory.STEALTH, 8, 20, 25, 8000L),
        GameCommand("proxy", "Route traffic through proxy", "proxy <ip>:<port>", CommandCategory.STEALTH, 9, 15, 20, 4000L),
        GameCommand("encrypt", "Encrypt files/traffic", "encrypt <data> [--method=<alg>]", CommandCategory.CRYPTO, 9, 15, 25, 3000L),
        GameCommand("worm", "Deploy self-replicating worm", "worm <target> [--spread]", CommandCategory.EXPLOIT, 10, 40, 60, 20000L),
        GameCommand("firewall", "Deploy firewall rules", "firewall [--allow=<ip>] [--deny=<ip>]", CommandCategory.DEFENSE, 10, 25, 35, 8000L),
        GameCommand("trace", "Trace attacker origin", "trace [session]", CommandCategory.DEFENSE, 11, 20, 30, 6000L),
        GameCommand("honeypot", "Deploy honeypot trap", "honeypot [--port=N]", CommandCategory.DEFENSE, 12, 30, 40, 15000L),
        GameCommand("overload", "Overload target with traffic", "overload <ip> [--duration=N]", CommandCategory.NETWORK, 12, 50, 30, 25000L),
        GameCommand("crack", "Crack password hash", "crack <hash> [--wordlist=<file>]", CommandCategory.CRYPTO, 13, 35, 55, 15000L),
        GameCommand("rootkit", "Install rootkit on target", "rootkit <ip> [--stealth]", CommandCategory.EXPLOIT, 14, 45, 50, 20000L),
        GameCommand("botnet", "Control botnet nodes", "botnet [command] [--target=<ip>]", CommandCategory.NETWORK, 15, 30, 40, 10000L),
        GameCommand("dnshijack", "Hijack DNS records", "dnshijack <domain> --redirect=<ip>", CommandCategory.EXPLOIT, 16, 35, 45, 18000L),
        GameCommand("zero-day", "Deploy zero-day exploit", "zero-day <target> [--cve=<id>]", CommandCategory.EXPLOIT, 18, 60, 70, 30000L),
        GameCommand("ai-assist", "AI-assisted hacking", "ai-assist <query>", CommandCategory.SYSTEM, 20, 40, 80, 15000L)
    )

    val commandsByLevel: Map<Int, List<GameCommand>> = allCommands.groupBy { it.minLevel }
    val commandMap: Map<String, GameCommand> = allCommands.associateBy { it.name }
}
