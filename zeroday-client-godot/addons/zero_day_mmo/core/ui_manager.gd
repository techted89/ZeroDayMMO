extends Node

var login_panel: Control
var hud_controller: Node
var terminal_core: Node
var loading_overlay: Control
var notification_center: Node

func _ready():
	process_mode = PROCESS_MODE_ALWAYS

func register_ui_elements(login: Control, hud: Node, terminal: Node, loading: Control, notifications: Node):
	login_panel = login
	hud_controller = hud
	terminal_core = terminal
	loading_overlay = loading
	notification_center = notifications
	if loading_overlay:
		loading_overlay.hide()
	if login_panel:
		login_panel.hide()
	if hud_controller:
		hud_controller.set("visible", false)

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
