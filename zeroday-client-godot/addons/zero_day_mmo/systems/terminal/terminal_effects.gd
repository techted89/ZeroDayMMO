extends Node

@export var pulse_duration: float = 0.5
@export var fade_duration: float = 0.3

func pulse_alpha(control: CanvasItem, target_alpha: float = 0.0):
	if not control: return
	var tween = create_tween()
	tween.tween_property(control, "modulate:a", target_alpha, pulse_duration)

func fade_in(control: CanvasItem, duration: float = -1.0):
	if not control: return
	var d = duration if duration > 0 else fade_duration
	control.modulate.a = 0.0
	var tween = create_tween()
	tween.tween_property(control, "modulate:a", 1.0, d)

func fade_out(control: CanvasItem, duration: float = -1.0):
	if not control: return
	var d = duration if duration > 0 else fade_duration
	var tween = create_tween()
	tween.tween_property(control, "modulate:a", 0.0, d)

func shake_control(control: CanvasItem, intensity: float = 5.0, duration: float = 0.3):
	if not control: return
	var original = control.position
	var elapsed = 0.0
	while elapsed < duration:
		var dt = get_process_delta_time()
		elapsed += dt
		var decay = 1.0 - (elapsed / duration)
		var offset = Vector2(randf_range(-1.0, 1.0), randf_range(-1.0, 1.0)) * intensity * decay
		control.position = original + offset
		await get_tree().process_frame
	control.position = original
