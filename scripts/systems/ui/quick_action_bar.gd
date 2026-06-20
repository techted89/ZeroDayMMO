extends HBoxContainer

var _buttons: Dictionary = {}
var _cooldowns: Dictionary = {}

signal action_triggered(action_id: String)

const ACTIONS = [
	{"id": "scan", "label": "SCAN", "shortcut": "1", "cooldown": 2.0},
	{"id": "attack", "label": "ATTACK", "shortcut": "2", "cooldown": 1.5},
	{"id": "defend", "label": "DEFEND", "shortcut": "3", "cooldown": 3.0},
	{"id": "stealth", "label": "STEALTH", "shortcut": "4", "cooldown": 5.0},
	{"id": "heal", "label": "HEAL", "shortcut": "5", "cooldown": 10.0},
	{"id": "inventory", "label": "INV", "shortcut": "6", "cooldown": 0.0},
]

func _ready():
	_setup_buttons()

func _setup_buttons():
	for action in ACTIONS:
		var btn = Button.new()
		btn.text = action.label + "\n[" + action.shortcut + "]"
		btn.flat = true
		btn.custom_minimum_size = Vector2(80, 50)
		btn.size_flags_horizontal = Control.SIZE_SHRINK_CENTER
		btn.pressed.connect(_on_action_pressed.bind(action.id))
		btn.tooltip_text = action.label + " (" + action.shortcut + ")"
		
		_buttons[action.id] = {
			"button": btn,
			"cooldown": action.cooldown,
			"data": action
		}
		add_child(btn)

func _on_action_pressed(action_id: String):
	if _cooldowns.has(action_id):
		return
	
	var action = _buttons.get(action_id)
	if not action:
		return
	
	action_triggered.emit(action_id)
	
	if action.cooldown > 0:
		_start_cooldown(action_id, action)

func _start_cooldown(action_id: String, action: Dictionary):
	_cooldowns[action_id] = true
	var btn = action.button as Button
	if btn:
		btn.disabled = true
		btn.modulate = Color(0.4, 0.4, 0.4, 0.6)
	
	get_tree().create_timer(action.cooldown).timeout.connect(func():
		_cooldowns.erase(action_id)
		if is_instance_valid(btn):
			btn.disabled = false
			btn.modulate = Color(1, 1, 1, 1)
	)

func set_action_enabled(action_id: String, enabled: bool):
	var action = _buttons.get(action_id)
	if action:
		action.button.disabled = not enabled
		action.button.modulate = Color(1, 1, 1, 1) if enabled else Color(0.4, 0.4, 0.4, 0.6)

func set_action_visible(action_id: String, visible: bool):
	var action = _buttons.get(action_id)
	if action:
		action.button.visible = visible

func reset_cooldowns():
	_cooldowns.clear()
	for action_id in _buttons:
		var btn = _buttons[action_id].button
		btn.disabled = false
		btn.modulate = Color(1, 1, 1, 1)
