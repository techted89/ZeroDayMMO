extends Node

func fade_in(item: CanvasItem, duration: float, delay: float = 0.0, callback: Callable = Callable()):
	if not item: return
	item.modulate.a = 0.0
	if delay > 0: await get_tree().create_timer(delay).timeout
	var tween = create_tween()
	tween.tween_property(item, "modulate:a", 1.0, duration)
	await tween.finished
	if callback: callback.call()

func fade_out(item: CanvasItem, duration: float, delay: float = 0.0, callback: Callable = Callable()):
	if not item: return
	if delay > 0: await get_tree().create_timer(delay).timeout
	var tween = create_tween()
	tween.tween_property(item, "modulate:a", 0.0, duration)
	await tween.finished
	if callback: callback.call()

func slide_in(control: Control, from: Vector2, duration: float, delay: float = 0.0, callback: Callable = Callable()):
	if not control: return
	var target = control.position
	control.position = from
	control.modulate.a = 0.0
	if delay > 0: await get_tree().create_timer(delay).timeout
	var tween = create_tween()
	tween.tween_property(control, "position", target, duration)
	tween.set_parallel()
	tween.tween_property(control, "modulate:a", 1.0, duration)
	await tween.finished
	if callback: callback.call()

func slide_out(control: Control, to: Vector2, duration: float, delay: float = 0.0, callback: Callable = Callable()):
	if not control: return
	if delay > 0: await get_tree().create_timer(delay).timeout
	var tween = create_tween()
	tween.tween_property(control, "position", to, duration)
	tween.set_parallel()
	tween.tween_property(control, "modulate:a", 0.0, duration)
	await tween.finished
	if callback: callback.call()

func scale_in(control: Control, from: float, duration: float, delay: float = 0.0, callback: Callable = Callable()):
	if not control: return
	control.scale = Vector2(from, from)
	if delay > 0: await get_tree().create_timer(delay).timeout
	var tween = create_tween()
	tween.tween_property(control, "scale", Vector2.ONE, duration)
	await tween.finished
	if callback: callback.call()

func scale_out(control: Control, to: float, duration: float, delay: float = 0.0, callback: Callable = Callable()):
	if not control: return
	if delay > 0: await get_tree().create_timer(delay).timeout
	var tween = create_tween()
	tween.tween_property(control, "scale", Vector2(to, to), duration)
	await tween.finished
	if callback: callback.call()

func pulse(control: Control, amplitude: float, duration: float, loop: bool = true, callback: Callable = Callable()):
	if not control: return
	var base = control.scale
	while true:
		var tween = create_tween()
		tween.tween_method(func(t): control.scale = base * (1.0 + amplitude * sin(t * PI)), 0.1, duration)
		await tween.finished
		if not loop: break
	control.scale = base
	if callback: callback.call()

func animate_number(text: RichTextLabel, from_v: float, to_v: float, duration: float, callback: Callable = Callable()):
	if not text: return
	var tween = create_tween()
	tween.tween_method(func(t): text.text = str(int(lerpf(from_v, to_v, t))), from_v, to_v, duration)
	await tween.finished
	text.text = str(int(to_v))
	if callback: callback.call()

func flash_color(item: CanvasItem, flash_color: Color, duration: float, flash_count: int = 3, callback: Callable = Callable()):
	if not item: return
	var original = item.self_modulate
	var half = duration / (flash_count * 2.0)
	for i in range(flash_count):
		var t1 = create_tween()
		t1.tween_property(item, "self_modulate", flash_color, half)
		await t1.finished
		var t2 = create_tween()
		t2.tween_property(item, "self_modulate", original, half)
		await t2.finished
	item.self_modulate = original
	if callback: callback.call()
