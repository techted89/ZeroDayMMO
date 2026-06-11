extends Node

var _resource_bars: Dictionary = {}
var _warning_labels: Dictionary = {}
var _flash_tweens: Dictionary = {}

signal resource_critical(resource_type: String, current: float)
signal resource_recovered(resource_type: String)

func register_bar(resource_type: String, bar: ProgressBar):
	_resource_bars[resource_type] = bar
	
	var warning = Label.new()
	warning.text = _get_warning_text(resource_type)
	warning.add_theme_color_override("font_color", Color(1, 0.2, 0.1, 1))
	warning.add_theme_font_size_override("font_size", 14)
	warning.hide()
	
	bar.add_child(warning)
	warning.position = Vector2(bar.size.x - 120, -2)
	_warning_labels[resource_type] = warning
	
	bar.resized.connect(_on_bar_resized.bind(resource_type, bar, warning))

func _on_bar_resized(resource_type: String, bar: ProgressBar, warning: Label):
	if is_instance_valid(bar) and is_instance_valid(warning):
		warning.position = Vector2(bar.size.x - 120, -2)

func update_resource(resource_type: String, current: float, max_value: float):
	if not _resource_bars.has(resource_type):
		return
	
	var bar = _resource_bars[resource_type]
	var warning = _warning_labels[resource_type]
	var ratio = current / max_value if max_value > 0 else 0.0
	
	bar.value = current
	bar.max_value = max_value
	
	if ratio <= 0.15:
		if bar.value > 0:
			warning.text = _get_warning_text(resource_type)
			warning.show()
			_start_flash(resource_type, bar)
			resource_critical.emit(resource_type, current)
		else:
			warning.text = "DEPLETED"
			warning.show()
			bar.modulate = Color(1, 0.1, 0.1, 1)
			resource_critical.emit(resource_type, current)
	else:
		warning.hide()
		if _flash_tweens.has(resource_type):
			_flash_tweens[resource_type].kill()
			_flash_tweens.erase(resource_type)
		bar.modulate = Color(1, 1, 1, 1)
		if ratio > 0.5:
			resource_recovered.emit(resource_type)

func _start_flash(resource_type: String, bar: ProgressBar):
	if _flash_tweens.has(resource_type):
		return
	
	var tween = create_tween().set_loops()
	tween.tween_property(bar, "modulate", Color(1, 0.3, 0.3, 1), 0.3)
	tween.tween_property(bar, "modulate", Color(1, 1, 1, 1), 0.3)
	_flash_tweens[resource_type] = tween
	tween.finished.connect(_on_flash_finished.bind(resource_type, tween))

func _on_flash_finished(resource_type: String, tween: Tween):
	if _flash_tweens.has(resource_type) and _flash_tweens[resource_type] == tween:
		_flash_tweens.erase(resource_type)

func _get_warning_text(resource_type: String) -> String:
	match resource_type:
		"cpu": return "CPU CRITICAL"
		"ram": return "RAM LOW"
		"bandwidth": return "BW LOW"
		"hp": return "HP CRITICAL"
		_: return "LOW"
