extends Node

signal login_result(success: bool, data: Dictionary)
signal register_result(success: bool, data: Dictionary)
signal command_result(success: bool, output: String, data: Dictionary)

var network: Node
var ui: Node
var _inventory_panel: Node
var _resource_tick_timer: float = 0.0

func _ready():
	process_mode = PROCESS_MODE_ALWAYS
	network = NetworkManager
	ui = UIManager
	_connect_signals()

func _connect_signals():
	network.connected.connect(_on_connected)
	network.connection_failed.connect(_on_connection_failed)
	network.disconnected.connect(_on_disconnected)

func connect_to_server() -> void:
	ZeroDayCore.game_state = ZeroDayData.GameState.CONNECTING
	ui.show_loading(true)
	var url = ServerConfig.get_server_url()
	var error = network.connect_to_server(url)
	if error != OK:
		ui.show_loading(false)
		ui.show_error("Failed to start connection: %s" % error)

func _on_connected() -> void:
	ZeroDayCore.game_state = ZeroDayData.GameState.LOGIN
	ui.show_loading(false)
	ui.show_login_panel()
	if ZeroDayCore.current_player:
		ui.show_terminal()
		ZeroDayCore.game_state = ZeroDayData.GameState.TERMINAL

func _on_connection_failed(msg: String) -> void:
	ZeroDayCore.game_state = ZeroDayData.GameState.DISCONNECTED
	ui.show_loading(false)
	ui.show_error(msg)
	ui.append_terminal_output("[ERROR] %s" % msg, "#ff4444")

func _on_disconnected(reason: String) -> void:
	ZeroDayCore.game_state = ZeroDayData.GameState.DISCONNECTED
	ui.append_terminal_output("[DISCONNECTED] %s" % reason, "#ff4444")

func _process(delta: float) -> void:
	_resource_tick_timer += delta
	if _resource_tick_timer >= 3.0 and ZeroDayCore.current_player:
		_resource_tick_timer = 0.0
		ZeroDayCore.current_player.regenerate_resources()
		var player_dict = _player_to_dict(ZeroDayCore.current_player)
		ui.update_hud(player_dict)

func login(username: String, password: String) -> void:
	if username.is_empty() or password.is_empty():
		ui.show_error("Please enter username and password")
		return
	var result = await network.login(username, password)
	var success = result.get("success", false) or result.get("type", "") == "login_success"
	if success:
		var player_data = result.get("player", result.get("payload", result))
		if typeof(player_data) == TYPE_DICTIONARY:
			ZeroDayCore.update_player(player_data)
			_on_login_success(player_data)
		login_result.emit(true, result)
	else:
		var err = result.get("error", result.get("message", "Login failed"))
		ui.show_error(err)
		login_result.emit(false, result)

func register_player(username: String, password: String) -> void:
	if username.is_empty() or password.is_empty():
		ui.show_error("Please enter username and password")
		return
	if password.length() < 4:
		ui.show_error("Password must be at least 4 characters")
		return
	var result = await network.register_player(username, password)
	var success = result.get("success", false) or result.get("type", "") == "register_success"
	if success:
		var player_data = result.get("player", result.get("payload", result))
		if typeof(player_data) == TYPE_DICTIONARY:
			ZeroDayCore.update_player(player_data)
			_on_register_success(player_data)
		register_result.emit(true, result)
	else:
		var err = result.get("error", result.get("message", "Registration failed"))
		ui.show_error(err)
		register_result.emit(false, result)

func _on_login_success(player_data: Dictionary) -> void:
	ZeroDayCore.game_state = ZeroDayData.GameState.TERMINAL
	ui.show_terminal()
	ui.update_hud(player_data)
	var ip = player_data.get("current_ip", "127.0.0.1")
	var p = ZeroDayCore.current_player
	if p: p.current_ip = ip
	ui.append_terminal_output("Welcome back, %s!" % player_data.get("username", "h4ck3r"), "#00FF00")
	ui.append_terminal_output("You are level %d at %s. Type 'help' to see all commands." % [player_data.get("level", 1), ip], "#cccccc")

func _on_register_success(player_data: Dictionary) -> void:
	ZeroDayCore.game_state = ZeroDayData.GameState.TERMINAL
	ui.show_terminal()
	ui.update_hud(player_data)
	var ip = player_data.get("current_ip", "127.0.0.1")
	var p = ZeroDayCore.current_player
	if p: p.current_ip = ip
	ui.append_terminal_output("Account created! Welcome, %s." % player_data.get("username", "h4ck3r"), "#00FF00")
	ui.append_terminal_output("Your journey begins at %s. Type 'help' to see what you can do." % ip, "#cccccc")
	ui.append_terminal_output("Tip: Try 'daily' to claim your first login reward!", "#FFFF00")

func execute_command(input: String) -> void:
	if input.is_empty() or ZeroDayCore.current_player == null:
		return
	ui.append_terminal_output("> %s" % input, "#00FF00")

	var parsed = TerminalParser.parse(input)
	if parsed == null or not parsed.is_valid:
		ui.append_terminal_output("Command not found: '%s'. Type 'help' to see available commands." % input, "#FF4444")
		return

	if parsed.command.required_level > ZeroDayCore.current_player.level:
		ui.append_terminal_output("Requires level %d. Your level: %d." % [parsed.command.required_level, ZeroDayCore.current_player.level], "#FF4444")
		return

	var cpu_cost = parsed.command.cpu_cost
	ui.append_terminal_output("[Cost: %d CPU | Balance: %d/%d MHz]" % [cpu_cost, ZeroDayCore.current_player.cpu, ZeroDayCore.current_player.max_cpu], "#808080")

	if cpu_cost > ZeroDayCore.current_player.cpu:
		ui.append_terminal_output("Insufficient CPU. Need %d, have %d." % [cpu_cost, ZeroDayCore.current_player.cpu], "#FF4444")
		return

	var cmd_name = parsed.command.name
	if _handle_special_command(cmd_name, parsed.args):
		ZeroDayCore.current_player.cpu -= cpu_cost
		ui.update_hud(_player_to_dict(ZeroDayCore.current_player))
		return

	ZeroDayCore.current_player.cpu -= cpu_cost
	ui.update_hud(_player_to_dict(ZeroDayCore.current_player))

	var result = await network.execute_command(input)
	if result.is_empty():
		ui.append_terminal_output("[ERROR] No response from server.", "#FF4444")
		ZeroDayCore.current_player.cpu += cpu_cost
		ui.update_hud(_player_to_dict(ZeroDayCore.current_player))
		return

	if result.has("error"):
		var err_msg = result.get("error", "Unknown error")
		ui.append_terminal_output("[ERROR] %s" % err_msg, "#FF4444")
		ZeroDayCore.current_player.cpu += cpu_cost
		ui.update_hud(_player_to_dict(ZeroDayCore.current_player))
		return

	var output = result.get("output", "Command executed successfully.")
	ui.append_terminal_output(output, "#cccccc")

	if result.has("player") and typeof(result.player) == TYPE_DICTIONARY:
		ZeroDayCore.update_player(result.player)
		ui.update_hud(result.player)

	if result.has("events"):
		_process_game_events(result.events)
	if result.has("notifications"):
		_process_notifications(result.notifications)

	command_result.emit(true, output, result)

func _handle_special_command(cmd: String, args: Array) -> bool:
	match cmd:
		"help":
			var help_text = TerminalParser.get_help_text(args[0] if args.size() > 0 else "")
			ui.append_terminal_output(help_text, "#cccccc")
		"clear":
			ui.clear_terminal()
		"status":
			var p = ZeroDayCore.current_player
			var s = "[color=#00FF00]╔══════════════════════════════════════╗\n║  PLAYER STATUS                      ║\n╚══════════════════════════════════════╝[/color]\n\n"
			s += "User:     %s (Lv.%d)\n" % [p.username, p.level]
			s += "IP:       %s\n" % p.current_ip
			s += "XP:       %d/%d\n" % [p.experience, p.experience_to_next]
			s += "CPU:      %d/%d MHz\n" % [p.cpu, p.max_cpu]
			s += "RAM:      %d/%d MB\n" % [p.ram, p.max_ram]
			s += "BW:       %d/%d Mbps\n" % [p.bandwidth, p.max_bandwidth]
			s += "Credits:  $%d\n" % p.credits
			s += "Rep:      %d\n" % p.reputation
			s += "Tasks:    %d active | %d completed" % [p.active_tasks.size(), p.completed_tasks_count]
			ui.append_terminal_output(s, "#cccccc")
		"exit":
			ui.append_terminal_output("Disconnecting from ZeroDay network...", "#FF4444")
			network.disconnect_from_server()
		"reconnect":
			ui.append_terminal_output("Attempting reconnection...", "#FFFF00")
			connect_to_server()
		"travel":
			_handle_travel(args)
		"attack":
			_handle_attack(args)
		"build_base":
			_handle_build_base(args)
		"claim":
			_handle_claim(args)
		"base":
			_handle_base_status()
		"tutorial":
			_handle_tutorial(args)
		"flee":
			_handle_flee()
		"scan_ip":
			_handle_scan_ip(args)
		"router":
			_handle_router(args)
		_:
			return false
	return true

func _handle_travel(args: Array):
	if args.is_empty():
		ui.append_terminal_output("Usage: travel <target_ip>", "#FFFF00")
		return
	var target = args[0]
	var current = ZeroDayCore.current_player
	if current == null: return
	var octets = target.split(".")
	if octets.size() != 4:
		ui.append_terminal_output("Invalid IP format.", "#FF4444")
		return
	var dist = _ip_distance(current.current_ip, target)
	var bw_needed = dist * 10
	if current.bandwidth < bw_needed:
		ui.append_terminal_output("Insufficient bandwidth. Need %d, have %d." % [bw_needed, current.bandwidth], "#FF4444")
		return
	var is_local = target.begins_with("127.")
	if is_local:
		ui.append_terminal_output("Traveling to %s... Connected." % target, "#00FF00")
	else:
		ui.append_terminal_output("Traveling outside localhost to %s... Firewall protection lost. PvP zone active." % target, "#FF4444")
	var old_ip = current.current_ip
	current.current_ip = target
	ZeroDayCore.player_updated.emit(_player_to_dict(current))
	ui.append_terminal_output("Arrived at %s. Use 'scan_ip %s' to scan the area." % [target, target], "#00FF00")
	ZeroDayCore.notification_received.emit("TRAVEL", "Arrived", "You are now at %s" % target)
	ZeroDayCore.player_moved.emit(target, old_ip)

func _handle_scan_ip(args: Array):
	if args.is_empty():
		ui.append_terminal_output("Usage: scan_ip <target_ip>", "#FFFF00")
		return
	var target = args[0]
	var octets = target.split(".")
	if octets.size() != 4:
		ui.append_terminal_output("Invalid IP format.", "#FF4444")
		return
	var o1 = int(octets[0])
	var o2 = int(octets[1])
	var is_local = target.begins_with("127.")
	var is_dark = o1 >= 224
	var is_perimeter = o1 >= 192 and o1 < 224
	var is_core = o1 >= 64 and o1 < 128
	var biome_name = "Perimeter"
	var danger_name = "Low"
	if is_local: biome_name = "Localhost"
	elif is_dark: biome_name = "DarkWeb"
	elif is_perimeter: biome_name = "Perimeter"
	elif o1 >= 128: biome_name = "Firewall"
	elif o1 >= 64: biome_name = "DataStreams"
	elif o1 >= 32: biome_name = "CoreNetwork"
	else: biome_name = "DeepNet"
	danger_name = "Safe"
	if not is_local:
		if o1 >= 224: danger_name = "Extreme"
		elif o1 >= 192: danger_name = "High"
		elif o1 >= 128: danger_name = "Medium"
		elif o1 >= 64: danger_name = "Low"
	var nodes_here = _generate_area_nodes(target)
	var output = "[color=#00FF00]╔══════════════════════════════════════╗\n"
	output += "║  SCAN RESULT: %-19s║\n" % target
	output += "╚══════════════════════════════════════╝[/color]\n"
	output += "Biome:  %s\n" % biome_name
	output += "Danger: %s\n" % danger_name
	output += "PvP:    %s\n\n" % ("NO (Protected)" if is_local else "YES (Unprotected)")
	output += "Nodes discovered:\n"
	for n in nodes_here:
		var icon = ""
		match n.type:
			"router": icon = "[color=#00FFFF][RTR][/color]"
			"crypto_mine": icon = "[color=#FFD700][$$$][/color]"
			"data_center": icon = "[color=#4488FF][DAT][/color]"
			"virus": icon = "[color=#FF4444][VIR][/color]"
			"worm": icon = "[color=#FF6600][WRM][/color]"
			"devourer": icon = "[color=#880000][DEV][/color]"
			"ai_overlord": icon = "[color=#8800CC][AI][/color]"
			"base": icon = "[color=#00FF00][BAS][/color]"
			"empty": icon = "[color=#555555][   ][/color]"
			"firewall": icon = "[color=#FFFFFF][FWL][/color]"
			_: icon = "[color=#555555][   ][/color]"
		output += "  %s %s" % [icon, n.ip]
		if n.has("owner") and n.owner: output += " [color=#FFFF00](%s)[/color]" % n.owner
		output += "\n"
	ui.append_terminal_output(output, "#cccccc")

func _handle_attack(args: Array):
	if args.is_empty():
		ui.append_terminal_output("Usage: attack <target_ip>", "#FFFF00")
		return
	var target = args[0]
	var p = ZeroDayCore.current_player
	if p == null: return
	var dist = _ip_distance(p.current_ip, target)
	if dist > 5:
		ui.append_terminal_output("Target too far. Travel closer first.", "#FF4444")
		return
	var nodes = _generate_area_nodes(p.current_ip)
	var target_node = null
	for n in nodes:
		if n.ip == target:
			target_node = n
			break
	if target_node == null:
		target_node = _generate_node_at(target)
	if target_node.type == "empty" or target_node.type == "router" or target_node.type == "data_center":
		ui.append_terminal_output("No hostile entities at %s." % target, "#FFFF00")
		return
	var enemy_hp = 20
	var enemy_atk = 5
	var enemy_def = 2
	var enemy_name = target_node.type
	match enemy_name:
		"virus":
			enemy_hp = 20 + p.level * 3
			enemy_atk = 5 + p.level
			enemy_def = 2 + p.level / 2
		"worm":
			enemy_hp = 35 + p.level * 4
			enemy_atk = 8 + p.level * 1.5
			enemy_def = 3 + p.level / 2
		"devourer":
			enemy_hp = 80 + p.level * 5
			enemy_atk = 15 + p.level * 2
			enemy_def = 8 + p.level
		"ai_overlord":
			enemy_hp = 200 + p.level * 8
			enemy_atk = 25 + p.level * 3
			enemy_def = 15 + p.level * 1.5
	var player_hp = 100 + p.level * 10
	var player_atk = 10 + p.level * 2
	var player_def = 5 + p.level
	var round = 0
	ui.append_terminal_output("[color=#FF4444]╔══════════════════════════════════════╗\n║  COMBAT: %s vs %-17s║\n╚══════════════════════════════════════╝[/color]" % [enemy_name.to_upper(), target], "#FF4444")
	while player_hp > 0 and enemy_hp > 0:
		round += 1
		var p_dmg = max(1, player_atk - enemy_def / 2 + randi() % 6 - 3)
		var e_dmg = max(1, enemy_atk - player_def / 2 + randi() % 4 - 2)
		enemy_hp -= p_dmg
		player_hp -= e_dmg
		ui.append_terminal_output("Round %d: You deal %d damage | Enemy deals %d damage" % [round, p_dmg, e_dmg], "#cccccc")
		if round >= 20:
			ui.append_terminal_output("[color=#FF4444]Combat timeout - both sides withdraw.[/color]", "#FF4444")
			return
	if enemy_hp <= 0:
		var loot = {}
		match enemy_name:
			"virus": loot = {"credits": 10 + p.level * 3, "data": 3 + p.level}
			"worm": loot = {"credits": 25 + p.level * 5, "data": 8 + p.level * 2, "scrap_hardware": 2}
			"devourer": loot = {"credits": 75 + p.level * 8, "data": 20 + p.level * 3, "crypto_chunks": 3}
			"ai_overlord": loot = {"credits": 250 + p.level * 12, "data": 50 + p.level * 5, "ai_core": 2, "crypto_chunks": 10}
		var loot_text = ""
		for item in loot:
			loot_text += "%s x%d " % [item, loot[item]]
			match item:
				"credits": p.credits += loot[item]
				"data": p.experience += loot[item] * 2
		ui.append_terminal_output("[color=#00FF00]Victory! %s defeated after %d rounds.[/color]" % [enemy_name, round], "#00FF00")
		ui.append_terminal_output("Loot: %s" % loot_text, "#FFD700")
		ZeroDayCore.player_updated.emit(_player_to_dict(p))
		ZeroDayCore.notification_received.emit("COMBAT", "Victory", "Defeated %s at %s" % [enemy_name, target])
		ZeroDayCore.combat_occurred.emit(target, "victory")
	else:
		var death_penalty = int(p.credits * 0.1)
		p.credits -= death_penalty
		ZeroDayCore.player_updated.emit(_player_to_dict(p))
		ui.append_terminal_output("[color=#FF4444]Defeated! You lost $%d and respawn at 127.0.0.1.[/color]" % death_penalty, "#FF4444")
		p.current_ip = "127.0.0.1"
		ZeroDayCore.player_updated.emit(_player_to_dict(p))
		ui.append_terminal_output("Respawning at localhost (127.0.0.1)...", "#FFFF00")
		ZeroDayCore.notification_received.emit("COMBAT", "Defeated", "You lost to %s at %s" % [enemy_name, target])
		ZeroDayCore.combat_occurred.emit(target, "defeat")

func _handle_build_base(args: Array):
	var p = ZeroDayCore.current_player
	if p == null: return
	var current_ip = p.current_ip
	if current_ip.begins_with("127."):
		ui.append_terminal_output("Cannot build a base inside localhost (127.x.x.x). Travel outside first.", "#FF4444")
		return
	var base_key = "base_location"
	var existing = ZeroDayCore.get_pref(base_key, "")
	if existing != "":
		ui.append_terminal_output("You already have a base at %s." % existing, "#FFFF00")
		return
	ZeroDayCore.save_pref(base_key, current_ip)
	ZeroDayCore.save_pref("base_subnet", 24)
	ZeroDayCore.save_pref("control_nodes", "[]")
	ZeroDayCore.save_pref("base_bonuses", '{"bandwidth":10,"defense":5,"resource":5,"attack":5}')
	ui.append_terminal_output("[color=#00FF00]╔══════════════════════════════════════╗\n║  BASE ESTABLISHED                    ║\n╚══════════════════════════════════════╝[/color]", "#00FF00")
	ui.append_terminal_output("Base placed at %s. Subnet: /24." % current_ip, "#00FF00")
	ui.append_terminal_output("You can now control up to 256 IPs in this range. Use 'claim <ip>' to expand.", "#00FFFF")
	ZeroDayCore.notification_received.emit("BASE", "Base Established", "Your base is at %s" % current_ip)

func _handle_claim(args: Array):
	if args.is_empty():
		ui.append_terminal_output("Usage: claim <target_ip>", "#FFFF00")
		return
	var target = args[0]
	var base_key = "base_location"
	var base_ip = ZeroDayCore.get_pref(base_key, "")
	if base_ip == "":
		ui.append_terminal_output("You need a base first. Use 'build_base' outside localhost.", "#FF4444")
		return
	var dist = _ip_distance(base_ip, target)
	if dist > 256:
		ui.append_terminal_output("Target too far from your base subnet. Move closer.", "#FF4444")
		return
	var p = ZeroDayCore.current_player
	if p == null: return
	var nodes_here = _generate_area_nodes(target)
	var node = null
	for n in nodes_here:
		if n.ip == target:
			node = n
			break
	if node == null:
		node = _generate_node_at(target)
	if node.type in ["virus", "worm", "devourer", "ai_overlord"]:
		ui.append_terminal_output("Node %s is controlled by hostile entities. Use 'attack %s' to clear it first." % [target, target], "#FF4444")
		return
	if node.has("owner") and node.owner != "":
		ui.append_terminal_output("Node %s is already claimed by %s." % [target, node.owner], "#FF4444")
		return
	var control_nodes_str = ZeroDayCore.get_pref("control_nodes", "[]")
	var control_nodes = []
	if typeof(control_nodes_str) == TYPE_STRING:
		control_nodes = JSON.parse_string(control_nodes_str)
	if control_nodes == null: control_nodes = []
	control_nodes.append(target)
	ZeroDayCore.save_pref("control_nodes", JSON.stringify(control_nodes))
	var node_type_name = "Empty"
	if node.has("type"):
		match node.type:
			"crypto_mine": node_type_name = "Crypto Mine (+crypto)"
			"data_center": node_type_name = "Data Center (+data)"
			"router": node_type_name = "Router (+travel)"
			"empty": node_type_name = "Claimed Territory"
	ui.append_terminal_output("Node %s claimed as %s." % [target, node_type_name], "#00FF00")
	ZeroDayCore.notification_received.emit("CLAIM", "Node Claimed", "%s added to your subnet" % target)
	ZeroDayCore.node_claimed.emit(target, p.username)

func _handle_base_status():
	var base_ip = ZeroDayCore.get_pref("base_location", "")
	if base_ip == "":
		ui.append_terminal_output("No base established. Travel outside localhost and use 'build_base'.", "#FFFF00")
		return
	var subnet = ZeroDayCore.get_pref("base_subnet", 24)
	var control_str = ZeroDayCore.get_pref("control_nodes", "[]")
	var bonuses_str = ZeroDayCore.get_pref("base_bonuses", "{}")
	var control_nodes = []
	if typeof(control_str) == TYPE_STRING: control_nodes = JSON.parse_string(control_str)
	var bonuses = {}
	if typeof(bonuses_str) == TYPE_STRING: bonuses = JSON.parse_string(bonuses_str)
	if control_nodes == null: control_nodes = []
	if bonuses == null: bonuses = {}
	var output = "[color=#00FF00]╔══════════════════════════════════════╗\n║  BASE STATUS                        ║\n╚══════════════════════════════════════╝[/color]\n"
	output += "Location: %s\n" % base_ip
	output += "Subnet:   /%d\n\n" % subnet
	output += "[color=#00FFFF]--- Control Nodes (%d) ---[/color]\n" % control_nodes.size()
	for cn in control_nodes:
		output += "  %s\n" % cn
	output += "\n[color=#00FFFF]--- Bonuses ---[/color]\n"
	for b in bonuses:
		output += "  %s: +%d\n" % [b.capitalize(), bonuses[b]]
	output += "\n[color=#808080]Nodes provide stat bonuses. More nodes = stronger base. Use 'claim <ip>' to expand.[/color]"
	ui.append_terminal_output(output, "#cccccc")

func _handle_tutorial(args: Array):
	var subcmd = args[0] if args.size() > 0 else "status"
	match subcmd:
		"start":
			var completed = ZeroDayCore.get_pref("tutorial_completed", false) or ZeroDayCore.get_pref("tutorial_step", 0) >= 10
			if completed:
				ui.append_terminal_output("Tutorial already completed. Use 'help' to explore commands.", "#FFFF00")
				return
			ZeroDayCore.save_pref("tutorial_active", true)
			ui.append_terminal_output("[color=#00FF00]╔══════════════════════════════════════╗\n║  TUTORIAL STARTED                    ║\n╚══════════════════════════════════════╝[/color]", "#00FF00")
			ui.append_terminal_output("Step 1/10: Welcome to ZeroDay.", "#00FF00")
			ui.append_terminal_output("You are a hacker in a vast network. Your terminal is your weapon.", "#cccccc")
			ui.append_terminal_output("Try: 'help' to see commands | 'status' for your profile | 'scan_ip 127.0.0.2' to explore", "#FFFF00")
		"skip":
			ZeroDayCore.save_pref("tutorial_completed", true)
			ZeroDayCore.save_pref("tutorial_step", 10)
			ZeroDayCore.save_pref("tutorial_active", false)
			ui.append_terminal_output("Tutorial skipped. Type 'help' if you need assistance.", "#FFFF00")
		_":
			var step = ZeroDayCore.get_pref("tutorial_step", 0)
			var completed = ZeroDayCore.get_pref("tutorial_completed", false)
			if completed:
				ui.append_terminal_output("Tutorial: COMPLETED. Levels 1-5 basics mastered.", "#00FF00")
			else:
				ui.append_terminal_output("Tutorial progress: Step %d/10. Type 'tutorial start' to continue or 'tutorial skip'." % [step + 1], "#FFFF00")

func _handle_flee():
	ui.append_terminal_output("You withdraw from combat, losing some resources in the retreat.", "#FFFF00")
	var p = ZeroDayCore.current_player
	if p:
		var loss = int(p.credits * 0.05)
		p.credits -= loss
		ui.append_terminal_output("Lost $%d in the retreat." % loss, "#FF4444")
		ZeroDayCore.player_updated.emit(_player_to_dict(p))

func _handle_router(args: Array):
	var subcmd = args[0] if args.size() > 0 else "list"
	var p = ZeroDayCore.current_player
	match subcmd:
		"list":
			ui.append_terminal_output("[color=#00FFFF]╔══════════════════════════════════════╗\n║  KNOWN ROUTERS                      ║\n╚══════════════════════════════════════╝[/color]", "#00FFFF")
			var routers = ["127.0.1.1 (Gateway Router)"]
			var known = ZeroDayCore.get_pref("known_routers", "[]")
			if typeof(known) == TYPE_STRING:
				var parsed = JSON.parse_string(known)
				if parsed:
					for r in parsed: routers.append(r)
			for r in routers:
				ui.append_terminal_output("  %s" % r, "#00FFFF")
			ui.append_terminal_output("\nUse 'travel <router_ip>' to fast travel.", "#808080")
		"discover":
			var current = p.current_ip if p else "127.0.0.1"
			if current.begins_with("127."):
				ui.append_terminal_output("No new routers in localhost area. Travel further out.", "#FFFF00")
			else:
				var known = ZeroDayCore.get_pref("known_routers", "[]")
				var router_list = []
				if typeof(known) == TYPE_STRING:
					router_list = JSON.parse_string(known)
				if router_list == null: router_list = []
				var octets = current.split(".")
				var router_ip = "%s.%s.0.1" % [octets[0], octets[1]]
				if not router_ip in router_list:
					router_list.append(router_ip)
					ZeroDayCore.save_pref("known_routers", JSON.stringify(router_list))
					ui.append_terminal_output("Discovered router at %s!" % router_ip, "#00FFFF")
				else:
					ui.append_terminal_output("Router %s already known." % router_ip, "#FFFF00")
		_:
			ui.append_terminal_output("Usage: router [list|discover]", "#FFFF00")

func _generate_area_nodes(center_ip: String) -> Array:
	var octets = center_ip.split(".")
	var base = int(octets[0])
	var o2 = int(octets[1])
	var o3 = int(octets[2])
	var o4 = int(octets[3])
	var is_local = center_ip.begins_with("127.")
	var nodes = []
	var rng = randi()
	for dx in range(-2, 3):
		for dy in range(-2, 3):
			var nx = o3 + dx
			var ny = o4 + dy
			if nx < 0 or nx > 255 or ny < 0 or ny > 255: continue
			var ip = "%d.%d.%d.%d" % [base, o2, nx, ny]
			if ip == center_ip:
				nodes.append({"ip": ip, "type": "empty", "owner": ""})
				continue
			var seed_val = base * 1000000 + o2 * 10000 + nx * 100 + ny
			var seeded = (seed_val * 1103515245 + 12345) % 2147483648
			var type_idx = seeded % 12
			var node = {"ip": ip, "owner": ""}
			match type_idx:
				0, 1: node.type = "empty"
				2: node.type = "router"
				3: node.type = "crypto_mine"
				4: node.type = "data_center"
				5, 6: node.type = "virus"
				7: node.type = "worm"
				8: node.type = "devourer"
				9: node.type = "ai_overlord" if is_local else "firewall"
				10: node.type = "firewall" if is_local else "empty"
				11: node.type = "empty"
			if base >= 224 and type_idx < 9:
				var danger_roll = seeded % 4
				if danger_roll == 0: node.type = "devourer"
				elif danger_roll == 1: node.type = "ai_overlord"
			if is_local and node.type == "devourer": node.type = "empty"
			if is_local and node.type == "ai_overlord": node.type = "data_center"
			nodes.append(node)
	return nodes

func _generate_node_at(ip: String) -> Dictionary:
	var octets = ip.split(".")
	var base = int(octets[0])
	var o2 = int(octets[1])
	var nx = int(octets[2])
	var ny = int(octets[3])
	var seed_val = base * 1000000 + o2 * 10000 + nx * 100 + ny
	var seeded = (seed_val * 1103515245 + 12345) % 2147483648
	var type_idx = seeded % 8
	var node = {"ip": ip, "owner": ""}
	match type_idx:
		0, 1, 2: node.type = "empty"
		3: node.type = "crypto_mine"
		4: node.type = "data_center"
		5: node.type = "virus"
		6: node.type = "worm"
		7: node.type = "router"
	if ip.begins_with("127."):
		if type_idx == 6 or type_idx == 7: node.type = "empty"
	return node

func _ip_distance(ip1: String, ip2: String) -> int:
	var o1 = ip1.split(".")
	var o2 = ip2.split(".")
	if o1.size() != 4 or o2.size() != 4: return 999999
	var v1 = (int(o1[0]) << 24) | (int(o1[1]) << 16) | (int(o1[2]) << 8) | int(o1[3])
	var v2 = (int(o2[0]) << 24) | (int(o2[1]) << 16) | (int(o2[2]) << 8) | int(o2[3])
	return abs(v1 - v2)

func _process_game_events(events: Variant) -> void:
	if typeof(events) != TYPE_ARRAY:
		events = [events]
	for event in events:
		if typeof(event) == TYPE_DICTIONARY:
			var type = event.get("type", "")
			match type:
				"level_up":
					ZeroDayCore.level_up.emit(event.get("new_level", 1), event.get("is_breakthrough", false))
				"achievement":
					ZeroDayCore.add_notification("ACHIEVEMENT", event.get("title", ""), event.get("description", ""))

func _process_notifications(notifications: Variant) -> void:
	if typeof(notifications) != TYPE_ARRAY:
		notifications = [notifications]
	for n in notifications:
		if typeof(n) == TYPE_DICTIONARY:
			ZeroDayCore.add_notification(
				n.get("type", "INFO"),
				n.get("title", "Notification"),
				n.get("description", "")
			)

func _player_to_dict(player: ZeroDayData.PlayerData) -> Dictionary:
	var d: Dictionary = {}
	if player == null:
		return d
	d["id"] = player.id
	d["username"] = player.username
	d["level"] = player.level
	d["experience"] = player.experience
	d["experience_to_next"] = player.experience_to_next
	d["cpu"] = player.cpu
	d["max_cpu"] = player.max_cpu
	d["ram"] = player.ram
	d["max_ram"] = player.max_ram
	d["bandwidth"] = player.bandwidth
	d["max_bandwidth"] = player.max_bandwidth
	d["credits"] = player.credits
	d["reputation"] = player.reputation
	d["prestige_level"] = player.prestige_level
	d["prestige_points"] = player.prestige_points
	d["unlocked_commands"] = player.unlocked_commands
	d["discovered_nodes_count"] = player.discovered_nodes_count
	d["completed_tasks_count"] = player.completed_tasks_count
	d["current_storyline"] = player.current_storyline
	d["storyline_progress"] = player.storyline_progress
	d["faction_id"] = player.faction_id
	d["active_zero_day_exploits"] = player.active_zero_day_exploits
	d["firewall_boost"] = player.firewall_boost
	d["world_event_participation"] = player.world_event_participation
	d["unlocked_achievement_count"] = player.unlocked_achievement_count
	d["skill_points"] = player.skill_points
	d["unlocked_skill_count"] = player.unlocked_skill_count
	d["unread_notifications"] = player.unread_notifications
	d["active_challenge_count"] = player.active_challenge_count
	d["career_path"] = player.career_path
	d["heat_level"] = player.heat_level
	d["notoriety"] = player.notoriety
	d["justice_points"] = player.justice_points
	d["bounty_price"] = player.bounty_price
	d["current_zone_id"] = player.current_zone_id
	d["current_ip"] = player.current_ip
	d["breakthrough_multiplier"] = player.breakthrough_multiplier
	d["login_streak"] = player.login_streak
	d["total_logins"] = player.total_logins
	return d
