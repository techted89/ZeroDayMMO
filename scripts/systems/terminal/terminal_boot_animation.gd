extends Node

signal boot_complete

var _lines: Array[String] = []
var _current_index: int = 0
var _output: RichTextLabel = null
var _is_running: bool = false

const BOOT_LINES = [
	"[color=#00AA00][ OK  ] Initializing ZeroDay kernel...[/color]",
	"[color=#00AA00][ OK  ] Loading security modules...[/color]",
	"[color=#FFFF00][WARN] Firewall bypass module requires authentication[/color]",
	"[color=#00AA00][ OK  ] Establishing encrypted tunnel...[/color]",
	"[color=#00AA00][ OK  ] Mounting virtual filesystem[/color]",
	"[color=#00FF00][INFO] ZeroDay Terminal v1.0 ready[/color]",
	"[color=#00FF00][INFO] Connected to ZeroDay Network[/color]",
	"",
	"[color=#00FF66]Welcome to ZeroDay MMO[/color]",
	"[color=#00FF66]Type [b]/help[/b] for available commands[/color]",
	"[color=#00FF66]Type [b]/login[/b] to authenticate[/color]",
	"",
]

func play(output_label: RichTextLabel):
	if _is_running:
		return
	_is_running = true
	_output = output_label
	_current_index = 0
	_lines = BOOT_LINES.duplicate()
	_output.text = ""
	_show_next_line()

func _show_next_line():
	if not _is_running or not _output:
		return
	
	if _current_index >= _lines.size():
		_is_running = false
		boot_complete.emit()
		return
	
	if _output.text.length() > 0:
		_output.text += "\n"
	
	_output.text += _lines[_current_index]
	_current_index += 1
	
	var delay = 0.05 if _current_index < 5 else 0.15
	get_tree().create_timer(delay).timeout.connect(_show_next_line)

func skip():
	if not _is_running:
		return
	_is_running = false
	_output.text = ""
	for line in _lines:
		if _output.text.length() > 0:
			_output.text += "\n"
		_output.text += line
	boot_complete.emit()

func is_running() -> bool:
	return _is_running
