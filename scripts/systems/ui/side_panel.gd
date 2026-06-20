extends PanelContainer

enum PanelTab { TASKS, INVENTORY, STATS, HELP }

var _current_tab: int = PanelTab.TASKS
var _tab_container: TabContainer = null

func _ready():
	_setup_panel()
	UIManager.get_event_system().task_updated.connect(_refresh_current)

func _setup_panel():
	var vbox = VBoxContainer.new()
	vbox.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	vbox.size_flags_vertical = Control.SIZE_EXPAND_FILL
	
	_tab_container = TabContainer.new()
	_tab_container.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	_tab_container.size_flags_vertical = Control.SIZE_EXPAND_FILL
	_tab_container.tab_changed.connect(_on_tab_changed)
	
	_tab_container.add_child(_create_tasks_tab())
	_tab_container.add_child(_create_inventory_tab())
	_tab_container.add_child(_create_stats_tab())
	_tab_container.add_child(_create_help_tab())
	
	vbox.add_child(_tab_container)
	add_child(vbox)
	
	set_size(Vector2(250, 300))

func _create_tasks_tab() -> Control:
	var tab = VBoxContainer.new()
	tab.name = "Tasks"
	
	var scroll = ScrollContainer.new()
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	
	var task_list = VBoxContainer.new()
	task_list.name = "TaskList"
	task_list.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	
	scroll.add_child(task_list)
	tab.add_child(scroll)
	
	UIManager.get_event_system().task_updated.connect(func(_tid, _s, _p): _refresh_task_list(task_list))
	
	return tab

func _create_inventory_tab() -> Control:
	var tab = VBoxContainer.new()
	tab.name = "Inventory"
	
	var scroll = ScrollContainer.new()
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	
	var item_list = VBoxContainer.new()
	item_list.name = "ItemList"
	item_list.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var empty_label = Label.new()
	empty_label.text = "No items"
	item_list.add_child(empty_label)
	
	scroll.add_child(item_list)
	tab.add_child(scroll)
	
	return tab

func _create_stats_tab() -> Control:
	var tab = VBoxContainer.new()
	tab.name = "Stats"
	
	var stats = VBoxContainer.new()
	stats.name = "StatsContent"
	stats.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	
	stats.add_child(_create_stat_row("Level", "1"))
	stats.add_child(_create_stat_row("XP", "0 / 100"))
	stats.add_child(_create_stat_row("Credits", "$0"))
	stats.add_child(HSeparator.new())
	stats.add_child(_create_stat_row("CPU", "100%"))
	stats.add_child(_create_stat_row("RAM", "100%"))
	stats.add_child(_create_stat_row("Bandwidth", "100%"))
	stats.add_child(HSeparator.new())
	stats.add_child(_create_stat_row("Tasks Done", "0"))
	stats.add_child(_create_stat_row("NPAC Kills", "0"))
	
	tab.add_child(stats)
	
	return tab

func _create_help_tab() -> Control:
	var tab = VBoxContainer.new()
	tab.name = "Help"
	
	var help_text = RichTextLabel.new()
	help_text.name = "HelpContent"
	help_text.bbcode_enabled = true
	help_text.size_flags_vertical = Control.SIZE_EXPAND_FILL
	help_text.text = """[color=#00FF00][b]ZeroDay Commands[/b][/color]
	
[color=#00CC00]/help[/color] - Show this help
[color=#00CC00]/login[/color] - Login to game
[color=#00CC00]/scan[/color] - Scan network
[color=#00CC00]/attack[/color] - Attack target
[color=#00CC00]/inventory[/color] - View items
[color=#00CC00]/stats[/color] - View stats
[color=#00CC00]/tasks[/color] - View tasks
"""
	
	var scroll = ScrollContainer.new()
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	scroll.add_child(help_text)
	tab.add_child(scroll)
	
	return tab

func _create_stat_row(label: String, value: String) -> HBoxContainer:
	var row = HBoxContainer.new()
	row.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	
	var lbl = Label.new()
	lbl.text = label + ":"
	lbl.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	
	var val = Label.new()
	val.text = value
	val.add_theme_color_override("font_color", Color(0, 1, 0.3, 1))
	val.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	
	row.add_child(lbl)
	row.add_child(val)
	return row

func switch_tab(tab: int):
	if tab >= 0 and tab < _tab_container.get_tab_count():
		_tab_container.current_tab = tab
		_current_tab = tab

func _on_tab_changed(tab_index: int):
	_current_tab = tab_index
	_refresh_current()

func _refresh_current(_a = null, _b = null, _c = null):
	match _current_tab:
		PanelTab.TASKS:
			_refresh_task_list(_tab_container.get_child(0).get_node("TaskList"))
		PanelTab.INVENTORY:
			_refresh_inventory()
		PanelTab.STATS:
			_refresh_stats()

func _refresh_task_list(task_list: VBoxContainer):
	if not task_list:
		return
	
	for child in task_list.get_children():
		child.queue_free()
	
	var player = GameManager.player_data if is_instance_valid(GameManager) else null
	if player and player is Dictionary:
		var tasks = player.get("active_tasks", [])
		if tasks.size() == 0:
			var lbl = Label.new()
			lbl.text = "No active tasks"
			task_list.add_child(lbl)
			return
		for t in tasks:
			var lbl = Label.new()
			if typeof(t) == TYPE_DICTIONARY:
				lbl.text = "- %s" % t.get("title", t.get("id", "Unknown"))
			else:
				lbl.text = "- %s" % str(t)
			lbl.add_theme_color_override("font_color", Color(0, 1, 0.3))
			task_list.add_child(lbl)
	else:
		var lbl = Label.new()
		lbl.text = "No active tasks"
		task_list.add_child(lbl)

func _refresh_inventory():
	var item_list = _tab_container.get_child(1).get_node("ItemList")
	if not item_list:
		return
	for child in item_list.get_children():
		child.queue_free()
	
	var lbl = Label.new()
	lbl.text = "Inventory loading..."
	item_list.add_child(lbl)

func _refresh_stats():
	var stats = _tab_container.get_child(2).get_node("StatsContent")
	if not stats or not GameManager.player_data:
		return
	
	var children = stats.get_children()
	var data = GameManager.player_data
	if children.size() >= 3:
		children[1].text = "Level: " + str(data.get("level", 1))
	if children.size() >= 5:
		children[3].text = "Credits: $" + str(data.get("credits", 0))
