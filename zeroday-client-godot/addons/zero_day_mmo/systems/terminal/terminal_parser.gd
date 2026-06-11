extends Node

class CommandDefinition:
	var name: String
	var short_description: String
	var required_level: int
	var cpu_cost: int
	var usage: String
	var long_description: String
	var color: String

	func _init(n: String, sd: String, rl: int, cc: int, u: String, ld: String, c: String):
		name = n
		short_description = sd
		required_level = rl
		cpu_cost = cc
		usage = u
		long_description = ld
		color = c

class ParsedCommand:
	var command: CommandDefinition
	var raw_name: String
	var args: Array[String]
	var arg_string: String
	var is_valid: bool
	var level_requirement: int

var all_commands: Array[CommandDefinition] = []

func _ready():
	_initialize_commands()

func _initialize_commands() -> void:
	all_commands = [
		_command("help", "Shows available commands or help for a specific command", 1, 0, "help [command]", "Display usage information for all commands or a specific one.", "#cccccc"),
		_command("scan", "Scan a target IP for open ports and services", 1, 5, "scan <target_ip>", "Performs a basic port scan on the target system.", "#00FF00"),
		_command("connect", "Connect to a remote host", 1, 5, "connect <ip>", "Establish a connection to a remote system.", "#00FF00"),
		_command("whoami", "Display current user info", 1, 0, "whoami", "Show your current username and session information.", "#00FF00"),
		_command("status", "Display current system status", 1, 0, "status", "Shows your current stats.", "#00FF00"),
		_command("ls", "List directory contents", 1, 2, "ls [path]", "List files and directories on the current system.", "#00FF00"),
		_command("cat", "Read file contents", 2, 2, "cat <file>", "Display the contents of a file.", "#00FF00"),
		_command("ifconfig", "Show network interfaces", 2, 3, "ifconfig", "Display network interface configuration.", "#00FF00"),
		_command("scan_deep", "Deep scan revealing vulnerabilities", 10, 15, "scan_deep <target_ip>", "Advanced scan that reveals OS version and vulnerabilities.", "#00FF00"),
		_command("nmap", "Network mapper - detailed reconnaissance", 3, 15, "nmap <target>", "Full network mapping with OS detection.", "#00FF00"),
		_command("ping", "Ping a target to check connectivity", 2, 3, "ping <target_ip>", "Send ICMP echo request.", "#00FF00"),
		_command("traceroute", "Trace network path to target", 3, 10, "traceroute <target_ip>", "Display the route packets take.", "#00FF00"),
		_command("whois", "Look up domain/IP registration info", 2, 3, "whois <domain>", "Query WHOIS databases.", "#00FF00"),
		_command("ssh", "Secure Shell connection", 4, 10, "ssh <user>@<target>", "Establish an SSH connection.", "#00FF00"),
		_command("ftp", "Connect via FTP", 4, 8, "ftp <ip>", "Establish an FTP connection.", "#00FF00"),
		_command("exploit", "Run an exploit against a target", 5, 25, "exploit <target>", "Attempt to exploit a vulnerability.", "#FF6600"),
		_command("bruteforce", "Brute force credentials", 6, 30, "bruteforce <target>", "Crack credentials through brute force.", "#FF6600"),
		_command("decrypt", "Decrypt intercepted data", 6, 20, "decrypt <data>", "Decrypt captured data.", "#FF6600"),
		_command("sqlmap", "SQL injection scanner", 7, 25, "sqlmap <url>", "Automated SQL injection tool.", "#FF6600"),
		_command("backdoor", "Install a backdoor", 7, 20, "backdoor <target_ip>", "Install persistent backdoor.", "#FF6600"),
		_command("sniff", "Capture network packets", 8, 15, "sniff [interface]", "Capture and analyze network traffic.", "#00FF00"),
		_command("spoof", "Spoof network identity", 8, 20, "spoof <ip>", "Spoof your network identity.", "#00FFFF"),
		_command("proxy", "Route traffic through proxy chain", 9, 15, "proxy [list|add|remove|status]", "Manage proxy chains.", "#00FFFF"),
		_command("encrypt", "Encrypt your data", 9, 15, "encrypt <data>", "Encrypt sensitive data.", "#00FF00"),
		_command("worm", "Deploy self-replicating worm", 10, 40, "worm <target>", "Deploy a worm to spread across network.", "#FF6600"),
		_command("firewall", "Configure firewall rules", 10, 25, "firewall [enable|disable|status]", "Manage your firewall.", "#00FFFF"),
		_command("trace", "Trace an attack back to source", 11, 20, "trace <incident_id>", "Trace a cyber attack to its origin.", "#00FFFF"),
		_command("honeypot", "Deploy a honeypot", 12, 30, "honeypot [deploy|status|log]", "Set up a decoy system.", "#00FFFF"),
		_command("overload", "Overload target with traffic", 12, 50, "overload <ip>", "Flood target with traffic.", "#FF4444"),
		_command("crack", "Crack password hashes", 13, 35, "crack <hash>", "Crack password hashes.", "#FF6600"),
		_command("rootkit", "Deploy rootkit", 14, 45, "rootkit <target_ip>", "Deploy advanced rootkit.", "#FF6600"),
		_command("botnet", "Control botnet nodes", 15, 30, "botnet [command]", "Control your botnet network.", "#FF4444"),
		_command("dnshijack", "Hijack DNS records", 16, 35, "dnshijack <domain>", "Hijack DNS records.", "#FF6600"),
		_command("zero-day", "Deploy zero-day exploit", 18, 60, "zero-day <target>", "Deploy zero-day exploit.", "#FF4444"),
		_command("ai-assist", "AI-assisted hacking", 20, 40, "ai-assist <query>", "Use AI assistance.", "#00FFFF"),
		_command("ids", "Intrusion Detection System", 8, 10, "ids [status|alerts|config]", "Monitor your IDS.", "#00FFFF"),
		_command("cloak", "Activate stealth mode", 15, 20, "cloak [on|off]", "Activate stealth protocols.", "#00FFFF"),
		_command("ddos", "Launch a DDoS attack", 18, 30, "ddos <target_ip>", "Launch a DDoS attack.", "#FF4444"),
		_command("payload", "Generate a custom payload", 10, 15, "payload [type]", "Create custom payloads.", "#FF6600"),
		_command("scan_subnet", "Scan an entire subnet", 8, 12, "scan_subnet <subnet/CIDR>", "Scan all IPs in a subnet.", "#00FF00"),
		_command("log", "View system and activity logs", 3, 2, "log [type]", "Display system logs.", "#808080"),
		_command("analyze", "Deep analysis of network nodes", 12, 15, "analyze <target_ip>", "Comprehensive node analysis.", "#00FFFF"),
		_command("alarm", "Set up network alarms", 8, 8, "alarm [set|list|clear]", "Configure automated alarms.", "#00FFFF"),
		_command("upgrade", "Upgrade your system", 5, 10, "upgrade <component>", "Upgrade CPU, RAM, or Bandwidth.", "#00FF00"),
		_command("profile", "View and edit your profile", 1, 0, "profile [view|edit]", "Display your hacking profile.", "#00FF00"),
		_command("auction", "Access black market auction", 15, 5, "auction [list|bid|sell]", "Browse the underground market.", "#FF00FF"),
		_command("tasks", "Manage your active tasks", 1, 0, "tasks [list|accept|complete]", "View or manage tasks.", "#FFFF00"),
		_command("faction", "Access faction features", 5, 0, "faction [info|create|donate]", "Manage your faction.", "#00FFFF"),
		_command("network", "Open network visualization", 1, 0, "network [show|hide|refresh]", "Toggle the network map.", "#00FFFF"),
		_command("story", "Progress through storyline", 3, 0, "story [status|start|advance]", "View or advance the storyline.", "#00FFFF"),
		_command("research", "Open the research lab", 5, 0, "research [list|start|claim]", "Research new exploits.", "#AA44FF"),
		_command("kmap", "Open the Knowledge Map", 3, 0, "kmap [show|fragments]", "View the Knowledge Map.", "#4488FF"),
		_command("nexus", "Nexus scripting engine", 8, 0, "nexus [run|save|load|edit]", "Access the Nexus scripting language.", "#00FF00"),
		_command("scripts", "Manage saved Nexus scripts", 8, 0, "scripts [list|delete|rename]", "Manage your Nexus scripts.", "#00FF00"),
		_command("inventory", "Open your item inventory", 3, 0, "inventory [list|use|drop]", "View and manage your items.", "#00FF00"),
		_command("achievements", "View your achievements", 1, 0, "achievements [list|recent]", "Browse your unlocked achievements.", "#FFD700"),
		_command("challenges", "View daily/weekly challenges", 3, 0, "challenges [daily|weekly]", "Check your challenges.", "#FFFF00"),
		_command("events", "View active world events", 5, 0, "events [list|participate]", "See active world events.", "#FF6600"),
		_command("skills", "Open the skill tree", 2, 0, "skills [tree|list]", "View and unlock skills.", "#00FF00"),
		_command("evolutions", "Open the evolution skill tree", 2, 0, "evolutions [show]", "View your evolution tree.", "#00FFFF"),
		_command("daily", "Open daily reward calendar", 1, 0, "daily", "Claim your daily rewards.", "#FFD700"),
		_command("arena", "Enter the PvP arena", 10, 0, "arena [find|status|stats]", "Enter the PvP hacking arena.", "#FF4444"),
		_command("defense", "Start network defense mode", 8, 0, "defense [start|status]", "Defend your network.", "#00FFFF"),
		_command("prestige", "Prestige system info", 50, 0, "prestige [info|reset]", "Prestige for permanent bonuses.", "#FFD700"),
		_command("clear", "Clear the terminal screen", 1, 0, "clear", "Clear all output from the terminal.", "#FFFFFF"),
		_command("notifications", "Open notifications panel", 1, 0, "notifications [show]", "View your notification history.", "#00FF00"),
		_command("mail", "Access in-game mail", 5, 0, "mail [inbox|read|send]", "Send and receive messages.", "#00FF00"),
		_command("party", "Manage your party", 3, 0, "party [create|invite|leave]", "Form or manage a party.", "#00FF00"),
		_command("friends", "Manage friends list", 3, 0, "friends [list|add|remove]", "Add or remove friends.", "#00FF00"),
		_command("shop", "Visit the in-game shop", 1, 0, "shop [browse|buy]", "Browse and purchase items.", "#FFFF00"),
		_command("chain", "Manage automation command chains", 4, 0, "chain [list|info|run|delete]", "Execute sequenced commands.", "#00FF00"),
		_command("equip", "Equip an item from inventory", 1, 0, "equip <item_id>", "Equip a weapon or tool.", "#00FF00"),
		_command("unequip", "Unequip an item", 1, 0, "unequip [slot|item_id]", "Remove an equipped item.", "#00FF00"),
		_command("career", "View or choose your career path", 5, 0, "career [whitehat|blackhat|status]", "Choose your path.", "#00FFFF"),
		_command("map", "Open the world map and travel", 3, 0, "map [show|travel|zone] [id]", "View the world map.", "#00FF00"),
		_command("hideout", "Manage your hideout", 3, 0, "hideout [status|build|upgrade] [mod]", "Build and upgrade your hideout.", "#00FFFF"),
		_command("quests", "View and manage quests", 2, 0, "quests [list|accept|abandon] [id]", "View active quests.", "#FFFF00"),
		_command("boss", "Challenge zone bosses", 10, 0, "boss [info|fight]", "Challenge zone bosses for rare loot.", "#FF4444"),
		_command("arrest", "Attempt to arrest a criminal", 5, 0, "arrest [target_id]", "Arrest high-heat players.", "#00FFFF"),
		_command("bounty", "View active bounties", 5, 0, "bounty [list|track]", "View bounties on players.", "#FFFF00"),
		_command("netstat", "Display network connections", 1, 3, "netstat [-a|-n]", "Display active connections.", "#00FF00"),
		_command("inject", "Inject malicious code", 2, 10, "inject [target] [payload]", "Inject code into a target.", "#FF6600"),
		_command("equipment", "View equipped items", 1, 0, "equipment", "Open the Inventory Equipment panel.", "#00FF00"),
		_command("reconnect", "Reconnect to the server", 1, 0, "reconnect", "Re-establish connection.", "#FFFF00"),
	]

func _command(n, sd, rl, cc, u, ld, c) -> CommandDefinition:
	return CommandDefinition.new(n, sd, rl, cc, u, ld, c)

func parse(input: String) -> ParsedCommand:
	if input.is_empty(): return null
	input = input.strip_edges()
	var parts = _split_args(input)
	if parts.size() == 0: return null
	var cmd_name = parts[0].to_lower()
	var matched = _find_command(cmd_name)
	if matched == null and (cmd_name == "h" or cmd_name == "?"):
		matched = _find_command("help")

	var result = ParsedCommand.new()
	result.command = matched
	result.raw_name = parts[0]
	result.args = parts.slice(1)
	result.arg_string = " ".join(parts.slice(1))
	result.is_valid = matched != null
	result.level_requirement = matched.required_level if matched else 999
	return result

func _find_command(name: String) -> CommandDefinition:
	for cmd in all_commands:
		if cmd.name == name:
			return cmd
	for cmd in all_commands:
		if cmd.name.begins_with(name) and name.length() >= 3:
			return cmd
	return null

func _split_args(input: String) -> Array[String]:
	var args: Array[String] = []
	var current = ""
	var in_quote = false
	for i in input.length():
		var c = input[i]
		if c == '"' or c == "'":
			in_quote = not in_quote
		elif c == ' ' and not in_quote:
			if current.length() > 0:
				args.append(current)
				current = ""
		else:
			current += c
	if current.length() > 0:
		args.append(current)
	return args

func get_help_text(command_name: String = "") -> String:
	if command_name.is_empty():
		var lines = PackedStringArray()
		lines.append("[color=#00FF00]╔══════════════════════════════════════╗")
		lines.append("║        ZERODAY COMMAND LIST         ║")
		lines.append("╠══════════════════════════════════════╣")
		lines.append("║  Type 'help [command]' for details   ║")
		lines.append("╚══════════════════════════════════════╝[/color]")

		var categories = {
			"NETWORK": ["scan", "scan_deep", "nmap", "ping", "traceroute", "netstat", "whois", "scan_subnet", "trace"],
			"EXPLOIT": ["exploit", "backdoor", "rootkit", "crack", "decrypt", "encrypt", "payload", "inject"],
			"DEFENSE": ["firewall", "honeypot", "ids", "cloak", "proxy", "alarm"],
			"UTILITY": ["help", "clear", "status", "profile", "log", "analyze", "upgrade", "shop", "auction", "equip", "unequip", "career"],
			"SOCIAL": ["tasks", "faction", "party", "friends", "mail", "notifications"],
			"SYSTEM": ["network", "story", "research", "kmap", "nexus", "scripts", "inventory", "achievements", "challenges", "events", "skills", "evolutions", "daily", "chain", "map", "hideout", "quests", "equipment"],
			"COMBAT": ["arena", "defense", "ddos", "boss", "arrest", "bounty"],
			"ADVANCED": ["prestige", "exit", "reconnect"]
		}

		for cat_name in categories:
			var cat_cmds = categories[cat_name]
			var available = []
			for cn in cat_cmds:
				var c = _find_command(cn)
				if c: available.append(c)
			if available.is_empty(): continue
			lines.append("\n[color=#00FFFF]── [%s] ────────────────────────[/color]" % cat_name)
			for c in available:
				lines.append("  [color=#FFFFFF]%-15s[/color] [color=#808080]%s[/color]" % [c.name, c.short_description])

		lines.append("\n[color=#808080]Total commands: %d | Type 'help <command>' for details.[/color]" % all_commands.size())
		return "\n".join(lines)

	var specific = _find_command(command_name.to_lower())
	if specific == null:
		return "[color=#FF4444]Command '%s' not found.[/color]" % command_name

	return ("[color=#00FF00]╔══════════════════════════════════════╗\n" +
		"║  %-36s║\n" +
		"╚══════════════════════════════════════╝[/color]\n\n" +
		"[color=#FFFFFF]%s[/color]\n\n" +
		"[color=#00FFFF]Usage:[/color]    [color=#FFFFFF]%s[/color]\n" +
		"[color=#00FFFF]Required:[/color] [color=#FFFFFF]Level %d[/color]\n" +
		"[color=#00FFFF]Cost:[/color]      [color=#FFFFFF]%d CPU[/color]") % [specific.name.to_upper(), specific.long_description, specific.usage, specific.required_level, specific.cpu_cost]
