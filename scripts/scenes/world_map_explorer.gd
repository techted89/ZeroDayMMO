extends Control

@export var grid_size: int = 7
@export var cell_size: int = 60

signal node_selected(ip: String)

var _nodes: Array = []
var _current_ip: String = "127.0.0.1"
var _player_pos: Vector2i = Vector2i(0, 0)

var _node_type_colors: Dictionary = {
	"empty": Color(0.2, 0.2, 0.2),
	"router": Color(0, 0.8, 1),
	"crypto_mine": Color(1, 0.8, 0),
	"data_center": Color(0.3, 0.5, 1),
	"virus": Color(1, 0.2, 0.2),
	"worm": Color(1, 0.4, 0),
	"devourer": Color(0.5, 0, 0),
	"ai_overlord": Color(0.5, 0, 0.8),
	"base": Color(0, 1, 0.3),
	"firewall": Color(1, 1, 1),
}

var _node_type_icons: Dictionary = {
	"empty": "·",
	"router": "⬡",
	"crypto_mine": "₿",
	"data_center": "◈",
	"virus": "☠",
	"worm": "⌘",
	"devourer": "◆",
	"ai_overlord": "▲",
	"base": "⬛",
	"firewall": "▣",
}

var _cells: Dictionary = {}

@onready var _grid_container: GridContainer = $GridContainer
@onready var _info_label: RichTextLabel = $InfoLabel

func _ready():
	_setup_grid()

func _setup_grid():
	_grid_container.columns = grid_size
	for y in range(grid_size):
		for x in range(grid_size):
			var cell = PanelContainer.new()
			cell.custom_minimum_size = Vector2(cell_size, cell_size)
			cell.mouse_filter = Control.MOUSE_FILTER_STOP
			cell.gui_input.connect(_on_cell_input.bind(x, y))

			var vbox = VBoxContainer.new()
			vbox.alignment = BOX_ALIGNMENT_CENTER
			vbox.size_flags_horizontal = Control.SIZE_EXPAND_FILL
			vbox.size_flags_vertical = Control.SIZE_EXPAND_FILL

			var icon = Label.new()
			icon.name = "Icon"
			icon.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
			icon.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
			icon.add_theme_font_size_override("font_size", 18)
			vbox.add_child(icon)

			var ip_label = Label.new()
			ip_label.name = "IPLabel"
			ip_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
			ip_label.add_theme_font_size_override("font_size", 8)
			vbox.add_child(ip_label)

			cell.add_child(vbox)
			_grid_container.add_child(cell)
			_cells[Vector2i(x, y)] = {"panel": cell, "icon": icon, "ip_label": ip_label}

func load_area(center_ip: String):
	_current_ip = center_ip
	var octets = center_ip.split(".")
	if octets.size() != 4:
		return
	var base = int(octets[0])
	var o2 = int(octets[1])
	var cx = int(octets[2])
	var cy = int(octets[3])
	var half = grid_size / 2

	_nodes.clear()
	for dx in range(-half, half + 1):
		for dy in range(-half, half + 1):
			var nx = cx + dx
			var ny = cy + dy
			if nx < 0 or nx > 255 or ny < 0 or ny > 255:
				continue
			var ip = "%d.%d.%d.%d" % [base, o2, nx, ny]
			var is_center = (dx == 0 and dy == 0)
			var node_data = {"ip": ip, "type": "empty", "owner": "", "is_player": is_center}

			if not is_center:
				var seed_val = base * 1000000 + o2 * 10000 + nx * 100 + ny
				var seeded = (seed_val * 1103515245 + 12345) % 2147483648
				var type_idx = seeded % 12
				match type_idx:
					0, 1: node_data.type = "empty"
					2: node_data.type = "router"
					3: node_data.type = "crypto_mine"
					4: node_data.type = "data_center"
					5, 6: node_data.type = "virus"
					7: node_data.type = "worm"
					8: node_data.type = "devourer"
					9: node_data.type = "ai_overlord" if not center_ip.begins_with("127.") else "empty"
					10: node_data.type = "firewall"
					11: node_data.type = "empty"
				if base >= 224 and type_idx < 9:
					var danger_roll = seeded % 4
					if danger_roll == 0: node_data.type = "devourer"
					elif danger_roll == 1: node_data.type = "ai_overlord"

			_nodes.append(node_data)

	_update_grid_display()

func _update_grid_display():
	var half = grid_size / 2
	var cell_idx = 0
	for y in range(grid_size):
		for x in range(grid_size):
			var node_data = _get_node_at_grid(x, y, half)
			var cell_info = _cells.get(Vector2i(x, y))
			if not cell_info:
				continue
			if node_data:
				cell_info.icon.text = _node_type_icons.get(node_data.type, "?")
				var color = _node_type_colors.get(node_data.type, Color(0.3, 0.3, 0.3))
				if node_data.get("is_player", false):
					cell_info.icon.add_theme_color_override("font_color", Color(0, 1, 0.5))
					cell_info.icon.text = "⬤"
				else:
					cell_info.icon.add_theme_color_override("font_color", color)
				cell_info.ip_label.text = node_data.ip
				var style = StyleBoxFlat.new()
				style.bg_color = Color(color.r * 0.1, color.g * 0.1, color.b * 0.1, 0.3)
				style.border_color = color
				style.border_width_left = 1
				style.border_width_right = 1
				style.border_width_top = 1
				style.border_width_bottom = 1
				cell_info.panel.add_theme_stylebox_override("panel", style)
			cell_idx += 1

func _get_node_at_grid(gx: int, gy: int, half: int) -> Dictionary:
	var target_octets = _current_ip.split(".")
	if target_octets.size() != 4:
		return {}
	var cx = int(target_octets[2])
	var cy = int(target_octets[3])
	var nx = cx + (gx - half)
	var ny = cy + (gy - half)
	var target_ip = "%s.%s.%d.%d" % [target_octets[0], target_octets[1], nx, ny]

	for n in _nodes:
		if n.ip == target_ip:
			return n
	return {}

func _on_cell_input(event: InputEvent, x: int, y: int):
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		var half = grid_size / 2
		var node_data = _get_node_at_grid(x, y, half)
		if node_data and not node_data.get("is_player", false):
			node_selected.emit(node_data.ip)

func get_player_info() -> String:
	var p = ZeroDayCore.current_player
	if not p:
		return "Player not loaded"
	return "[color=#00FF00]📍 %s | Level %d | HP: %d/%d[/color]" % [p.username, p.level, 100 + p.level * 10, 100 + p.level * 10]
