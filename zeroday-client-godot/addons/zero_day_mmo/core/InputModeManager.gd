extends Node

# Manages input mode transitions between mobile and desktop

@export var is_mobile_mode: bool = false

var _mobile_input_handler: MobileInputHandler
var _desktop_input_handler: InputMap

func _ready():
    _initialize_input_handlers()
    _switch_input_mode(is_mobile_mode)

func _initialize_input_handlers():
    _mobile_input_handler = MobileInputHandler.new()
    _desktop_input_handler = InputMap.new()

func _switch_input_mode(mobile: bool):
    if mobile:
        Input.set_input_map(_mobile_input_handler)
    else:
        Input.set_input_map(_desktop_input_handler)

func set_mobile_mode(enabled: bool):
    is_mobile_mode = enabled
    _switch_input_mode(enabled)
    emit_signal("mobile_mode_changed")