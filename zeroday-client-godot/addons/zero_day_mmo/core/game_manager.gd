extends Node

signal login_result(success: bool, data: Dictionary)
signal register_result(success: bool, data: Dictionary)
signal command_result(success: bool, output: String, data: Dictionary)

var network: NetworkManager
var ui: UIManager
var inventory_panel: InventoryPanel
var _resource_tick_timer: float = 0.0

func _ready():
    process_mode = PROCESS_MODE_ALWAYS
    network = NetworkManager
    ui = UIManager
    
    # Initialize Input Mode Manager
    _input_mode_manager = InputModeManager.new()
    _input_mode_manager.connect("mobile_mode_changed", _on_mobile_mode_changed)
    
    # Initialize InventoryPanel
    _inventory_panel = InventoryPanel.new()
    add_child(_inventory_panel)
    _inventory_panel.connect("item_equipped", _on_inventory_equipped)
    _inventory_panel.connect("item_used", _on_inventory_used)
    
    # Initialize Mobile Input Handler
    _mobile_input_handler = MobileInputHandler.new()
    _mobile_input_handler.connect("attack", _handle_attack)
    _mobile_input_handler.connect("move_right", _move_player)
    _mobile_input_handler.connect("move_left", _move_player)
    _mobile_input_handler.connect("move_up", _move_player)
    _mobile_input_handler.connect("move_down", _move_player)
    
    # Initialize Desktop Input Handler
    _desktop_input_handler = InputMap.new()
    _desktop_input_handler.connect("attack", _handle_attack)
    _desktop_input_handler.connect("ui_left", _move_player)
    _desktop_input_handler.connect("ui_right", _move_player)
    _desktop_input_handler.connect("ui_up", _move_player)
    _desktop_input_handler.connect("ui_down", _move_player)
    
    # Set initial input mode
    _input_mode_manager.set_mobile_mode(false)
    
    _connect_signals()
    
    # Setup 2.5D View Mode
    if Main2D:
        Main2D.connect("pressed", _on_view_mode_pressed")
	process_mode = PROCESS_MODE_ALWAYS
	network = NetworkManager
	ui = UIManager
	_connect_signals()

func _connect_signals():
    network.connected.connect(_on_connected)
    network.connection_failed.connect(_on_connection_failed)
    network.disconnected.connect(_on_disconnected)
    
    # Connect inventory panel signals
    _inventory_panel.connect("item_equipped", _on_inventory_equipped)
    _inventory_panel.connect("item_used", _on_inventory_used)
    
    # Setup 2.5D View Mode
    if Main2D:
        Main2D.connect("pressed", _on_view_mode_pressed)
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
        
    # Apply initial 2.5D view mode
    if Main2D:
        Main2D.set_view_mode("45_degree")
    ZeroDayCore.game_state = ZeroDayData.GameState.LOGIN
    ui.show_loading(false)
    ui.show_login_panel()
    if ZeroDayCore.current_player:
        ui.show_terminal()
        ZeroDayCore.game_state = ZeroDayData.GameState.TERMINAL
        
    # Apply initial 2.5D view mode
    if Main2D:
        Main2D.set_view_mode("45_degree")
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
    
    # Reset view mode on connection failure
    if Main2D:
        Main2D.set_view_mode("45_degree")
	ZeroDayCore.game_state = ZeroDayData.GameState.DISCONNECTED
	ui.show_loading(false)
	ui.show_error(msg)
	ui.append_terminal_output("[ERROR] %s" % msg, "#ff4444")

func _on_disconnected(reason: String) -> void:
    ZeroDayCore.game_state = ZeroDayData.GameState.DISCONNECTED
    ui.append_terminal_output("[DISCONNECTED] %s" % reason, "#ff4444")
    
    # Reset view mode on disconnect
    if Main2D:
        Main2D.set_view_mode("45_degree")
	ZeroDayCore.game_state = ZeroDayData.GameState.DISCONNECTED
	ui.append_terminal_output("[DISCONNECTED] %s" % reason, "#ff4444")

func _process(delta: float) -> void:
    _resource_tick_timer += delta
    if _resource_tick_timer >= 3.0 and ZeroDayCore.current_player:
        _resource_tick_timer = 0.0
        ZeroDayCore.current_player.regenerate_resources()
        var player_dict = _player_to_dict(ZeroDayCore.current_player)
        ui.update_hud(player_dict)
        
    # Handle 2.5D view mode changes
    if Main2D:
        Main2D.process_view_mode()
    _resource_tick_timer += delta
    if _resource_tick_timer >= 3.0 and ZeroDayCore.current_player:
        _resource_tick_timer = 0.0
        ZeroDayCore.current_player.regenerate_resources()
        var player_dict = _player_to_dict(ZeroDayCore.current_player)
        ui.update_hud(player_dict)
        
    # Handle 2.5D view mode changes
    if Main2D:
        Main2D.process_view_mode()
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
    
    # Initialize inventory
    _inventory_panel.add_item("Basic Sword")
    _inventory_panel.add_item("Stealth Cloak")
    
    # Initialize skill tree
    _initialize_skill_tree()
    
    # Show inventory UI
    ui.show_inventory_panel()
    
    # Apply initial 2.5D view mode
    if Main2D:
        Main2D.set_view_mode("45_degree")
    ZeroDayCore.game_state = ZeroDayData.GameState.TERMINAL
    ui.show_terminal()
    ui.update_hud(player_data)
    ui.append_terminal_output("Welcome back, %s!" % player_data.get("username", "h4ck3r"), "#00FF00")
    ui.append_terminal_output("You are level %d. Type 'help' to see all commands." % player_data.get("level", 1), "#cccccc")
    
    # Apply initial 2.5D view mode
    if Main2D:
        Main2D.set_view_mode("45_degree")
	ZeroDayCore.game_state = ZeroDayData.GameState.TERMINAL
	ui.show_terminal()
	ui.update_hud(player_data)
    ui.append_terminal_output("Welcome back, %s!" % player_data.get("username", "h4ck3r"), "#00FF00")
    ui.append_terminal_output("You are level %d. Type 'help' to see all commands." % player_data.get("level", 1), "#cccccc")
    
    # Initialize inventory
    _inventory_panel.add_item("Basic Sword")
    _inventory_panel.add_item("Stealth Cloak")

func _on_register_success(player_data: Dictionary) -> void:
    ZeroDayCore.game_state = ZeroDayData.GameState.TERMINAL
    ui.show_terminal()
    ui.update_hud(player_data)
    ui.append_terminal_output("Account created! Welcome, %s." % player_data.get("username", "h4ck3r"), "#00FF00")
    ui.append_terminal_output("Your journey begins. Type 'help' to see what you can do.", "#cccccc")
    ui.append_terminal_output("Tip: Try 'daily' to claim your first login reward!", "#FFFF00")
    
    # Initialize inventory
    _inventory_panel.add_item("Basic Sword")
    _inventory_panel.add_item("Stealth Cloak")
    
    # Initialize skill tree
    _initialize_skill_tree()
    
    # Show inventory UI
    ui.show_inventory_panel()
    
    # Connect inventory signals
    _inventory_panel.connect("item_equipped", _on_inventory_equipped)
    _inventory_panel.connect("item_used", _on_inventory_used)

func execute_command(input: String) -> void:
	if input.is_empty() or ZeroDayCore.current_player == null: return
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
		var err_msg = result.error
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
        "status":
            var p = ZeroDayCore.current_player
            var s = "[color=#00FF00]╔══════════════════════════════════════╗\n║  PLAYER STATUS                      ║\n╚══════════════════════════════════════╝[/color]\n"
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
        "view_mode":
            if args.size() > 0:
                var mode = args[0].to_lower()
                if Main2D:
                    Main2D.set_view_mode(mode)
        _:
            return false
    return true
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
	if typeof(events) != TYPE_ARRAY: events = [events]
	for event in events:
		if typeof(event) == TYPE_DICTIONARY:
			var type = event.get("type", "")
			match type:
				"level_up":
					ZeroDayCore.level_up.emit(event.get("new_level", 1), event.get("is_breakthrough", false))
				"achievement":
					ZeroDayCore.add_notification("ACHIEVEMENT", event.get("title", ""), event.get("description", ""))

func _process_notifications(notifications: Variant) -> void:
	if typeof(notifications) != TYPE_ARRAY: notifications = [notifications]
	for n in notifications:
		if typeof(n) == TYPE_DICTIONARY:
			ZeroDayCore.add_notification(
				n.get("type", "INFO"),
				n.get("title", "Notification"),
				n.get("description", "")
			)

func _player_to_dict(player: ZeroDayData.PlayerData) -> Dictionary:

func _on_view_mode_pressed(pressed: Button):
    var button_name = pressed.name
    var mode = ""
    
    if button_name == "45Degree":
        mode = "45_degree"
    elif button_name == "Isometric":
        mode = "isometric"
    elif button_name == "TopDown":
        mode = "top_down"
    elif button_name == "FrontSide":
        mode = "front_side"
    elif button_name == "ObliqueY":
        mode = "oblique_y"
    elif button_name == "ObliqueZ":
        mode = "oblique_z"
    
    if Main2D:
        Main2D.set_view_mode(mode)
	var d = {}
	for prop in player.get_property_list():
		if prop.usage & PROPERTY_USAGE_SCRIPT_VARIABLE:
			d[prop.name] = player.get(prop.name)
	return d
