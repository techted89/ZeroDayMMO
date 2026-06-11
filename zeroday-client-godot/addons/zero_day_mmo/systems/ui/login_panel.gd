extends PanelContainer

@export var username_field: LineEdit
@export var password_field: LineEdit
@export var login_button: Button
@export var register_button: Button
@export var error_text: RichTextLabel
@export var panel: Control

func _ready():
	if login_button: login_button.pressed.connect(_on_login)
	if register_button: register_button.pressed.connect(_on_register)
	if username_field: username_field.text_submitted.connect(func(_t): if password_field: password_field.grab_focus())
	if password_field: password_field.text_submitted.connect(func(_t): _on_login())

func _on_login():
	var username = username_field.text.strip_edges() if username_field else ""
	var password = password_field.text if password_field else ""
	if username.is_empty() or password.is_empty():
		show_error("Please enter username and password")
		return
	clear_error()
	GameManager.login(username, password)
	if login_button:
		login_button.disabled = true
		login_button.text = "CONNECTING..."

func _on_register():
	var username = username_field.text.strip_edges() if username_field else ""
	var password = password_field.text if password_field else ""
	if username.is_empty() or password.is_empty():
		show_error("Please enter username and password")
		return
	if password.length() < 4:
		show_error("Password must be at least 4 characters")
		return
	clear_error()
	GameManager.register_player(username, password)
	if register_button:
		register_button.disabled = true
		register_button.text = "REGISTERING..."

func show_error(message: String):
	if error_text:
		error_text.text = message
		error_text.show()
		error_text.modulate.a = 0.0
		var tween = create_tween()
		tween.tween_property(error_text, "modulate", Color.WHITE, 0.3)

func clear_error():
	if error_text:
		var tween = create_tween()
		tween.tween_property(error_text, "modulate", Color(1, 1, 1, 0), 0.2)
		tween.tween_callback(func(): error_text.hide())

func show_panel():
	if panel:
		panel.show()
		panel.scale = Vector2.ZERO
		var tween = create_tween()
		tween.tween_property(panel, "scale", Vector2.ONE, 0.5).set_ease(Tween.EASE_OUT).set_trans(Tween.TRANS_BACK)
	if username_field: username_field.grab_focus()
	if login_button: login_button.disabled = false
	if register_button: register_button.disabled = false
	if login_button: login_button.text = "LOGIN"
	if register_button: register_button.text = "REGISTER"

func hide_panel():
	if panel:
		var tween = create_tween()
		tween.tween_property(panel, "modulate", Color(1, 1, 1, 0), 0.3)
		tween.tween_callback(func(): panel.hide())
