extends Node

@export var shake_intensity: float = 5.0
@export var shake_duration: float = 0.3
@export var glitch_overlay: TextureRect
@export var glitch_duration: float = 0.5
@export var flash_overlay: ColorRect
@export var flash_color: Color = Color.WHITE
@export var border_glow: ColorRect
@export var border_color: Color = Color.CYAN

var _original_position: Vector2

func _ready():
	process_mode = PROCESS_MODE_ALWAYS
	if flash_overlay:
		flash_overlay.hide()
		flash_overlay.color = flash_color
	if glitch_overlay: glitch_overlay.hide()
	if border_glow: border_glow.hide()

func shake(custom_intensity: float = -1.0, custom_duration: float = -1.0):
	var intensity = custom_intensity if custom_intensity > 0 else shake_intensity
	var duration = custom_duration if custom_duration > 0 else shake_duration
	_shake_task(intensity, duration)

func _shake_task(intensity: float, duration: float):
	_original_position = position
	var elapsed = 0.0
	while elapsed < duration:
		await get_tree().process_frame
		var dt = get_process_delta_time()
		elapsed += dt
		var decay = 1.0 - (elapsed / duration)
		var offset = Vector2(randf_range(-1.0, 1.0), randf_range(-1.0, 1.0)) * intensity * decay
		position = _original_position + offset
	position = _original_position

func flash(color: Color = Color.WHITE, duration: float = 0.15):
	if not flash_overlay: return
	flash_overlay.show()
	flash_overlay.color = color
	var elapsed = 0.0
	while elapsed < duration:
		await get_tree().process_frame
		var dt = get_process_delta_time()
		elapsed += dt
		color.a = lerpf(0.6, 0.0, elapsed / duration)
		flash_overlay.color = color
	flash_overlay.hide()

func glitch(custom_duration: float = -1.0):
	if not glitch_overlay: return
	var dur = custom_duration if custom_duration > 0 else glitch_duration
	glitch_overlay.show()
	var elapsed = 0.0
	while elapsed < dur:
		await get_tree().process_frame
		var dt = get_process_delta_time()
		elapsed += dt
		glitch_overlay.region_rect = Rect2(Vector2(0.5 + randf_range(-0.1, 0.1), 0.5 + randf_range(-0.1, 0.1)), Vector2.ONE)
		glitch_overlay.modulate.a = randf_range(0.1, 0.4) * (1.0 - elapsed / dur)
		await get_tree().create_timer(0.05).timeout
	glitch_overlay.hide()

func pulse_border(color: Color = Color.CYAN, duration: float = 1.0):
	if not border_glow: return
	border_glow.show()
	border_glow.color = color
	var elapsed = 0.0
	while elapsed < duration:
		await get_tree().process_frame
		var dt = get_process_delta_time()
		elapsed += dt
		var t = elapsed / duration
		color.a = sin(t * PI) * 0.5
		border_glow.color = color
		border_glow.scale = Vector2(1.0 + t * 0.1, 1.0 + t * 0.1)
		await get_tree().process_frame
	border_glow.hide()

func hack_sequence():
	glitch(0.3)
	await get_tree().create_timer(0.1).timeout
	flash(Color(0, 1, 0.2, 0.5), 0.2)
	await get_tree().create_timer(0.1).timeout
	shake(8.0, 0.2)
	await get_tree().create_timer(0.2).timeout
	pulse_border(Color.CYAN, 0.8)
