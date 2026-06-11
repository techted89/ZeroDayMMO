extends Node

var _panels: Array[Panel] = []

func register_panel(panel: Panel):
	_panels.append(panel)
	_setup_panel(panel)

func _setup_panel(panel: Panel):
	panel.add_theme_stylebox_override("panel", _create_terminal_style())

func _create_terminal_style() -> StyleBoxFlat:
	var style = StyleBoxFlat.new()
	style.bg_color = Color(0, 0.05, 0, 0.85)
	style.border_color = Color(0, 0.6, 0.15, 0.6)
	style.border_width_left = 1
	style.border_width_right = 1
	style.border_width_top = 1
	style.border_width_bottom = 1
	style.corner_radius_top_left = 2
	style.corner_radius_top_right = 2
	style.corner_radius_bottom_left = 2
	style.corner_radius_bottom_right = 2
	style.content_margin_left = 8
	style.content_margin_right = 8
	style.content_margin_top = 4
	style.content_margin_bottom = 4
	return style

func emphasize_panel(panel: Panel, duration: float = 0.5):
	var orig_style = panel.get_theme_stylebox("panel").duplicate()
	var highlight = _create_terminal_style()
	highlight.border_color = Color(0, 1, 0.4, 1.0)
	panel.add_theme_stylebox_override("panel", highlight)
	
	get_tree().create_timer(duration).timeout.connect(func():
		if is_instance_valid(panel):
			panel.add_theme_stylebox_override("panel", orig_style)
	)

func set_error_style(panel: Panel):
	var style = _create_terminal_style()
	style.border_color = Color(1, 0.2, 0.1, 0.8)
	panel.add_theme_stylebox_override("panel", style)

func set_success_style(panel: Panel):
	var style = _create_terminal_style()
	style.border_color = Color(0, 1, 0.3, 0.8)
	panel.add_theme_stylebox_override("panel", style)
