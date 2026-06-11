# Hacking Terminal System - ZeroDayMMO

## Overview

The Hacking Terminal System provides a realistic command-line hacking simulation that integrates with the IPv4 world system. Players use actual hacking commands to scan networks, exploit vulnerabilities, claim territory, and fight threats.

## Core Components

### 1. HackingTerminalSystem
The main terminal interface that processes commands and manages hacking sessions.

**Features:**
- 25+ realistic hacking commands
- Skill-based progression (levels 1-10+)
- Resource management (bandwidth, processing, crypto)
- Session management for remote connections
- Integration with all world systems

### 2. ExploitDatabase
Contains 18 different exploits across 8 categories:
- Buffer Overflow (2 exploits)
- SQL Injection (2 exploits)
- Remote Code Execution (2 exploits)
- Authentication Bypass (3 exploits)
- Privilege Escalation (2 exploits)
- Network Attacks (2 exploits)
- Malware (2 exploits)
- Zero-Day (2 exploits)

### 3. NetworkScanner
Performs reconnaissance on IPv4 addresses:
- Port scanning (nmap-style)
- Service detection
- OS fingerprinting
- Vulnerability assessment
- Subnet scanning

## Command Reference

### Reconnaissance Commands

#### `nmap <ip> [-p] [-sV] [-O]`
**Level:** 1  
**Description:** Network scanner - discover hosts and services  
**Example:**
```bash
$ nmap 192.168.1.100 -p -sV -O

Starting Nmap scan against 192.168.1.100...

Nmap scan report for 192.168.1.100
Host is up (0.042s latency)

PORT     STATE  SERVICE
22/tcp   open   ssh (OpenSSH 8.2p1)
80/tcp   open   http (Apache httpd 2.4.41)
443/tcp  open   https (nginx 1.18.0)

OS details: Linux 4.15 - 5.6

Scan completed in 3.7s
```

#### `whois <ip>`
**Level:** 1  
**Description:** Query IP ownership information  
**Example:**
```bash
$ whois 10.0.0.1

WHOIS query for 10.0.0.1
========================

IP Address: 10.0.0.1
Biome: CoreNetwork
Organization: Anonymous
Owner: hacker_001
Claimed: 2026-06-08 15:30:00

Structures: 2
```

#### `ping <ip>`
**Level:** 1  
**Description:** Test connectivity to target  
**Example:**
```bash
$ ping 192.168.1.1

PING 192.168.1.1 56 data bytes
64 bytes from 192.168.1.1: icmp_seq=0 ttl=64 time=42 ms
64 bytes from 192.168.1.1: icmp_seq=1 ttl=64 time=38 ms
64 bytes from 192.168.1.1: icmp_seq=2 ttl=64 time=45 ms
64 bytes from 192.168.1.1: icmp_seq=3 ttl=64 time=41 ms

--- 192.168.1.1 ping statistics ---
4 packets transmitted, 4 received, 0% packet loss
```

### Connection Commands

#### `ssh <user>@<ip> [-p port]`
**Level:** 2  
**Description:** Connect to remote host via SSH  
**Example:**
```bash
$ ssh root@192.168.1.100

Connecting to 192.168.1.100 as root...
Connected to 192.168.1.100
root@192.168.1.100:~$
```

#### `telnet <ip> [port]`
**Level:** 1  
**Description:** Connect to remote host via Telnet

#### `connect <ip> <port>`
**Level:** 2  
**Description:** Establish raw connection to target

### Exploitation Commands

#### `exploit <exploit_id> [target]`
**Level:** 3  
**Description:** Run exploit against target  
**Example:**
```bash
$ exploit sqli_001 192.168.1.100

[*] Loading exploit: Basic SQL Injection
[*] Target: 192.168.1.100
[*] Checking vulnerabilities...
[+] Exploit successful!
[+] Gained access to 192.168.1.100
[+] Gained data: 50
```

#### `searchsploit <keyword>`
**Level:** 2  
**Description:** Search exploit database  
**Example:**
```bash
$ searchsploit sql

Search results for: sql

ID         Name                           Level    Type
----------------------------------------------------------------------
sqli_001   Basic SQL Injection            2        SQLInjection
sqli_002   Blind SQL Injection            4        SQLInjection
```

#### `msfconsole`
**Level:** 5  
**Description:** Launch Metasploit Framework

#### `sqlmap -u <url> [--dbs]`
**Level:** 4  
**Description:** SQL injection tool

### Post-Exploitation Commands

#### `shell`
**Level:** 3  
**Description:** Spawn interactive shell

#### `upload <local_file> <remote_path>`
**Level:** 3  
**Description:** Upload file to target

#### `download <remote_file> <local_path>`
**Level:** 3  
**Description:** Download file from target

#### `persist [method]`
**Level:** 5  
**Description:** Install persistence mechanism

### Network Attack Commands

#### `dos <ip> [duration]`
**Level:** 4  
**Description:** Denial of Service attack

#### `arp_spoof <target_ip> <gateway_ip>`
**Level:** 4  
**Description:** ARP spoofing attack

#### `mitm <target1> <target2>`
**Level:** 5  
**Description:** Man-in-the-middle attack

### Cryptography Commands

#### `hashcat <hash> [wordlist]`
**Level:** 3  
**Description:** Password cracking tool

#### `john <file>`
**Level:** 3  
**Description:** John the Ripper password cracker

#### `openssl <command> [args]`
**Level:** 2  
**Description:** Encryption/decryption tool

### System Commands

#### `help [command]`
**Level:** 1  
**Description:** Show available commands

#### `status`
**Level:** 1  
**Description:** Show current session status  
**Example:**
```bash
$ status

=== SESSION STATUS ===

Active Session: abc123-def456
Target: 192.168.1.100
User: root
Connected: 15:30:00
Duration: 5.2 minutes

Hacking Level: 3
XP: 45/100
```

#### `resources`
**Level:** 1  
**Description:** Show available resources  
**Example:**
```bash
$ resources

=== RESOURCES ===

bandwidth       1000
processing       500
crypto           100
```

#### `level`
**Level:** 1  
**Description:** Show hacking level and XP  
**Example:**
```bash
$ level

=== HACKING LEVEL ===

Level: 3
XP: 45/100
Progress: 45.0%

Unlocked Commands: 18/25
```

#### `disconnect`
**Level:** 1  
**Description:** Close current session

#### `clear`
**Level:** 1  
**Description:** Clear terminal screen

## Gameplay Mechanics

### Skill Progression

Players level up by:
- Executing commands (+5 XP)
- Successful scans (+10 XP)
- Successful exploits (+50-500 XP based on difficulty)
- Claiming territory (+100 XP)
- Defeating threats (+20-200 XP based on threat level)

**Level Requirements:**
- Level 1: Basic recon (nmap, ping, whois)
- Level 2: Connections and basic exploits
- Level 3: Post-exploitation and crypto
- Level 4: Network attacks
- Level 5: Advanced techniques (MITM, Metasploit)
- Level 7+: Kernel exploits and zero-days
- Level 10: Ultimate zero-day exploits

### Resource System

**Bandwidth:** Used for network operations
- Scanning: -10 per scan
- Connections: -5 per connection
- DoS attacks: -50 per attack

**Processing:** Used for computation
- Exploits: -20 to -100 per exploit
- Password cracking: -30 per attempt
- Malware deployment: -50 per deployment

**Crypto:** Currency for advanced operations
- Zero-day exploits: -100 to -500
- Purchasing tools: varies
- Trading with factions: varies

### Exploit Success Rates

Success depends on:
1. **Base Success Rate:** Each exploit has a base chance (40-95%)
2. **Target Defenses:** Firewalls reduce success by 50%
3. **Player Level:** +5% per level above minimum
4. **Random Factor:** Slight randomness for unpredictability

**Example Calculation:**
```
Exploit: SQL Injection (70% base)
Target: Has firewall (70% * 0.5 = 35%)
Player: Level 4, exploit requires level 2 (+10%)
Final chance: 35% + 10% = 45%
```

### Integration with World Systems

#### Claiming Territory
```bash
# Scan for unowned IPs
$ nmap 10.0.0.0/24

# Exploit vulnerable host
$ exploit rce_001 10.0.0.50
[+] Exploit successful!
[+] Claimed tile 10.0.0.50 for your faction
```

#### Fighting Threats
```bash
# Scan for threats
$ nmap 192.168.1.100
[!] Active threats detected: 2

# Connect and fight
$ ssh admin@192.168.1.100
$ exploit mal_001
[+] Defeated threat: Basic Virus
[+] Rewards: 50 credits, 10 data
```

#### Building Defenses
```bash
# After claiming a tile
$ ssh root@10.0.0.50
$ upload firewall_config /etc/firewall/
[+] Firewall installed
[+] Tile defense increased by 50%
```

## Testing the System

### Quick Start

1. Open the test scene:
```bash
godot Scenes/HackingTest.tscn
```

2. Try basic commands:
```bash
$ help
$ nmap 192.168.1.100
$ whois 10.0.0.1
$ searchsploit sql
```

3. Attempt an exploit:
```bash
$ exploit sqli_001 192.168.1.100
```

4. Check your progress:
```bash
$ status
$ level
$ resources
```

### Keyboard Shortcuts

- **F1:** Quick help
- **F2:** Quick status
- **F3:** Quick resources
- **ESC:** Disconnect from current session

## Advanced Techniques

### Reconnaissance Workflow
```bash
# 1. Scan subnet for hosts
$ nmap 10.0.0.0/24

# 2. Identify promising targets
$ nmap 10.0.0.50 -p -sV -O

# 3. Check ownership
$ whois 10.0.0.50

# 4. Search for applicable exploits
$ searchsploit http
```

### Exploitation Workflow
```bash
# 1. Find vulnerability
$ searchsploit apache

# 2. Run exploit
$ exploit bof_002 10.0.0.50

# 3. Establish connection
$ ssh root@10.0.0.50

# 4. Deploy persistence
$ persist

# 5. Upload tools
$ upload backdoor.sh /tmp/
```

### Defense Evasion
```bash
# 1. Check for security
$ nmap 10.0.0.50 -p

# 2. If firewall detected, use stealth
$ exploit auth_003 10.0.0.50  # Session hijacking

# 3. Avoid detection
$ shell
$ persist rootkit
```

## Files

### Core System
- `Scripts/Terminal/HackingTerminalSystem.cs` - Main terminal logic
- `Scripts/Terminal/ExploitDatabase.cs` - Exploit definitions
- `Scripts/Terminal/NetworkScanner.cs` - Network reconnaissance

### Test Scene
- `Scripts/Terminal/HackingTestController.cs` - Test controller
- `Scenes/HackingTest.tscn` - Test scene
- `Assets/Fonts/monospace.tres` - Terminal font

## Future Enhancements

### Planned Features
1. **Multiplayer Hacking** - Attack other players' tiles
2. **Scripting System** - Write custom exploit scripts
3. **Tool Marketplace** - Buy/sell hacking tools
4. **Capture The Flag** - Competitive hacking challenges
5. **Social Engineering** - Phishing and pretexting attacks
6. **Hardware Hacking** - IoT and embedded device exploits
7. **Wireless Attacks** - WiFi and Bluetooth exploitation
8. **Forensics** - Investigate and trace attacks

### Extension Points
```csharp
// Add custom commands
hackingTerminal.RegisterCommand(new HackingCommand {
    Name = "custom_tool",
    Description = "My custom hacking tool",
    Execute = MyCustomToolFunction
});

// Add custom exploits
exploitDB.AddExploit(new Exploit {
    Id = "custom_001",
    Name = "Custom Exploit",
    Type = ExploitType.RCE,
    // ...
});
```

## Credits

- **Concept:** Realistic hacking simulation for ZeroDayMMO
- **Implementation:** C# / Godot 4.x
- **Inspiration:** Real-world penetration testing tools (nmap, Metasploit, sqlmap)
- **Theme:** Cyberpunk hacking MMO with IPv4-based world
