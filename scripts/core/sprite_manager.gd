extends Node

class_name SpriteManager

signal sprites_loaded

var _textures: Dictionary = {}
var _sprite_info: Dictionary = {}

var _enemy_texture_paths: Dictionary = {
	"virus": "res://generated_sprites/spritsheet (2).jpg",
	"worm": "res://generated_sprites/cyberworm.jpg",
	"cyberworm": "res://generated_sprites/cyberworm2.jpg",
	"world_grid": "res://generated_sprites/main-world-grid-nova.jpg",
}

func _ready():
	_preload_enemy_textures()

func _preload_enemy_textures():
	for key in _enemy_texture_paths:
		var path = _enemy_texture_paths[key]
		if ResourceLoader.exists(path):
			var tex = load(path)
			if tex:
				_textures[key] = tex
				_sprite_info[key] = {"path": path, "width": tex.get_width(), "height": tex.get_height()}
	sprites_loaded.emit()

func get_texture(key: String) -> Texture2D:
	return _textures.get(key)

func get_sprite_info(key: String) -> Dictionary:
	return _sprite_info.get(key, {})

func get_enemy_texture(enemy_type: String) -> Texture2D:
	var path = _enemy_texture_paths.get(enemy_type)
	if path and ResourceLoader.exists(path):
		if not _textures.has(enemy_type):
			_textures[enemy_type] = load(path)
		return _textures[enemy_type]
	var fallback = _textures.get("virus")
	if not fallback:
		for k in _textures:
			fallback = _textures[k]
			break
	return fallback

func draw_enemy_sprite(canvas: Control, enemy_type: String, rect: Rect2):
	var tex = get_enemy_texture(enemy_type)
	if not tex:
		return
	var tex_rect = TextureRect.new()
	tex_rect.texture = tex
	tex_rect.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	tex_rect.expand_mode = TextureRect.EXPAND_FIT_HEIGHT_PROPORTIONAL
	tex_rect.position = rect.position
	tex_rect.size = rect.size
	canvas.add_child(tex_rect)
