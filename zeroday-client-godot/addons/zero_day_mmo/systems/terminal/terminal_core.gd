extends PanelContainer

@export var input_field: LineEdit
@export var output_text: RichTextLabel
@export var scroll_rect: ScrollContainer
@export var max_lines: int = 500

@export var default_color: Color = Color(0.6, 1.0, 0.6, 1.0)
@export var success_color: Color = Color(0.5, 1.0, 0.5, 1.0)
@export var error_color: Color = Color(1.0, 0.4, 0.4, 1.0)
@export var info_color: Color = Color(0.5, 0.8, 1.0, 1.0)
@export var warning_color: Color = Color(1.0, 0.9, 0.4, 1.0)
@export var prompt_color: Color = Color(0.5, 1.0, 0.5, 1.0)
@export var prompt_symbol: String = "> "
@export var background_color: Color = Color(0.0, 0.1, 0.0, 0.8)

@export var terminal_effects: Node
@export var enable_typewriter: bool = true
@export var typewriter_char_speed: float = 0.02

var _history: Array[String] = []
var _history_index: int = -1
var _output_buffer: String = ""
var _typewriter_queue: Array[String] = []
var _is_typing: bool = false
var _prev_up: bool = false
var _prev_down: bool = false

func _ready():
	if input_field:
		input_field.text_submitted.connect(_on_submit)
		input_field.text_changed.connect(_on_input_changed)
	var boot_anim = get_node_or_null("../TerminalBootAnimation")
	if not boot_anim or not boot_anim.get("play_on_start"):
		_show_splash()

func _show_splash():
	_append_raw("[color=#00FF00]╔══════════════════════════════════════╗\n║       ZERODAY TERMINAL v1.0         ║\n║      MMORPG Hacking Simulator       ║\n╠══════════════════════════════════════╣\n║  Connect to server to begin...      ║\n╚══════════════════════════════════════╝[/color]")
	_append_raw("[color=#cccccc]Type 'help' to see available commands.[/color]")

func _on_submit(text: String):
	if text.is_empty(): return
	var command = text.strip_edges()
	_history.append(command)
	_history_index = _history.size()
	_append_raw("[color=#cccccc]%s%s[/color]" % [prompt_symbol, command])
	input_field.text = ""
	GameManager.execute_command(command)
	_scroll_to_bottom()
	if input_field:
		input_field.grab_focus()

func _on_input_changed(text: String):
	if text.length() == 1 and text.strip_edges() != "":
		var ac = GameManager.ui.terminal_core
		pass

func append_output(text: String, color_hex: String = "#cccccc"):
	var colored = "[color=%s]%s[/color]" % [color_hex, _escape(text)]
	if enable_typewriter:
		_typewriter_queue.append(colored)
		if not _is_typing:
			_process_typewriter()
	else:
		_output_buffer += colored + "\n"
		if output_text:
			output_text.text = _output_buffer
			_trim_lines()
		_scroll_to_bottom()

func _append_raw(text: String):
	_output_buffer += text + "\n"
	if output_text:
		output_text.text = _output_buffer
		_trim_lines()
	_scroll_to_bottom()

func _process_typewriter():
	_is_typing = true
	while _typewriter_queue.size() > 0:
		var line = _typewriter_queue.pop_front()
		var content = line
		var color_tag = ""
		if line.begins_with("[color="):
			var end_tag = line.find("]", 0)
			if end_tag >= 0:
				color_tag = line.substr(0, end_tag + 1)
				content = line.substr(end_tag + 1).replace("[/color]", "")
		for i in range(content.length() + 1):
			var partial = content.substr(0, i)
			var colored = color_tag + partial + "[/color]\n"
			_output_buffer += colored + "\n"
			_output_buffer = _output_buffer.substr(0, _output_buffer.length() - colored.length())
			_output_buffer += colored
			if output_text:
				output_text.text = _output_buffer
			_trim_lines()
			_scroll_to_bottom()
			await get_tree().create_timer(typewriter_char_speed).timeout
	_is_typing = false

func _escape(text: String) -> String:
	return text.replace("[", "[").replace("]", "]")

func _trim_lines():
	var full = output_text.text if output_text else _output_buffer
	var lines = full.split("\n")
	if lines.size() > max_lines:
		var trimmed = lines.slice(lines.size() - max_lines).join("\n")
		if output_text: output_text.text = trimmed
		_output_buffer = trimmed

func _scroll_to_bottom():
	await get_tree().process_frame
	if scroll_rect:
		var v_scroll = scroll_rect.get_v_scroll_bar()
		if v_scroll:
			scroll_rect.scroll_vertical = int(v_scroll.max_value)

func clear():
	_output_buffer = ""
	if output_text: output_text.text = ""
	_typewriter_queue.clear()
	_is_typing = false

func focus_input():
	if input_field: input_field.grab_focus()

func _process(delta: float):
	var cur_up = Input.is_physical_key_pressed(KEY_UP)
	var cur_down = Input.is_physical_key_pressed(KEY_DOWN)
	if cur_up and not _prev_up and _history.size() > 0:
		_history_index = maxi(0, _history_index - 1)
		if input_field: input_field.text = _history[_history_index]
		input_field.caret_column = input_field.text.length()
	elif cur_down and not _prev_down and _history.size() > 0:
		_history_index = mini(_history.size(), _history_index + 1)
		if input_field: input_field.text = "" if _history_index >= _history.size() else _history[_history_index]
		input_field.caret_column = input_field.text.length()
	_prev_up = cur_up
	_prev_down = cur_down
	if Input.is_physical_key_pressed(KEY_CTRL) and Input.is_physical_key_pressed(KEY_L):
		clear()
