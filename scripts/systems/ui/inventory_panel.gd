extends Control

@export var slot_count: int = 9
@export var item_size: Vector2 = Vector2(100, 100)

signal item_equipped(item: Dictionary)
signal item_used(item: Dictionary)

var _inventory: Array[Dictionary] = []
var _equipped_item: Dictionary = {}
var _slots: Array[PanelContainer] = []

func _ready():
	_setup_ui()

func _setup_ui():
	for i in range(slot_count):
		var slot = PanelContainer.new()
		slot.custom_minimum_size = item_size
		slot.position = Vector2(i * (item_size.x + 10), 10)
		slot.size = item_size
		add_child(slot)

		var vbox = VBoxContainer.new()
		slot.add_child(vbox)

		var icon = TextureRect.new()
		icon.name = "Icon"
		icon.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
		icon.custom_minimum_size = Vector2(item_size.x, item_size.y * 0.6)
		vbox.add_child(icon)

		var label = Label.new()
		label.name = "Label"
		label.text = "Empty"
		label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		vbox.add_child(label)

		_slots.append(slot)

func add_item(item: Dictionary) -> void:
	_inventory.append(item)
	var idx = _inventory.size() - 1
	_update_slot(item, idx)

func remove_item(index: int) -> void:
	if index >= 0 and index < _inventory.size():
		_inventory.remove_at(index)
		_clear_slot(index)

func equip_item(index: int) -> void:
	if index >= 0 and index < _inventory.size():
		var item = _inventory[index]
		_inventory.remove_at(index)
		_equipped_item = item
		item_equipped.emit(item)

func use_item(index: int) -> void:
	if index >= 0 and index < _inventory.size():
		var item = _inventory[index]
		_inventory.remove_at(index)
		item_used.emit(item)

func _update_slot(item: Dictionary, index: int) -> void:
	if index < 0 or index >= _slots.size():
		return
	var slot = _slots[index]
	var icon = slot.get_node_or_null("Icon") as TextureRect
	var label = slot.get_node_or_null("Label") as Label
	if label:
		label.text = item.get("name", "Item")
	if icon:
		var tex_path = "res://assets/sprites/items/%s.png" % item.get("id", "default")
		if ResourceLoader.exists(tex_path):
			icon.texture = load(tex_path)
		else:
			icon.texture = null

func _clear_slot(index: int) -> void:
	if index < 0 or index >= _slots.size():
		return
	var slot = _slots[index]
	var icon = slot.get_node_or_null("Icon") as TextureRect
	var label = slot.get_node_or_null("Label") as Label
	if label:
		label.text = "Empty"
	if icon:
		icon.texture = null

func get_inventory() -> Array[Dictionary]:
	return _inventory.duplicate()
