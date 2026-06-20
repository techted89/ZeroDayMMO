extends Control

@export var enemy_sprite_size: Vector2 = Vector2(200, 200)
@export var player_sprite_size: Vector2 = Vector2(150, 150)

signal combat_ended(result: String)

var _enemy_type: String = ""
var _enemy_hp: int = 0
var _enemy_max_hp: int = 0
var _player_hp: int = 0
var _player_max_hp: int = 0
var _combat_log: Array[String] = []
var _is_active: bool = false
var _round: int = 0

@onready var _enemy_container: PanelContainer = $EnemyContainer
@onready var _enemy_sprite: TextureRect = $EnemyContainer/EnemySprite
@onready var _enemy_name: Label = $EnemyContainer/EnemyName
@onready var _enemy_hp_bar: ProgressBar = $EnemyContainer/EnemyHPBar
@onready var _enemy_hp_text: Label = $EnemyContainer/EnemyHPText

@onready var _player_container: PanelContainer = $PlayerContainer
@onready var _player_sprite: TextureRect = $PlayerContainer/PlayerSprite
@onready var _player_hp_bar: ProgressBar = $PlayerContainer/PlayerHPBar
@onready var _player_hp_text: Label = $PlayerContainer/PlayerHPText

@onready var _log_container: VBoxContainer = $LogContainer/LogScroll/LogContent
@onready var _round_label: Label = $RoundLabel

func _ready():
	hide()

func start_combat(enemy_type: String, enemy_name: String, enemy_hp: int, player_hp: int):
	_enemy_type = enemy_type
	_enemy_hp = enemy_hp
	_enemy_max_hp = enemy_hp
	_player_hp = player_hp
	_player_max_hp = player_hp
	_combat_log.clear()
	_is_active = true
	_round = 0

	_enemy_name.text = enemy_name.to_upper()
	_update_enemy_sprite()
	_update_bars()
	_clear_log()

	show()
	modulate.a = 0.0
	var tween = create_tween()
	tween.tween_property(self, "modulate:a", 1.0, 0.5)

	_add_log("[color=#FF4444]⚔ COMBAT INITIATED: %s[/color]" % enemy_name.to_upper())
	_add_log("[color=#888888]Round 1 begins...[/color]")

func _update_enemy_sprite():
	var tex = SpriteManager.get_enemy_texture(_enemy_type)
	if tex:
		_enemy_sprite.texture = tex
	else:
		_enemy_sprite.texture = null

func _update_bars():
	_enemy_hp_bar.max_value = _enemy_max_hp
	_enemy_hp_bar.value = _enemy_hp
	_enemy_hp_text.text = "%d / %d" % [_enemy_hp, _enemy_max_hp]

	_player_hp_bar.max_value = _player_max_hp
	_player_hp_bar.value = _player_hp
	_player_hp_text.text = "%d / %d" % [_player_hp, _player_max_hp]

func process_combat_round(player_damage: int, enemy_damage: int, player_atk: int, enemy_atk: int, player_def: int, enemy_def: int) -> Dictionary:
	if not _is_active:
		return {"ended": false}

	_round += 1
	_enemy_hp = max(0, _enemy_hp - player_damage)
	_player_hp = max(0, _player_hp - enemy_damage)

	_update_bars()

	_add_log("Round %d:" % _round)
	_add_log("  [color=#00FF00]You deal %d damage[/color] (ATK %d vs DEF %d)" % [player_damage, player_atk, enemy_def])
	_add_log("  [color=#FF4444]Enemy deals %d damage[/color] (ATK %d vs DEF %d)" % [enemy_damage, enemy_atk, player_def])

	if _enemy_hp <= 0:
		_end_combat("victory")
		return {"ended": true, "result": "victory"}

	if _player_hp <= 0:
		_end_combat("defeat")
		return {"ended": true, "result": "defeat"}

	return {"ended": false}

func _end_combat(result: String):
	_is_active = false
	var msg = "VICTORY!" if result == "victory" else "DEFEAT!"
	var color = "#00FF00" if result == "victory" else "#FF4444"
	_add_log("[color=%s]═══ %s ═══[/color]" % [color, msg])

	var tween = create_tween()
	tween.tween_property(self, "modulate:a", 0.0, 1.0)
	tween.tween_callback(func(): 
		hide()
		combat_ended.emit(result)
	)

func _add_log(text: String):
	_combat_log.append(text)
	var label = Label.new()
	label.text = text
	label.bbcode_enabled = true
	label.add_theme_color_override("font_color", Color(0.8, 0.8, 0.8))
	label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_log_container.add_child(label)
	_log_container.get_parent().get_parent().scroll_vertical = (LogContainer.get_parent() as ScrollContainer).get_v_scroll_bar().max_value

func _clear_log():
	for child in _log_container.get_children():
		child.queue_free()

func update_enemy_hp(hp: int):
	_enemy_hp = hp
	_update_bars()

func update_player_hp(hp: int):
	_player_hp = hp
	_update_bars()
