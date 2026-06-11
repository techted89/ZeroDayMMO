extends Control

@export var player_name_text: RichTextLabel
@export var level_text: RichTextLabel
@export var cpu_slider: ProgressBar
@export var cpu_text: RichTextLabel
@export var ram_slider: ProgressBar
@export var ram_text: RichTextLabel
@export var bandwidth_slider: ProgressBar
@export var bandwidth_text: RichTextLabel
@export var exp_slider: ProgressBar
@export var exp_text: RichTextLabel
@export var credits_text: RichTextLabel
@export var reputation_text: RichTextLabel
@export var commands_text: RichTextLabel
@export var nodes_text: RichTextLabel
@export var active_task_count_text: RichTextLabel
@export var bar_anim_duration: float = 0.3

var _last_data: Dictionary = {}

func update_hud(player_data: Dictionary):
	if _last_data.is_empty():
		_last_data = player_data
		_set_instant(player_data)
		return
	_animate_hud(player_data)
	_last_data = player_data

func _set_instant(p: Dictionary):
	if player_name_text: player_name_text.text = p.get("username", "")
	if level_text: level_text.text = "LVL %d" % p.get("level", 1)
	if exp_slider:
		var exp = p.get("experience", 0)
		var exp_next = p.get("experience_to_next", 100)
		exp_slider.max_value = 100.0
		exp_slider.value = clampi(int(exp / float(maxi(exp_next, 1)) * 100.0), 0, 100)
	if exp_text:
		exp_text.text = "%d / %d XP" % [p.get("experience", 0), p.get("experience_to_next", 100)]
	if cpu_slider:
		cpu_slider.max_value = 100.0
		cpu_slider.value = clampi(int(float(p.get("cpu", 0)) / float(maxi(p.get("max_cpu", 1), 1)) * 100.0), 0, 100)
	if cpu_text: cpu_text.text = "%d/%d MHz" % [p.get("cpu", 0), p.get("max_cpu", 0)]
	if ram_slider:
		ram_slider.max_value = 100.0
		ram_slider.value = clampi(int(float(p.get("ram", 0)) / float(maxi(p.get("max_ram", 1), 1)) * 100.0), 0, 100)
	if ram_text: ram_text.text = "%d/%d MB" % [p.get("ram", 0), p.get("max_ram", 0)]
	if bandwidth_slider:
		bandwidth_slider.max_value = 100.0
		bandwidth_slider.value = clampi(int(float(p.get("bandwidth", 0)) / float(maxi(p.get("max_bandwidth", 1), 1)) * 100.0), 0, 100)
	if bandwidth_text: bandwidth_text.text = "%d/%d Mbps" % [p.get("bandwidth", 0), p.get("max_bandwidth", 0)]
	if credits_text: credits_text.text = "$%d" % p.get("credits", 0)
	if reputation_text: reputation_text.text = str(p.get("reputation", 0))
	if commands_text: commands_text.text = str(p.get("unlocked_commands", []).size())
	if nodes_text: nodes_text.text = str(p.get("completed_tasks_count", 0))
	if active_task_count_text: active_task_count_text.text = "Tasks: %d" % p.get("active_tasks", []).size()

func _animate_hud(p: Dictionary):
	if player_name_text: player_name_text.text = p.get("username", "")
	if level_text: level_text.text = "LVL %d" % p.get("level", 1)
	if exp_slider:
		var exp = p.get("experience", 0)
		var exp_next = p.get("experience_to_next", 100)
		var tween = create_tween()
		tween.tween_property(exp_slider, "value", clampi(int(exp / float(maxi(exp_next, 1)) * 100.0), 0, 100), bar_anim_duration)
	if exp_text:
		exp_text.text = "%d / %d XP" % [p.get("experience", 0), p.get("experience_to_next", 100)]
	if cpu_slider:
		var tween = create_tween()
		tween.tween_property(cpu_slider, "value", clampi(int(float(p.get("cpu", 0)) / float(maxi(p.get("max_cpu", 1), 1)) * 100.0), 0, 100), bar_anim_duration)
	if cpu_text: cpu_text.text = "%d/%d MHz" % [p.get("cpu", 0), p.get("max_cpu", 0)]
	if ram_slider:
		var tween = create_tween()
		tween.tween_property(ram_slider, "value", clampi(int(float(p.get("ram", 0)) / float(maxi(p.get("max_ram", 1), 1)) * 100.0), 0, 100), bar_anim_duration)
	if ram_text: ram_text.text = "%d/%d MB" % [p.get("ram", 0), p.get("max_ram", 0)]
	if bandwidth_slider:
		var tween = create_tween()
		tween.tween_property(bandwidth_slider, "value", clampi(int(float(p.get("bandwidth", 0)) / float(maxi(p.get("max_bandwidth", 1), 1)) * 100.0), 0, 100), bar_anim_duration)
	if bandwidth_text: bandwidth_text.text = "%d/%d Mbps" % [p.get("bandwidth", 0), p.get("max_bandwidth", 0)]
	if credits_text: credits_text.text = "$%d" % p.get("credits", 0)
	if reputation_text: reputation_text.text = str(p.get("reputation", 0))

func show_feedback_text(text: String, color: Color, duration: float = 1.5):
	var label = Label.new()
	label.text = text
	label.add_theme_color_override("font_color", color)
	label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	add_child(label)
	label.position = Vector2(size.x / 2.0 - 50.0, size.y / 2.0)
	var tween = create_tween()
	tween.tween_property(label, "position", label.position + Vector2(0, -40), duration)
	tween.set_parallel()
	tween.tween_property(label, "modulate:a", 0.0, duration)
	tween.tween_callback(func(): label.queue_free())

func set_hud_visible(visible: bool):
	if visible:
		show()
		modulate.a = 0.0
		var tween = create_tween()
		tween.tween_property(self, "modulate", Color.WHITE, 0.5)
	else:
		var tween = create_tween()
		tween.tween_property(self, "modulate", Color(1, 1, 1, 0), 0.3)
		tween.tween_callback(func(): hide())
