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
	ui.append_terminal_output("Welcome back, %s!" % player_data.get("username", "h4ck3r"), "#00FF00")
	ui.append_terminal_output("You are level %d. Type 'help' to see all commands." % player_data.get("level", 1), "#cccccc")

func _on_register_success(player_data: Dictionary) -> void:
	ZeroDayCore.game_state = ZeroDayData.GameState.TERMINAL
	ui.show_terminal()
	ui.update_hud(player_data)
	ui.append_terminal_output("Account created! Welcome, %s." % player_data.get("username", "h4ck3r"), "#00FF00")
	ui.append_terminal_output("Your journey begins. Type 'help' to see what you can do.", "#cccccc")
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
		_:
			return false
	return true

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
	d["breakthrough_multiplier"] = player.breakthrough_multiplier
	d["login_streak"] = player.login_streak
	d["total_logins"] = player.total_logins
	return d
