package com.zeroday.service

import com.zeroday.model.*

class CommandService(
    private val playerService: PlayerService,
    private val gameEventBus: GameEventBus? = null
) {

    suspend fun executeCommand(playerId: String, input: String): CommandResult {
        val parts = input.trim().split("\\s+".toRegex())
        if (parts.isEmpty()) return CommandResult("", "No command entered", CommandStatus.ERROR)

        val commandName = parts[0].lowercase()
        val args = parts.drop(1).toTypedArray()

        val player = playerService.getPlayer(playerId) ?: return CommandResult(commandName, "Player not found", CommandStatus.ERROR)

        if (!player.unlockedCommands.contains(commandName)) {
            return CommandResult(commandName, "Unknown command. Type 'help' for available commands.", CommandStatus.ERROR)
        }

        val command = CommandRegistry.commandMap[commandName] ?: return CommandResult(commandName, "Command not found in registry", CommandStatus.ERROR)

        if (player.level < command.minLevel) {
            return CommandResult(commandName, "You need level ${command.minLevel} to use this command.", CommandStatus.ERROR)
        }

        val canConsume = playerService.consumeResources(playerId, command.cpuCost, command.ramCost)
        if (canConsume.getOrNull() != true) {
            return CommandResult(commandName, "Insufficient resources. CPU needed: ${command.cpuCost}, RAM needed: ${command.ramCost}", CommandStatus.ERROR)
        }

        return processCommand(player, commandName, args)
    }

    private suspend fun processCommand(player: Player, cmd: String, args: Array<out String>): CommandResult {
        return when (cmd) {
            "help" -> handleHelp(player, args)
            "scan" -> handleScan(player, args)
            "connect" -> handleConnect(player, args)
            "whoami" -> handleWhoami(player)
            "status" -> handleStatus(player)
            "ls" -> handleLs(player, args)
            "cat" -> handleCat(player, args)
            "ifconfig" -> handleIfconfig(player)
            "ping" -> handlePing(player, args)
            "nmap" -> handleNmap(player, args)
            "traceroute" -> handleTraceroute(player, args)
            "whois" -> handleWhois(player, args)
            "ssh" -> handleSsh(player, args)
            "ftp" -> handleFtp(player, args)
            "exploit" -> handleExploit(player, args)
            "bruteforce" -> handleBruteforce(player, args)
            "decrypt" -> handleDecrypt(player, args)
            "sqlmap" -> handleSqlmap(player, args)
            "backdoor" -> handleBackdoor(player, args)
            "sniff" -> handleSniff(player, args)
            "spoof" -> handleSpoof(player, args)
            "proxy" -> handleProxy(player, args)
            "encrypt" -> handleEncrypt(player, args)
            "worm" -> handleWorm(player, args)
            "firewall" -> handleFirewall(player, args)
            "trace" -> handleTrace(player, args)
            "honeypot" -> handleHoneypot(player, args)
            "overload" -> handleOverload(player, args)
            "crack" -> handleCrack(player, args)
            "rootkit" -> handleRootkit(player, args)
            "botnet" -> handleBotnet(player, args)
            "dnshijack" -> handleDnshijack(player, args)
            "zero-day" -> handleZeroday(player, args)
            "ai-assist" -> handleAiAssist(player, args)
            else -> CommandResult(cmd, "Unknown command: $cmd", CommandStatus.ERROR)
        }
    }

    private suspend fun handleHelp(player: Player, args: Array<out String>): CommandResult {
        if (args.isNotEmpty() && args[0] in player.unlockedCommands) {
            val cmd = CommandRegistry.commandMap[args[0]]!!
            return CommandResult("help", """
${cmd.name}: ${cmd.description}
  Syntax: ${cmd.syntax}
  Category: ${cmd.category.displayName}
  Level Required: ${cmd.minLevel}
  Cost: ${cmd.cpuCost} CPU, ${cmd.ramCost} RAM
  Cooldown: ${cmd.cooldownMs}ms
            """.trimIndent(), CommandStatus.SUCCESS)
        }

        val categorized = player.unlockedCommands
            .mapNotNull { CommandRegistry.commandMap[it] }
            .groupBy { it.category.displayName }

        val output = categorizeOutput(categorized, player)
        return CommandResult("help", output, CommandStatus.SUCCESS)
    }

    private fun categorizeOutput(categorized: Map<String, List<GameCommand>>, player: Player): String {
        val sb = StringBuilder("╔══════════════════════════════════════╗\n")
        sb.appendLine("║         ZERODAY TERMINAL v1.0        ║")
        sb.appendLine("╚══════════════════════════════════════╝")
        sb.appendLine("Available Commands (${player.unlockedCommands.size}/${CommandRegistry.allCommands.size}):")
        sb.appendLine()
        categorized.forEach { (category, cmds) ->
            sb.appendLine(" [$category]")
            cmds.forEach { cmd ->
                sb.appendLine("  ├─ ${cmd.name.padEnd(15)} ${cmd.description}")
            }
            sb.appendLine()
        }
        sb.append("\nType 'help <command>' for details on a specific command.")
        return sb.toString()
    }

    private suspend fun handleScan(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { "localhost" }
        return when {
            target == "localhost" || target == "127.0.0.1" -> {
                CommandResult("scan", """
Scanning localhost (127.0.0.1)...
┌─────────────────────────────────────┐
│ Hostname: localhost                 │
│ OS: ZeroDay Linux 5.x              │
│ Uptime: 42 days                    │
├─────────────────────────────────────┤
│ PORT     STATE      SERVICE        │
│ 22/tcp   open       ssh            │
│ 80/tcp   open       http           │
│ 443/tcp  open       https          │
│ 8080/tcp filtered   http-proxy     │
└─────────────────────────────────────┘
                """.trimIndent(), CommandStatus.SUCCESS)
            }
            target == "darknet" -> {
                CommandResult("scan", """
Scanning darknet...
┌──────────────────────────────────────────────┐
│ Available Tasks:                             │
│  t1  [EASY]    Scan Corporate Network        │
│  t2  [MEDIUM]  Extract Customer Database     │
│  t3  [HARD]    Deploy Ransomware Simulation  │
│  t4  [HARD]    Botnet Recruitment Drive      │
│  t5  [EXPERT]  Crack Government Cipher       │
│  t6  [LEGEND]  Zero-Day Auction Hijack       │
│  t7  [MEDIUM]  Defend Against APT            │
│  t8  [HARD]    Darknet Forum Heist           │
│  t9  [EXPERT]  Fireside Chat Intercept       │
│  t10 [EXPERT]  Worm Containment              │
│  t11 [TRIVIAL] Simple Data Grab              │
│  t12 [EASY]    Brute Force Challenge         │
│  t13 [MEDIUM]  Phishing Campaign             │
│  t14 [HARD]    DDoS Coordination             │
│  t15 [MEDIUM]  Bug Bounty: E-commerce        │
└──────────────────────────────────────────────┘
Type 'accept <task_id>' to take a contract.
                """.trimIndent(), CommandStatus.SUCCESS)
            }
            player.discoveredNodes.contains(target) -> {
                CommandResult("scan", "Target $target is in your known network. Use 'nmap $target' for detailed scan.", CommandStatus.SUCCESS)
            }
            else -> {
                CommandResult("scan", "Scanning $target...\nNo hosts found. You may not have access to this network segment.", CommandStatus.SUCCESS)
            }
        }
    }

    private suspend fun handleConnect(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("connect", "Usage: connect <ip>", CommandStatus.ERROR) }
        if (player.discoveredNodes.contains(target)) {
            playerService.discoverNode(player.id, target)
            return CommandResult("connect", """
Connected to $target
Welcome to ${getHostname(target)}.
Type 'help' for available commands on this system.
            """.trimIndent(), CommandStatus.SUCCESS)
        }
        return CommandResult("connect", "Connection failed: $target is unreachable. Scan the network first.", CommandStatus.ERROR)
    }

    private suspend fun handleWhoami(player: Player): CommandResult {
        return CommandResult("whoami", """
User: ${player.username}
ID: ${player.id}
Level: ${player.level}
Reputation: ${player.reputation}
Last Login: ${player.lastLoginIp}
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleStatus(player: Player): CommandResult {
        return CommandResult("status", """
╔══════════════════════════════════════╗
║         SYSTEM STATUS               ║
╠══════════════════════════════════════╣
║ Player: ${player.username.padEnd(20)} ║
║ Level: ${player.level.toString().padEnd(21)} ║
║ XP: ${"${player.experience}/${player.experienceToNext}".padEnd(19)} ║
╠══════════════════════════════════════╣
║ Resources:                          ║
║  CPU: ${"${player.cpu}/${player.maxCpu} MHz".padEnd(20)} ║
║  RAM: ${"${player.ram}/${player.maxRam} MB".padEnd(20)} ║
║  BW:  ${"${player.bandwidth}/${player.maxBandwidth} Mbps".padEnd(20)} ║
║  Credits: ${player.credits.toString().padEnd(16)} ║
║  Reputation: ${player.reputation.toString().padEnd(13)} ║
╠══════════════════════════════════════╣
║ Storyline: ${(player.currentStoryline ?: "None").padEnd(16)} ║
║ Nodes Discovered: ${player.discoveredNodes.size.toString().padEnd(9)} ║
║ Commands Unlocked: ${player.unlockedCommands.size.toString().padEnd(10)} ║
║ Active Tasks: ${player.activeTasks.size.toString().padEnd(14)} ║
╚══════════════════════════════════════╝
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleLs(player: Player, args: Array<out String>): CommandResult {
        val path = args.getOrElse(0) { "." }
        return CommandResult("ls", """
Directory listing for $path:
drwxr-xr-x  2 root root 4096 Jan 01 00:00 .
drwxr-xr-x  2 root root 4096 Jan 01 00:00 ..
-rw-r--r--  1 root root  256 Jan 01 00:00 notes.txt
-rw-r--r--  1 root root 1024 Jan 01 00:00 config.json
-rw-r--r--  1 root root   64 Jan 01 00:00 .bash_history
drwxr-xr-x  2 root root 4096 Jan 01 00:00 data
drwxr-xr-x  2 root root 4096 Jan 01 00:00 logs
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleCat(player: Player, args: Array<out String>): CommandResult {
        val file = args.getOrElse(0) { return CommandResult("cat", "Usage: cat <file>", CommandStatus.ERROR) }
        val contents = fileContents[file] ?: """
[File: $file]
ACCESS DENIED: Insufficient permissions or file not found.
        """.trimIndent()
        return CommandResult("cat", contents, CommandStatus.SUCCESS)
    }

    private suspend fun handleIfconfig(player: Player): CommandResult {
        return CommandResult("ifconfig", """
eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
    inet 127.0.0.1  netmask 255.0.0.0  broadcast 127.255.255.255
    inet6 ::1  prefixlen 128  scopeid 0x10<host>
    ether 00:1a:2b:3c:4d:5e  txqueuelen 1000  (Ethernet)
    RX packets 14253  bytes 15420341 (14.7 MB)
    TX packets 8921  bytes 892143 (871.2 KB)

wlan0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
    inet 10.0.0.42  netmask 255.255.255.0  broadcast 10.0.0.255
    inet6 fe80::214:51ff:fe2a:3b1c  prefixlen 64  scopeid 0x20<link>
    ether 00:14:51:2a:3b:1c  txqueuelen 1000  (Ethernet)
    RX packets 38921  bytes 42153921 (40.2 MB)
    TX packets 45212  bytes 5214321 (4.97 MB)

Network ranges accessible:
  - 10.0.0.0/24 (Local subnet)
  - 10.0.1.0/24 (Corporate network)
  - 172.16.0.0/16 (Darknet)
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handlePing(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("ping", "Usage: ping <ip>", CommandStatus.ERROR) }
        if (player.discoveredNodes.contains(target)) {
            return CommandResult("ping", """
PING $target ($target) 56(84) bytes of data.
64 bytes from $target: icmp_seq=1 ttl=64 time=12.3 ms
64 bytes from $target: icmp_seq=2 ttl=64 time=11.8 ms
64 bytes from $target: icmp_seq=3 ttl=64 time=13.1 ms
64 bytes from $target: icmp_seq=4 ttl=64 time=11.5 ms

--- $target ping statistics ---
4 packets transmitted, 4 received, 0% packet loss, time 3004ms
rtt min/avg/max/mdev = 11.5/12.175/13.1/0.622 ms
            """.trimIndent(), CommandStatus.SUCCESS)
        }
        return CommandResult("ping", "ping: $target: Temporary failure in name resolution", CommandStatus.ERROR)
    }

    private suspend fun handleNmap(player: Player, args: Array<out String>): CommandResult {
        val target = args.find { !it.startsWith("-") } ?: return CommandResult("nmap", "Usage: nmap [-sS|-sV] <target>", CommandStatus.ERROR)
        val body = if (player.discoveredNodes.contains(target)) {
            """
PORT     STATE    SERVICE      VERSION
22/tcp   open     ssh          OpenSSH 8.9p1
80/tcp   open     http         Apache httpd 2.4.57
443/tcp  open     https        nginx 1.24.0
3306/tcp filtered mysql
8080/tcp open     http-proxy   Squid proxy 6.0

Host script results:
|_nbstat: NetBIOS name: ${getHostname(target)}, NetBIOS user: <unknown>
| smb-os-discovery:
|   OS: Linux 5.15
|   Computer name: ${getHostname(target)}
|   Domain name: zeroday.local

Nmap done: 1 IP address (1 host up) scanned in 3.42s
"""
        } else {
            """
PORT     STATE    SERVICE
All 1000 scanned ports on $target are filtered

Nmap done: 1 IP address (1 host up) scanned in 15.23s
Note: Host seems to be behind a firewall. Try a different subnet.
"""
        }
        return CommandResult("nmap", """
Starting Nmap 7.94 ( https://nmap.org )
Scanning $target
$body
""".trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleTraceroute(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("traceroute", "Usage: traceroute <ip>", CommandStatus.ERROR) }
        return CommandResult("traceroute", """
traceroute to $target, 30 hops max, 60 byte packets
 1  10.0.0.1 (10.0.0.1)  0.512 ms  0.498 ms  0.512 ms
 2  10.0.1.1 (10.0.1.1)  1.234 ms  1.198 ms  1.211 ms
 3  172.16.0.1 (172.16.0.1)  3.456 ms  3.421 ms  3.389 ms
 4  $target ($target)  5.678 ms  5.621 ms  5.599 ms
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleWhois(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("whois", "Usage: whois <ip>", CommandStatus.ERROR) }
        return CommandResult("whois", """
Whois lookup for $target:
┌──────────────────────────────────────────┐
│ Organization: ZeroDay Industries         │
│ Network Range: ${target.substringBeforeLast(".")}.0/24 │
│ Country: US                              │
│ Admin: admin@zeroday.local               │
│ ISP: ZeroDay Communications              │
│ Abuse: abuse@zeroday.local               │
├──────────────────────────────────────────┤
│ DNS Records:                             │
│   A: $target                              │
│   PTR: ${getHostname(target)}.zeroday.local │
│   MX: mail.zeroday.local                 │
└──────────────────────────────────────────┘
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleSsh(player: Player, args: Array<out String>): CommandResult {
        if (args.isEmpty()) return CommandResult("ssh", "Usage: ssh <user>@<ip>", CommandStatus.ERROR)
        val target = args[0].substringAfter("@")
        return CommandResult("ssh", """
SSH connection established to $target
Authenticating...
Welcome ${player.username}@${getHostname(target)}

Last login: ${java.time.Instant.now()}
[${player.username}@${getHostname(target)} ~]$
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleFtp(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("ftp", "Usage: ftp <ip>", CommandStatus.ERROR) }
        return CommandResult("ftp", """
Connected to $target.
220 ProFTPD 1.3.5 Server ready.
Name (${player.username}): anonymous
331 Anonymous login ok, send your email as password.
Password:
230 Anonymous access granted.
Remote system type is UNIX.
ftp> ls
-rw-r--r--   1 root     root         1024 Jan 01 00:00 welcome.txt
-rw-r--r--   1 root     root        16384 Jan 01 00:00 shares.zip
ftp>
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleExploit(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("exploit", "Usage: exploit <target> [--payload=<name>] [--use-zeroday]", CommandStatus.ERROR) }
        val payload = args.find { it.startsWith("--payload=") }?.substringAfter("=") ?: "reverse_shell"
        val useZeroday = args.any { it == "--use-zeroday" }

        if (useZeroday) {
            if (player.activeZeroDayExploits.isEmpty()) {
                return CommandResult("exploit", "No zero-day exploits loaded. Craft one in the Research Lab.", CommandStatus.ERROR)
            }
            val exploit = playerService.consumeZeroDayExploit(player.id).getOrNull()
            if (exploit == null) return CommandResult("exploit", "Failed to load zero-day exploit.", CommandStatus.ERROR)

            return CommandResult("exploit", """
  ███████╗██████╗  ██████╗ ██████╗ ██████╗  █████╗ ██╗   ██╗
  ╚══███╔╝██╔══██╗██╔════╝██╔═══██╗██╔══██╗██╔══██╗╚██╗ ██╔╝
    ███╔╝ ██║  ██║██║     ██║   ██║██║  ██║███████║ ╚████╔╝ 
   ███╔╝  ██║  ██║██║     ██║   ██║██║  ██║██╔══██║  ╚██╔╝  
  ███████╗██████╔╝╚██████╗╚██████╔╝██████╔╝██║  ██║   ██║   
  ╚══════╝╚═════╝  ╚═════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝   

Deploying ZERO-DAY exploit against $target...
Payload: ${exploit.name} (${exploit.rarity.displayName})

[+] Target identified: ${getHostname(target)}
[+] No security measures detected - exploit bypassed all defenses
[+] Using zero-day: ${exploit.name} (${exploit.description})
[+] Exploit power: ${getZeroDayPower(exploit)}
[+] Session opened with FULL SYSTEM CONTROL
[+] No traces left behind - system logs show normal activity

Zero-day deployed successfully. Use remaining for critical targets only.
            """.trimIndent(), CommandStatus.SUCCESS)
        }

        return CommandResult("exploit", """
Running exploit against $target...
Payload: $payload

[+] Target identified: ${getHostname(target)}
[+] OS: Linux 5.15 - x86_64
[+] Vulnerability detected: CVE-2024-${(1000..9999).random()}
[+] Exploiting...
[+] Payload delivered successfully!
[+] Session opened: ${player.username}@${getHostname(target)} (uid=0)
[+] Root access granted!

System compromised. Type commands on ${getHostname(target)} or 'exit' to return.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private fun getZeroDayPower(item: com.zeroday.model.InventoryItem): Int = when (item.rarity) {
        com.zeroday.model.ItemRarity.RARE -> 5
        com.zeroday.model.ItemRarity.EPIC -> 10
        com.zeroday.model.ItemRarity.LEGENDARY -> 99
        else -> 3
    }

    private suspend fun handleBruteforce(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("bruteforce", "Usage: bruteforce <target> [--threads=N]", CommandStatus.ERROR) }
        val threads = args.find { it.startsWith("--threads=") }?.substringAfter("=")?.toIntOrNull() ?: 4
        return CommandResult("bruteforce", """
Brute forcing credentials for $target
Using $threads threads...

[+] Trying admin:admin... Failed
[+] Trying root:toor... Failed
[+] Trying admin:password123... Failed
[+] Trying administrator:admin... Failed
[+] Trying root:root... SUCCESS!

Credentials found: root / root

Access granted to $target
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleDecrypt(player: Player, args: Array<out String>): CommandResult {
        val file = args.getOrElse(0) { return CommandResult("decrypt", "Usage: decrypt <file> [--key=<key>]", CommandStatus.ERROR) }
        val key = args.find { it.startsWith("--key=") }?.substringAfter("=") ?: "bruteforce"
        return CommandResult("decrypt", """
Decrypting $file...
Method: ${if (key == "bruteforce") "Brute force + Dictionary" else "AES-256-CBC with provided key"}

[+] Analyzing encryption...
[+] Detected: AES-256-CBC
[+] Cracking...
[${"#"}] 10%
[${"#####"}] 50%
[${"##########"}] 100%
[+] Decryption successful!

Decrypted content:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
CLASSIFIED ACCESS CODES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Server: 10.0.2.50
User: db_admin
Pass: D4t4b@s3S3cur3!

Encryption key: ${(0..15).map { "0123456789abcdef".random() }.joinToString("")}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleSqlmap(player: Player, args: Array<out String>): CommandResult {
        val url = args.getOrElse(0) { return CommandResult("sqlmap", "Usage: sqlmap <url>", CommandStatus.ERROR) }
        return CommandResult("sqlmap", """
sqlmap (1.7.12) - Automatic SQL injection tool

[*] targeting: $url
[+] GET parameter 'id' appears to be injectable
[+] Testing MySQL...
[+] Technique: UNION query SQLi
[+] Payload: id=1 UNION SELECT 1,2,3,group_concat(table_name),5 FROM information_schema.tables

[+] Extracted database tables:
  - users (42 records)
  - products (156 records)
  - orders (891 records)
  - credentials (3 records)

[+] Dumping 'credentials' table:
  1 | admin    | 5f4dcc3b5aa765d61d8327deb882cf99
  2 | operator | 482c811da5d5b4bc6d497ffa98491e38
  3 | root     | ${'$'}2y${'$'}10${'$'}N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

[+] Table dumped! Hash cracking recommended.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleBackdoor(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("backdoor", "Usage: backdoor <ip> [--persistent]", CommandStatus.ERROR) }
        val persistent = args.any { it == "--persistent" }
        return CommandResult("backdoor", """
Installing backdoor on $target...
${if (persistent) "[+] Persistent mode enabled" else "[+] Standard backdoor"}

[+] Establishing foothold...
[+] Creating hidden process: [kworker/3:1+events] (PID: ${(1000..9999).random()})
[+] Opening reverse port: ${(40000..60000).random()}
[+] Backdoor installed successfully!
[+] Persistence: ${if (persistent) "System reboot resistant (cron job + init.d script)" else "Non-persistent (will be removed on reboot)"}

Use 'connect $target' to access the backdoor shell.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleSniff(player: Player, args: Array<out String>): CommandResult {
        val iface = args.getOrElse(0) { "eth0" }
        return CommandResult("sniff", """
Packet capture on $iface...
Capturing 20 packets...

┌────────────────────────────────────────────────────────────────┐
│  #  TIME        SOURCE           DESTINATION      PROTO  SIZE │
├────────────────────────────────────────────────────────────────┤
│  1  0.000123    10.0.0.42        10.0.0.1          TCP     64 │
│  2  0.000456    10.0.0.1         10.0.0.42         TCP     64 │
│  3  0.001234    10.0.0.42        203.0.113.1       TCP   1280 │ │
│  4  0.001567    10.0.0.42        10.0.1.5          SSH    256 │
│  5  0.002345    10.0.1.5         10.0.0.42         SSH    256 │
│  6  0.003456    10.0.0.42        10.0.1.200        HTTP   512 │
│  7  0.004567    10.0.1.200       10.0.0.42         HTTP   512 │
│  8  0.005678    10.0.0.42        172.16.0.1        TLS   1024 │
│  9  0.006789    172.16.0.1       10.0.0.42         TLS   1024 │
│ 10  0.007890    10.0.0.42        10.0.2.10         SMB    316 │
└────────────────────────────────────────────────────────────────┘

[!] Sensitive data detected in packet #6: POST credentials to /login
[!] Session cookie captured from packet #8
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleSpoof(player: Player, args: Array<out String>): CommandResult {
        val ip = args.getOrElse(0) { return CommandResult("spoof", "Usage: spoof <ip> [--mac=<addr>]", CommandStatus.ERROR) }
        val mac = args.find { it.startsWith("--mac=") }?.substringAfter("=") ?: "00:11:22:33:44:55"
        return CommandResult("spoof", """
Spoofing identity as $ip...
MAC: $mac

[+] ARP cache poisoned on local network
[+] IP identity spoofed: ${player.username}@$ip
[+] MAC address changed: $mac
[+] All outgoing traffic will now appear to originate from $ip

Use 'status' to verify your spoofed identity.
Use 'proxy <ip>:<port>' to chain through a proxy for additional anonymity.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleProxy(player: Player, args: Array<out String>): CommandResult {
        val proxyAddr = args.getOrElse(0) { return CommandResult("proxy", "Usage: proxy <ip>:<port>", CommandStatus.ERROR) }
        return CommandResult("proxy", """
Configuring proxy chain...
Proxy: $proxyAddr

[+] SOCKS5 proxy configured
[+] Traffic routing: ${player.username} -> $proxyAddr -> internet
[+] Anonymity level: Medium (single hop)

Available proxy chains:
  1. $proxyAddr (current)
  2. Tor network (requires darknet access)
  3. VPN tunnel (requires level 12+)

Use 'proxy --chain <n>' to switch chains.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleEncrypt(player: Player, args: Array<out String>): CommandResult {
        val data = args.getOrElse(0) { return CommandResult("encrypt", "Usage: encrypt <data> [--method=<alg>]", CommandStatus.ERROR) }
        val method = args.find { it.startsWith("--method=") }?.substringAfter("=") ?: "aes-256"
        return CommandResult("encrypt", """
Encrypting data using $method...
Data: '$data'

[+] Generating key...
[+] Encryption complete!

Encrypted output: ${(0..63).map { "0123456789abcdef".random() }.joinToString("")}
Method: $method
Key: ${(0..31).map { "0123456789abcdef".random() }.joinToString("")} (store securely)
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleWorm(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("worm", "Usage: worm <target> [--spread]", CommandStatus.ERROR) }
        val spread = args.any { it == "--spread" }
        return CommandResult("worm", """
Deploying worm to $target...
[+] Payload: zeroday_worm_v1.0
${if (spread) "[+] Auto-spread mode ENABLED" else "[+] Targeted deployment"}

[+] Infecting $target...
[+] Worm installed on ${getHostname(target)}
[+] Payload: keylogger, backdoor, cryptominer

${if (spread) """
[+] Spreading to adjacent nodes...
[+] 10.0.0.5 - INFECTED
[+] 10.0.0.3 - INFECTED
[+] 10.0.1.5 - INFECTED
[+] 10.0.1.10 - INFECTED
[!] Worm propagation detected! Firewalls may alert.
""" else ""}
Worm active. Use 'botnet status' to check infected hosts.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleFirewall(player: Player, args: Array<out String>): CommandResult {
        val allow = args.find { it.startsWith("--allow=") }?.substringAfter("=")
        val deny = args.find { it.startsWith("--deny=") }?.substringAfter("=")
        return CommandResult("firewall", """
Configuring firewall...

${if (allow != null) "[+] ALLOW rule added: $allow" else ""}
${if (deny != null) "[+] DENY rule added: $deny" else ""}
${if (allow == null && deny == null) "[+] No rules specified. Showing current rules." else ""}

Current firewall rules:
┌──────┬──────────────────┬──────────┬──────────┐
│  #   │  SOURCE          │  PORT    │  ACTION  │
├──────┼──────────────────┼──────────┼──────────┤
│  1   │  0.0.0.0/0       │  22      │  ALLOW   │
│  2   │  0.0.0.0/0       │  80      │  ALLOW   │
│  3   │  0.0.0.0/0       │  443     │  ALLOW   │
│  4   │  127.0.0.0/8     │  *       │  ALLOW   │
${if (deny != null) "│  5   │  ${deny.padEnd(16)}│  *       │  DENY    │" else ""}
└──────┴──────────────────┴──────────┴──────────┘
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleTrace(player: Player, args: Array<out String>): CommandResult {
        val session = args.getOrElse(0) { "all" }
        return CommandResult("trace", """
Tracing ${if (session == "all") "all active sessions" else "session $session"}...

[+] Scanning for intrusion attempts...
[+] ${(1..5).random()} suspicious connections found

┌──────────────────────────────────────────────────────────┐
│  SESSION  SOURCE IP       COUNTRY   STATUS               │
├──────────────────────────────────────────────────────────┤
│  0x${(0..3).map { "0123456789abcdef".random() }.joinToString("")}    ${(1..4).map { (10..255).random() }.joinToString(".")}    RU      Active - data exfiltration │
│  0x${(0..3).map { "0123456789abcdef".random() }.joinToString("")}    ${(1..4).map { (10..255).random() }.joinToString(".")}    CN      Active - port scanning     │
│  0x${(0..3).map { "0123456789abcdef".random() }.joinToString("")}    ${(1..4).map { (10..255).random() }.joinToString(".")}    US      Blocked - firewall rule    │
└──────────────────────────────────────────────────────────┘

[!] Trace route to 185.${(10..99).random()}.${(1..254).random()}.${(1..254).random()}
[!] Attacker likely using VPN/proxy chain
    """
            .trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleHoneypot(player: Player, args: Array<out String>): CommandResult {
        val port = args.find { it.startsWith("--port=") }?.substringAfter("=") ?: "8080"
        return CommandResult("honeypot", """
Deploying honeypot on port $port...

[+] Honeypot type: SSH + HTTP (emulated services)
[+] Listening on 0.0.0.0:$port
[+] Fake files planted: admin_credentials.txt, database_backup.sql
[+] Alerting configured: notification on compromise attempt

Honeypot active. Captured interactions will appear in logs.
Use 'cat logs/honeypot.log' to review captures.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleOverload(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("overload", "Usage: overload <ip> [--duration=N]", CommandStatus.ERROR) }
        val duration = args.find { it.startsWith("--duration=") }?.substringAfter("=")?.toIntOrNull() ?: 30
        return CommandResult("overload", """
Launching DDoS attack on $target...
Duration: ${duration}s
[+] Botnet nodes engaged: ${(5..50).random()}
[+] Attack vector: UDP flood + SYN flood + HTTP flood

Bandwidth usage: ${(50..500).random()} Mbps
Packets sent: ${(10000..100000).random()}

[!] Target $target is experiencing service degradation
[!] Estimated downtime: ${duration + (10..30).random()} seconds

Attack in progress... Type 'status' to monitor.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleCrack(player: Player, args: Array<out String>): CommandResult {
        val hash = args.getOrElse(0) { return CommandResult("crack", "Usage: crack <hash> [--wordlist=<file>]", CommandStatus.ERROR) }
        val wordlist = args.find { it.startsWith("--wordlist=") }?.substringAfter("=") ?: "rockyou.txt"
        return CommandResult("crack", """
Cracking hash: ${hash.take(32)}...
Wordlist: $wordlist

[+] Hash type detected: SHA-256
[+] Starting crack...
[+] Trying passwords from dictionary...

[${"#"}] 1% - Trying 'password'...
[${"#####"}] 12% - Trying '123456'...
[${"########"}] 25% - Trying 'qwerty'...
[${"###########"}] 35% - Trying 'letmein'...
[${"###############"}] 50% - Trying 'admin'...
[${"##################"}] 65% - Trying 'welcome'...
[${"######################"}] 80% - Trying 'monkey'...
[${"########################"}] 95% - Trying 'dragon'...

[+] CRACKED! Password: P@ssw0rd!${(10..99).random()}

Time elapsed: ${(2..30).random()}s
Attempts: ${(10000..1000000).random()}
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleRootkit(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("rootkit", "Usage: rootkit <ip> [--stealth]", CommandStatus.ERROR) }
        val stealth = args.any { it == "--stealth" }
        return CommandResult("rootkit", """
Installing rootkit on $target...
${if (stealth) "[+] Stealth mode: kernel module will hide from detection" else "[+] Standard installation"}

[+] Kernel module loaded: hideme.ko
[+] Files hidden: /var/log/auth.log entries cleaned
[+] Process hidden: /usr/sbin/sshd - modified binary
[+] Backdoor port: ${(30000..65000).random()} (UDP)
[+] Rootkit installed ${if (stealth) "with zero detections" else "(may be detected by rkhunter)"}

System fully compromised. You have persistent root access.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleBotnet(player: Player, args: Array<out String>): CommandResult {
        val subcmd = args.getOrElse(0) { "status" }
        return when (subcmd) {
            "status" -> CommandResult("botnet", """
Botnet Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Active nodes: ${(3..50).random()}
Total bandwidth: ${(100..1000).random()} Mbps
Total CPU: ${(500..5000).random()} MHz
Total RAM: ${(1024..16384).random()} MB

Node distribution:
  ├─ Servers: ${(1..10).random()}
  ├─ Workstations: ${(1..20).random()}
  ├─ IoT devices: ${(1..15).random()}
  └─ Mobile: ${(0..5).random()}

Commands: botnet scan, botnet attack <target>, botnet update
            """.trimIndent(), CommandStatus.SUCCESS)
            "scan" -> CommandResult("botnet", "Scanning for vulnerable hosts to add to botnet... Found ${(2..10).random()} new targets.", CommandStatus.SUCCESS)
            "attack" -> {
                val target = args.getOrElse(1) { "unknown" }
                CommandResult("botnet", "Botnet attack initiated on $target. ${(10..100).random()} nodes participating.", CommandStatus.SUCCESS)
            }
            "update" -> CommandResult("botnet", "Updating botnet payload across all nodes... ${(3..50).random()} nodes updated successfully.", CommandStatus.SUCCESS)
            else -> CommandResult("botnet", "Unknown botnet command: $subcmd", CommandStatus.ERROR)
        }
    }

    private suspend fun handleDnshijack(player: Player, args: Array<out String>): CommandResult {
        val domain = args.getOrElse(0) { return CommandResult("dnshijack", "Usage: dnshijack <domain> --redirect=<ip>", CommandStatus.ERROR) }
        val redirect = args.find { it.startsWith("--redirect=") }?.substringAfter("=") ?: return CommandResult("dnshijack", "Usage: dnshijack <domain> --redirect=<ip>", CommandStatus.ERROR)
        return CommandResult("dnshijack", """
DNS Hijack initiated on $domain...
Redirecting to $redirect

[+] DNS cache poisoned for $domain
[+] TTL set to 0 (immediate propagation)
[+] All queries for $domain will resolve to $redirect
[+] SSL certificates will show warning (users may ignore)

Hijack active. Traffic from $domain is now routed through your proxy.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleZeroday(player: Player, args: Array<out String>): CommandResult {
        val target = args.getOrElse(0) { return CommandResult("zero-day", "Usage: zero-day <target> [--cve=<id>]", CommandStatus.ERROR) }
        val cve = args.find { it.startsWith("--cve=") }?.substringAfter("=") ?: "CVE-2024-${(1000..9999).random()}"
        return CommandResult("zero-day", """
Deploying zero-day exploit against $target...
CVE: $cve
Type: Remote Code Execution
Severity: CRITICAL (10.0)

[+] Vulnerability confirmed: $cve
[+] No known signature - bypassing all AV/IDS
[+] Shellcode injected...
[+] SYSTEM privilege escalation successful!

  ███████╗██████╗  ██████╗ ██████╗ ██████╗  █████╗ ██╗   ██╗
  ╚══███╔╝██╔══██╗██╔════╝██╔═══██╗██╔══██╗██╔══██╗╚██╗ ██╔╝
    ███╔╝ ██║  ██║██║     ██║   ██║██║  ██║███████║ ╚████╔╝ 
   ███╔╝  ██║  ██║██║     ██║   ██║██║  ██║██╔══██║  ╚██╔╝  
  ███████╗██████╔╝╚██████╗╚██████╔╝██████╔╝██║  ██║   ██║   
  ╚══════╝╚═════╝  ╚═════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝   

Zero-day successfully deployed. Full system control achieved.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private suspend fun handleAiAssist(player: Player, args: Array<out String>): CommandResult {
        if (args.isEmpty()) return CommandResult("ai-assist", "Usage: ai-assist <query>", CommandStatus.ERROR)
        val query = args.joinToString(" ")
        return CommandResult("ai-assist", """
AI Assist (ZeroDay Neural Engine v3.0)
Query: $query

[+] Processing...
[+] Analyzing network topology...
[+] Cross-referencing vulnerability databases...

Analysis Results:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• Recommended targets: 10.0.1.200 (webapp - SQLi)
• Vulnerable services: Apache 2.4.49 (path traversal)
• Suggested exploit: exploit 10.0.1.200 --payload=shell_exec
• Estimated success rate: 87.3%
• Risk level: MEDIUM
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

AI processing complete. ${(100..500).random()} CPU cycles consumed.
        """.trimIndent(), CommandStatus.SUCCESS)
    }

    private fun getHostname(ip: String): String {
        val hostnames = mapOf(
            "0.0.0.0/0" to "global",
            "127.0.0.1" to "localhost",
            "10.0.0.1" to "gateway",
            "10.0.0.2" to "fileserver",
            "10.0.0.3" to "mailserver",
            "10.0.0.5" to "printer",
            "10.0.0.42" to "client-42",
            "10.0.1.1" to "corp-router",
            "10.0.1.5" to "vpn-server",
            "10.0.1.10" to "dev-db",
            "10.0.1.200" to "webapp",
            "10.0.2.10" to "internal-fs",
            "10.0.2.50" to "hr-database",
            "172.16.0.1" to "darknet-gateway",
            "172.16.0.50" to "marketplace",
            "172.16.0.100" to "forum",
            "203.0.113.1" to "backbone-router",
            "203.0.113.50" to "gov-server",
            "198.51.100.1" to "isp-router",
            "10.0.3.1" to "cloud-gateway",
            "10.0.3.100" to "cloud-server",
            "10.0.3.200" to "ecommerce",
            "10.0.4.1" to "mil-router",
            "10.0.4.20" to "mil-server",
            "10.0.5.1" to "sat-uplink",
            "10.0.5.100" to "corp-hq"
        )
        return hostnames[ip] ?: "host-${ip.replace('.', '-')}"
    }

    companion object {
        val fileContents = mapOf(
            "notes.txt" to """
=== Personal Notes ===
TODO:
- Patch SSH vulnerability on gateway
- Update firewall rules
- Check darknet for new contracts
Credentials for backup server: admin / b4ckup!23
            """.trimIndent(),
            "config.json" to """
{
  "server": "10.0.1.5",
  "port": 8080,
  "debug": false,
  "admin_users": ["root", "admin"],
  "features": {
    "remote_access": true,
    "auto_update": false
  }
}
            """.trimIndent(),
            "credentials.xlsx" to """
[Extracted from credentials.xlsx]
USER          | PASSWORD          | ACCESS LEVEL
admin         | P@ssw0rd!42       | ADMIN
john.smith    | Welcome1!         | USER
jane.doe      | Summer2024!       | USER
root          | R00tP@ss!         | ROOT
            """.trimIndent(),
            "network_map.pdf" to """
Network Topology:
  Internet <-> ISP Router (198.51.100.1)
    -> Corporate Gateway (10.0.1.1)
      -> Web Server (10.0.1.200)
      -> Database (10.0.1.10)
      -> VPN Server (10.0.1.5)
    -> Darknet Gateway (172.16.0.1)
      -> Marketplace (172.16.0.50)
      -> Forum (172.16.0.100)
    -> Military Network (10.0.4.0/24)
      -> Command Server (10.0.4.20)
    -> Satellite Uplink (10.0.5.1)
            """.trimIndent(),
            "flag.txt" to """
Congratulations!
Flag: ZERODAY{you_found_the_flag}
            """.trimIndent(),
            "access_codes.txt" to """
ACCESS CODES
============
Primary Server: 10.0.2.10
  User: operator
  Pass: 0p3r4t0r!@#

Database: 10.0.2.50
  User: db_admin
  Pass: D4t4b@s3S3cur3!
            """.trimIndent()
        )
    }
}

data class CommandResult(
    val command: String,
    val output: String,
    val status: CommandStatus
)

enum class CommandStatus {
    SUCCESS,
    ERROR,
    PENDING
}
