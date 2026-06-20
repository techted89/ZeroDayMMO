extends Node

signal mobile_mode_changed(enabled: bool)

@export var is_mobile_mode: bool = false

func _ready():
	_switch_input_mode(is_mobile_mode)

func _switch_input_mode(mobile: bool):
	if mobile:
		DisplayServer.window_set_mode(DisplayServer.WINDOW_MODE_MAXIMIZED)
		DisplayServer.window_set_flag(DisplayServer.WINDOW_FLAG_BORDERLESS, false)
	else:
		DisplayServer.window_set_mode(DisplayServer.WINDOW_MODE_WINDOWED)
		DisplayServer.window_set_flag(DisplayServer.WINDOW_FLAG_BORDERLESS, true)

func set_mobile_mode(enabled: bool):
	is_mobile_mode = enabled
	_switch_input_mode(enabled)
	mobile_mode_changed.emit(enabled)
