extends Node

var _damage_labels: Array[Label] = []
var _hit_flash_nodes: Array[Node] = []

func play_hit_effect(target: Control, damage: int, is_critical: bool = false):
	var label = Label.new()
	label.text = str(damage)
	if is_critical:
		label.add_theme_color_override("font_color", Color(1, 0, 0.3, 1))
		label.add_theme_font_size_override("font_size", 28)
	else:
		label.add_theme_color_override("font_color", Color(1, 0.5, 0, 1))
		label.add_theme_font_size_override("font_size", 20)
	
	label.position = target.global_position + Vector2(randf_range(-20, 20), 0)
	add_child(label)
	_damage_labels.append(label)
	
	var tween = create_tween().set_parallel(true)
	tween.tween_property(label, "position:y", label.position.y - 60, 1.0).set_ease(Tween.EASE_OUT)
	tween.tween_property(label, "modulate:a", 0.0, 0.8).set_delay(0.2)
	tween.finished.connect(_remove_damage_label.bind(label))

func _remove_damage_label(label: Label):
	if is_instance_valid(label):
		label.queue_free()
	_damage_labels.erase(label)

func play_heal_effect(target: Control, amount: int):
	var label = Label.new()
	label.text = "+" + str(amount)
	label.add_theme_color_override("font_color", Color(0, 1, 0.3, 1))
	label.add_theme_font_size_override("font_size", 22)
	label.position = target.global_position + Vector2(randf_range(-15, 15), -10)
	add_child(label)
	_damage_labels.append(label)
	
	var tween = create_tween().set_parallel(true)
	tween.tween_property(label, "position:y", label.position.y - 50, 0.8).set_ease(Tween.EASE_OUT)
	tween.tween_property(label, "modulate:a", 0.0, 0.6).set_delay(0.3)
	tween.finished.connect(_remove_damage_label.bind(label))

func play_screen_shake(intensity: float = 5.0, duration: float = 0.2):
	var camera = get_viewport().get_camera_2d()
	if not camera:
		return
	
	var original_pos = camera.position
	var tween = create_tween()
	tween.set_ease(Tween.EASE_OUT)
	tween.tween_method(_apply_shake.bind(original_pos), intensity, 0.0, duration)

func _apply_shake(intensity: float, original_pos: Vector2):
	var camera = get_viewport().get_camera_2d()
	if not camera:
		return
	camera.position = original_pos + Vector2(randf_range(-intensity, intensity), randf_range(-intensity, intensity))

func play_flash(node: CanvasItem, color: Color = Color(1, 0, 0, 0.3), duration: float = 0.15):
	var original_modulate = node.modulate
	
	var tween = create_tween().set_parallel(true)
	tween.tween_property(node, "modulate", color, duration * 0.3)
	tween.tween_property(node, "modulate", original_modulate, duration * 0.7).set_delay(duration * 0.3)

func clear():
	for label in _damage_labels:
		if is_instance_valid(label):
			label.queue_free()
	_damage_labels.clear()
