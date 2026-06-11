extends Node

var _is_animating: bool = false
var _output: RichTextLabel = null
var _pending_lines: Array[String] = []
var _current_index: int = 0
var _char_index: int = 0
var _timer: Timer = null

signal animation_complete

func _ready():
	_timer = Timer.new()
	_timer.timeout.connect(_on_tick)
	_timer.one_shot = false
	add_child(_timer)

func animate_text(output_label: RichTextLabel, lines: Array[String], speed: float = 0.02):
	if _is_animating:
		finish_instant()
	
	_is_animating = true
	_output = output_label
	_pending_lines = lines.duplicate()
	_current_index = 0
	_char_index = 0
	
	_timer.wait_time = speed
	_timer.start()

func _on_tick():
	if not _output or _current_index >= _pending_lines.size():
		_timer.stop()
		_is_animating = false
		animation_complete.emit()
		return
	
	var full_line = _pending_lines[_current_index]
	
	if _char_index >= full_line.length():
		if _output.text.length() > 0:
			_output.text += "\n"
		_output.text += full_line
		_current_index += 1
		_char_index = 0
		_update_scroll()
		return
	
	var to_show = full_line.substr(0, _char_index + 1)
	var lines_before = _get_existing_lines()
	var rebuild = ""
	for i in range(lines_before.size()):
		if i > 0:
			rebuild += "\n"
		rebuild += lines_before[i]
	
	if _current_index > 0:
		if rebuild.length() > 0:
			rebuild += "\n"
		if _current_index > 1:
			rebuild += lines_before.slice(1, _current_index).join("\n")
	
	rebuild += to_show
	_output.text = rebuild + "_"
	_char_index += 1
	_update_scroll()

func _get_existing_lines() -> Array[String]:
	if not _output:
		return []
	return _output.text.split("\n", false)

func _update_scroll():
	var scroll = _output.get_parent() as ScrollContainer
	if scroll:
		scroll.scroll_vertical = int(scroll.get_v_scroll_bar().max_value)

func finish_instant():
	_timer.stop()
	_is_animating = false
	if _output:
		var remaining = _pending_lines.slice(_current_index)
		for line in remaining:
			if _output.text.length() > 0:
				_output.text += "\n"
			_output.text += line
	_pending_lines.clear()
	_current_index = 0
	_char_index = 0
	animation_complete.emit()

func is_animating() -> bool:
	return _is_animating

func clear():
	if _is_animating:
		finish_instant()
	if _output:
		_output.text = ""
	_pending_lines.clear()
