extends Node

var _matches: Array[String] = []
var _selected_index: int = -1
var _original_text: String = ""
var _cursor_pos: int = 0
var _popup: PopupPanel = null
var _item_list: ItemList = null
var _input_field: LineEdit = null

signal command_selected(command: String)

func _ready():
	_ensure_popup()

func _ensure_popup():
	_popup = PopupPanel.new()
	_popup.name = "AutocompletePopup"
	_popup.size = Vector2(400, 200)
	_popup.hide()
	
	_item_list = ItemList.new()
	_item_list.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	_item_list.size_flags_vertical = Control.SIZE_EXPAND_FILL
	_item_list.allow_reselect = true
	_item_list.item_selected.connect(_on_item_selected)
	
	_popup.add_child(_item_list)
	add_child(_popup)

func attach(input_field: LineEdit):
	_input_field = input_field
	_input_field.text_changed.connect(_on_text_changed)
	_input_field.gui_input.connect(_on_gui_input)

func _on_text_changed(new_text: String):
	if not _input_field:
		return
	_cursor_pos = _input_field.caret_column
	_original_text = new_text
	_update_matches(new_text)

func _update_matches(text: String):
	_matches.clear()
	if text.is_empty():
		_hide_popup()
		return
	
	var term = text.left(_cursor_pos)
	var last_word = _get_current_word(term)
	if last_word.is_empty():
		_hide_popup()
		return
	
	_matches = TerminalParser.suggest(last_word)
	if _matches.is_empty():
		_hide_popup()
		return
	
	_selected_index = -1
	_populate_popup()
	_show_popup()

func _get_current_word(text: String) -> String:
	var space_idx = text.rfind(" ")
	if space_idx >= 0:
		return text.substr(space_idx + 1)
	return text

func _populate_popup():
	_item_list.clear()
	for match in _matches:
		_item_list.add_item(match)

func _show_popup():
	if not _input_field or _matches.is_empty():
		return
	
	var pos = _input_field.global_position
	pos.y += _input_field.size.y + 2
	_popup.popup()
	_popup.position = Vector2(
		mini(pos.x, DisplayServer.window_get_size().x - _popup.size.x),
		mini(pos.y, DisplayServer.window_get_size().y - _popup.size.y)
	)

func _hide_popup():
	if _popup and _popup.visible:
		_popup.hide()
	_selected_index = -1

func _on_gui_input(event: InputEvent):
	if not _popup or not _popup.visible:
		return
	
	if event is InputEventKey:
		if event.keycode == KEY_TAB or event.keycode == KEY_ENTER:
			if _selected_index >= 0 and _selected_index < _matches.size():
				_apply_selection(_matches[_selected_index])
			accept_event()
		
		elif event.keycode == KEY_UP:
			_selected_index = wrapi(_selected_index - 1, 0, _matches.size())
			_item_list.select(_selected_index)
			accept_event()
		
		elif event.keycode == KEY_DOWN:
			_selected_index = wrapi(_selected_index + 1, 0, _matches.size())
			_item_list.select(_selected_index)
			accept_event()
		
		elif event.keycode == KEY_ESCAPE:
			_hide_popup()

func _on_item_selected(index: int):
	_selected_index = index
	if index >= 0 and index < _matches.size():
		_apply_selection(_matches[index])

func _apply_selection(selection: String):
	if not _input_field:
		return
	var text = _input_field.text
	var term = text.left(_cursor_pos)
	var word_start = term.rfind(" ")
	if word_start >= 0:
		var prefix = text.left(word_start + 1)
		var suffix = text.substr(_cursor_pos)
		_input_field.text = prefix + selection + " "
		_input_field.caret_column = prefix.length() + selection.length() + 1
	else:
		_input_field.text = selection + " "
		_input_field.caret_column = selection.length() + 1
	_hide_popup()
	command_selected.emit(selection)
