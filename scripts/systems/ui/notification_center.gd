extends PanelContainer

var _notifications: Array[Dictionary] = []
var _container: VBoxContainer = null

const MAX_NOTIFICATIONS: int = 50
const NOTIFICATION_DURATION: float = 8.0

func _ready():
	_container = VBoxContainer.new()
	_container.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	add_child(_container)
	
	UIManager.get_event_system().notification_received.connect(_on_notification_received)

func add_notification(title: String, message: String, notif_type: String = "info"):
	var entry = {
		"id": "notif_" + str(Time.get_ticks_usec()),
		"title": title,
		"message": message,
		"type": notif_type,
		"timestamp": Time.get_unix_time_from_system(),
		"read": false
	}
	_notifications.append(entry)
	
	if _notifications.size() > MAX_NOTIFICATIONS:
		_notifications.pop_front()
	
	_create_notification_ui(entry)
	
	if visible:
		get_tree().create_timer(NOTIFICATION_DURATION).timeout.connect(_auto_remove.bind(entry.id))

func _on_notification_received(title: String, message: String, notif_type: String):
	add_notification(title, message, notif_type)

func _create_notification_ui(entry: Dictionary):
	if not _container:
		return
	
	var panel = PanelContainer.new()
	var hbox = HBoxContainer.new()
	var text_vbox = VBoxContainer.new()
	var title_label = RichTextLabel.new()
	var msg_label = RichTextLabel.new()
	
	title_label.bbcode_enabled = true
	title_label.text = "[b]" + entry.title + "[/b]"
	title_label.add_theme_font_size_override("normal_font_size", 14)
	
	msg_label.bbcode_enabled = true
	msg_label.text = entry.message
	msg_label.add_theme_font_size_override("normal_font_size", 12)
	msg_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	
	var style = StyleBoxFlat.new()
	match entry.type:
		"success":
			style.bg_color = Color(0, 0.15, 0, 0.9)
			style.border_color = Color(0, 1, 0.3, 0.7)
		"error":
			style.bg_color = Color(0.15, 0, 0, 0.9)
			style.border_color = Color(1, 0.2, 0.1, 0.7)
		"warning":
			style.bg_color = Color(0.15, 0.1, 0, 0.9)
			style.border_color = Color(1, 0.8, 0, 0.7)
		_: # info
			style.bg_color = Color(0, 0.05, 0.1, 0.9)
			style.border_color = Color(0.2, 0.6, 1, 0.7)
	
	style.border_width_left = 2
	style.content_margin_left = 8
	style.content_margin_right = 8
	style.content_margin_top = 4
	style.content_margin_bottom = 4
	panel.add_theme_stylebox_override("panel", style)
	
	text_vbox.add_child(title_label)
	text_vbox.add_child(msg_label)
	hbox.add_child(text_vbox)
	panel.add_child(hbox)
	
	panel.meta_id = entry.id
	_container.add_child(panel)
	
	get_tree().create_timer(NOTIFICATION_DURATION).timeout.connect(func():
		if is_instance_valid(panel):
			panel.queue_free()
	)

func _auto_remove(notif_id: String):
	for i in _notifications.size():
		if _notifications[i].id == notif_id:
			_notifications.remove_at(i)
			break

func mark_read(notif_id: String):
	for notif in _notifications:
		if notif.id == notif_id:
			notif.read = true
			break

func get_unread_count() -> int:
	var count = 0
	for n in _notifications:
		if not n.read:
			count += 1
	return count

func get_all() -> Array[Dictionary]:
	return _notifications.duplicate()

func clear():
	_notifications.clear()
	if _container:
		for child in _container.get_children():
			child.queue_free()
