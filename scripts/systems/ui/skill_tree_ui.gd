extends Control

signal skill_tree_ready()
signal skill_unlocked(skill_name: String)
signal skill_points_changed(points: int)

var skill_points_label: Label
var skill_level_label: Label
var _skill_points: int = 0

var _skills: Dictionary = {
	"scan": {"name": "Deep Scan", "desc": "Scan IPs from greater distance", "level": 1, "max_level": 5, "cost": 1, "icon": "🔍"},
	"defend": {"name": "Firewall", "desc": "Reduce incoming damage", "level": 1, "max_level": 5, "cost": 1, "icon": "🛡️"},
	"attack": {"name": "Overclock", "desc": "Increase attack power", "level": 1, "max_level": 5, "cost": 1, "icon": "⚡"},
	"stealth": {"name": "Stealth Net", "desc": "Reduce detection chance", "level": 1, "max_level": 3, "cost": 2, "icon": "👻"},
	"data": {"name": "Data Minig", "desc": "Bonus XP from kills", "level": 1, "max_level": 5, "cost": 1, "icon": "💾"},
	"bandwidth": {"name": "Band Boost", "desc": "Increase bandwidth cap", "level": 1, "max_level": 3, "cost": 2, "icon": "📡"},
	"crypto": {"name": "Crypto Mine", "desc": "Passive credit income", "level": 1, "max_level": 3, "cost": 2, "icon": "💰"},
}

var _skill_nodes: Dictionary = {}

func _ready():
	_setup_ui()

func _setup_ui():
	var margin = MarginContainer.new()
	margin.add_theme_constant_override("margin_all", 10)
	add_child(margin)

	var vbox = VBoxContainer.new()
	margin.add_child(vbox)

	var title = Label.new()
	title.text = "SKILL TREE"
	title.add_theme_color_override("font_color", Color(0, 1, 0.3))
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	title.add_theme_font_size_override("font_size", 24)
	vbox.add_child(title)

	var info_bar = HBoxContainer.new()
	vbox.add_child(info_bar)

	skill_points_label = Label.new()
	skill_points_label.text = "Skill Points: 0"
	skill_points_label.add_theme_color_override("font_color", Color(1, 1, 0))
	info_bar.add_child(skill_points_label)

	skill_level_label = Label.new()
	skill_level_label.text = "Tree Level: 1"
	skill_level_label.add_theme_color_override("font_color", Color(0, 1, 0.6))
	info_bar.add_child(skill_level_label)

	var scroll = ScrollContainer.new()
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	vbox.add_child(scroll)

	var grid = GridContainer.new()
	grid.columns = 4
	grid.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.add_child(grid)

	for key in _skills:
		var skill = _skills[key]
		var card = PanelContainer.new()
		card.custom_minimum_size = Vector2(160, 140)
		card.add_theme_stylebox_override("panel", _make_border_style(Color(0, 0.5, 0.2)))

		var card_vbox = VBoxContainer.new()
		card_vbox.alignment = BOX_ALIGNMENT_CENTER
		card.add_child(card_vbox)

		var icon_lbl = Label.new()
		icon_lbl.text = skill.icon
		icon_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		icon_lbl.add_theme_font_size_override("font_size", 28)
		card_vbox.add_child(icon_lbl)

		var name_lbl = Label.new()
		name_lbl.text = skill.name
		name_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		name_lbl.add_theme_color_override("font_color", Color(0.3, 1, 0.6))
		card_vbox.add_child(name_lbl)

		var level_lbl = Label.new()
		level_lbl.name = "LevelLabel"
		level_lbl.text = "Lv.%d/%d" % [skill.level, skill.max_level]
		level_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		level_lbl.add_theme_font_size_override("font_size", 12)
		card_vbox.add_child(level_lbl)

		var desc_lbl = Label.new()
		desc_lbl.text = skill.desc
		desc_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		desc_lbl.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		desc_lbl.add_theme_font_size_override("font_size", 10)
		card_vbox.add_child(desc_lbl)

		var btn = Button.new()
		btn.name = "UpgradeBtn"
		btn.text = "Upgrade (%d SP)" % skill.cost
		btn.disabled = true
		btn.pressed.connect(_on_upgrade_pressed.bind(key))
		card_vbox.add_child(btn)

		_skill_nodes[key] = {"card": card, "level_label": level_lbl, "btn": btn, "skill": skill}
		grid.add_child(card)

	emit_signal("skill_tree_ready")

func _make_border_style(color: Color) -> StyleBoxFlat:
	var style = StyleBoxFlat.new()
	style.bg_color = Color(0, 0.05, 0, 0.8)
	style.border_color = color
	style.border_width_left = 1
	style.border_width_right = 1
	style.border_width_top = 1
	style.border_width_bottom = 1
	style.corner_radius_top_left = 4
	style.corner_radius_top_right = 4
	style.corner_radius_bottom_right = 4
	style.corner_radius_bottom_left = 4
	return style

func _on_upgrade_pressed(skill_key: String):
	var node = _skill_nodes.get(skill_key)
	if not node:
		return
	var skill = node.skill
	if _skill_points >= skill.cost and skill.level < skill.max_level:
		_skill_points -= skill.cost
		skill.level += 1
		_update_skill_display(skill_key)
		skill_unlocked.emit(skill_key)
		skill_points_changed.emit(_skill_points)

func _update_skill_display(skill_key: String):
	var node = _skill_nodes.get(skill_key)
	if not node:
		return
	var skill = node.skill
	node.level_label.text = "Lv.%d/%d" % [skill.level, skill.max_level]
	if skill.level >= skill.max_level:
		node.btn.text = "MAXED"
		node.btn.disabled = true
		node.btn.add_theme_color_override("font_color", Color(1, 1, 0))
	else:
		node.btn.text = "Upgrade (%d SP)" % skill.cost
		node.btn.disabled = _skill_points < skill.cost
	skill_points_label.text = "Skill Points: %d" % _skill_points

func set_skill_points(points: int):
	_skill_points = points
	skill_points_label.text = "Skill Points: %d" % _skill_points
	for key in _skill_nodes:
		var node = _skill_nodes[key]
		var skill = node.skill
		if skill.level < skill.max_level:
			node.btn.disabled = _skill_points < skill.cost

func load_skills_from_server(skills_data: Dictionary):
	for key in skills_data:
		if _skills.has(key):
			_skills[key].level = skills_data.get(key, {}).get("level", 1)
			_update_skill_display(key)
