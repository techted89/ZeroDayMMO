extends Control

# GDScript Inventory Panel
# Supports item management and UI rendering

@export var slot_count: int = 9
@export var item_size: Vector2 = Vector2(100, 100)

signal item_equipped(item: String)
signal item_used(item: String)

var _inventory: Array = []
var _equipped_item: String = ""

# UI Elements
var _slots: Array = []
var _equipped_slot: Node = null

func _ready():
    _setup_ui()
    _load_inventory()

func _setup_ui():
    # Create slots
    for i in range(slot_count):
        var slot = CanvasItem.new()
        slot.position = Vector2(i * (item_size.x + 50), 50)
        add_child(slot)
        
        # Add item label
        var label = Label.new()
        label.text = "Slot %d" % (i + 1)
        label.position = slot.position + Vector2(25, 25)
        label.font_size = 14
        slot.add_child(label)
        
        # Add item image (placeholder)
        var image = CanvasItem.new()
        image.position = slot.position + Vector2(25, 50)
        image.texture = load_texture("res://assets/default_item.png")
        slot.add_child(image)
        
        _slots.append(slot)

func _load_inventory():
    _inventory = []
    for item in _inventory:
        _update_slot(item, 0)

func add_item(item: String) -> void:
    _inventory.append(item)
    _update_slot(item, _inventory.size() - 1)
    print("Added: %s" % item)

func remove_item(index: int) -> void:
    if index >= 0 and index < _inventory.size():
        var removed_item = _inventory[index]
        _inventory.remove(index)
        _update_slot(removed_item, index)
        print("Removed: %s" % removed_item)

func equip_item(index: int) -> void:
    if index >= 0 and index < _inventory.size():
        var item = _inventory[index]
        _inventory.remove(index)
        _equipped_item = item
        emit_signal("item_equipped", item)
        print("Equipped: %s" % item)
        
        # Update equipped slot
        if _equipped_slot:
            _equipped_slot.texture = load_texture("res://assets/default_item.png")
        
        # Create new equipped slot
        _equipped_slot = CanvasItem.new()
        _equipped_slot.position = Vector2(200, 50)
        _equipped_slot.texture = load_texture("res://assets/equipped_item.png")
        add_child(_equipped_slot)

func use_item(index: int) -> void:
    if index >= 0 and index < _inventory.size():
        var item = _inventory[index]
        _inventory.remove(index)
        emit_signal("item_used", item)
        print("Used: %s" % item)

func _update_slot(item: String, index: int) -> void:
    if index >= 0 and index < _slots.size():
        var slot = _slots[index]
        var label = slot.get_children()[1] as Label
        var image = slot.get_children()[2] as CanvasItem
        
        label.text = "%s" % item
        image.texture = load_texture("res://assets/item_%s.png" % item.lower())

func get_inventory() -> Array:
    return _inventory.copy()