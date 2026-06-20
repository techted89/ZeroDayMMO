extends Node

var login_panel: Control
var hud_controller: Node
var terminal_core: Node
var loading_overlay: Control
var notification_center: Node
var world_map_explorer: Control
var combat_visuals: Control

func _ready():
	process_mode = PROCESS_MODE_ALWAYS
	_setup_ui_references()

func _setup_ui_references():
	login_panel = get_node_or_null("/root/Main/Main2D/LoginPanel")
	hud_controller = get_node_or_null("/root/Main/Main2D/HUD")
	terminal_core = get_node_or_null("/root/Main/Main2D/Terminal")
	loading_overlay = get_node_or_null("/root/Main/Main2D/LoadingOverlay")
	notification_center = get_node_or_null("/root/Main/Main2D/NotificationCenter")
	world_map_explorer = get_node_or_null("/root/Main/Main2D/WorldMapExplorer")
	combat_visuals = get_node_or_null("/root/Main/Main2D/CombatVisuals")
	if loading_overlay:
		loading_overlay.hide()
	if login_panel:
		login_panel.hide()
	if hud_controller:
		hud_controller.set("visible", false)
	if world_map_explorer:
		world_map_explorer.hide()
	if combat_visuals:
		combat_visuals.hide()

func show_login_panel() -> void:
	if hud_controller:
		hud_controller.set("visible", false)
	if login_panel:
		login_panel.show()
		login_panel.modulate.a = 0.0
		var tween = create_tween()
		tween.tween_property(login_panel, "modulate:a", 1.0, 0.3)

func show_terminal() -> void:
	if login_panel:
		var tween = create_tween()
		tween.tween_property(login_panel, "modulate:a", 0.0, 0.3)
		tween.tween_callback(func(): login_panel.hide())
	if hud_controller:
		hud_controller.set("visible", true)
	if terminal_core:
		terminal_core.call("focus_input")

func update_hud(player_data: Dictionary) -> void:
	if hud_controller and hud_controller.has_method("update_hud"):
		hud_controller.call("update_hud", player_data)

func append_terminal_output(text: String, color: String = "#ffffff") -> void:
	if terminal_core and terminal_core.has_method("append_output"):
		terminal_core.call("append_output", text, color)

func clear_terminal() -> void:
	if terminal_core and terminal_core.has_method("clear"):
		terminal_core.call("clear")

func get_event_system():
	return get_node("/root/Main/EventSystem")

func show_error(message: String) -> void:
	if login_panel and login_panel.visible:
		if login_panel.has_method("show_error"):
			login_panel.call("show_error", message)
	else:
		append_terminal_output("[ERROR] %s" % message, "#ff4444")

func show_inventory_panel() -> void:
	pass

func show_loading(show: bool) -> void:
	if loading_overlay == null: return
	if show:
		loading_overlay.show()
		loading_overlay.modulate.a = 0.0
		var tween = create_tween()
		tween.tween_property(loading_overlay, "modulate:a", 1.0, 0.3)
	else:
		var tween = create_tween()
		tween.tween_property(loading_overlay, "modulate:a", 0.0, 0.3)
		tween.tween_callback(func(): loading_overlay.hide())

func show_world_map() -> void:
	if world_map_explorer:
		world_map_explorer.show()
		if world_map_explorer.has_method("load_area"):
			var p = ZeroDayCore.current_player
			if p:
				world_map_explorer.call("load_area", p.current_ip)
		world_map_explorer.modulate.a = 0.0
		var tween = create_tween()
		tween.tween_property(world_map_explorer, "modulate:a", 1.0, 0.3)

func hide_world_map() -> void:
	if world_map_explorer:
		var tween = create_tween()
		tween.tween_property(world_map_explorer, "modulate:a", 0.0, 0.3)
		tween.tween_callback(func(): world_map_explorer.hide())

func start_combat_visuals(enemy_type: String, enemy_name: String, enemy_hp: int, player_hp: int) -> void:
	if combat_visuals and combat_visuals.has_method("start_combat"):
		combat_visuals.call("start_combat", enemy_type, enemy_name, enemy_hp, player_hp)

func update_combat_visuals(player_damage: int, enemy_damage: int, player_atk: int, enemy_atk: int, player_def: int, enemy_def: int) -> Dictionary:
	if combat_visuals and combat_visuals.has_method("process_combat_round"):
		return combat_visuals.call("process_combat_round", player_damage, enemy_damage, player_atk, enemy_atk, player_def, enemy_def)
	return {"ended": false}

func hide_combat_visuals() -> void:
	if combat_visuals:
		combat_visuals.hide()
