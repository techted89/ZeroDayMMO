extends Node

signal player_updated(player_data: Dictionary)
signal login_successful(player_data: Dictionary)
signal register_successful(player_data: Dictionary)
signal connection_state_changed(state: int)
signal notification_received(type: String, title: String, description: String)
signal level_up(new_level: int, is_breakthrough: bool)
signal command_executed(command: String, success: bool)

var current_player: ZeroDayData.PlayerData = null
var game_state: ZeroDayData.GameState = ZeroDayData.GameState.DISCONNECTED
var settings: ConfigFile = ConfigFile.new()
var settings_path: String = "user://settings.cfg"

func _ready():
	process_mode = PROCESS_MODE_ALWAYS

func save_pref(key: String, value: Variant) -> void:
	settings.set_value("player", key, value)
	settings.save(settings_path)

func get_pref(key: String, default_value: Variant = null) -> Variant:
	settings.load(settings_path)
	return settings.get_value("player", key, default_value)

func update_player(data: Dictionary) -> void:
	if current_player == null:
		current_player = ZeroDayData.PlayerData.new(data)
	else:
		var old_level = current_player.level
		update_player_fields(data)
		if current_player.level > old_level:
			level_up.emit(current_player.level, current_player.level - old_level > 1)
	player_updated.emit(data)

func update_player_fields(data: Dictionary) -> void:
	if current_player == null: return
	for key in data.keys():
		if current_player.has(key):
			current_player.set(key, data[key])

func add_notification(type: String, title: String, description: String) -> void:
	notification_received.emit(type, title, description)
