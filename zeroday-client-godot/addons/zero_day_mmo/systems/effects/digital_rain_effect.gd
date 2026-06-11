extends Control

@export var columns: int = 40
@export var fall_speed: float = 0.5
@export var density: float = 0.3
@export var rain_color: Color = Color(0, 1, 0.2, 0.15)
@export var is_active: bool = true

var _rain_image: Image
var _rain_texture: ImageTexture
var _texture_height: int = 40
var _drop_positions: Array[float] = []
var _drop_speeds: Array[float] = []

func _ready():
	_texture_height = maxi(10, columns / 2)
	_rain_image = Image.create(columns, _texture_height, false, Image.FORMAT_RGBA8)
	_rain_texture = ImageTexture.create_from_image(_rain_image)
	_drop_positions.resize(columns)
	_drop_speeds.resize(columns)
	for i in range(columns):
		_drop_positions[i] = randf_range(0.0, _texture_height)
		_drop_speeds[i] = fall_speed * randf_range(0.5, 1.5)
	texture = _rain_texture
	material = null

func _process(delta: float):
	if not is_active or not _rain_image: return
	_rain_image.fill(Color.TRANSPARENT)
	for x in range(columns):
		if randf() > density * 0.3: continue
		_drop_positions[x] += _drop_speeds[x] * delta
		if _drop_positions[x] >= _texture_height:
			_drop_positions[x] = 0
			_drop_speeds[x] = fall_speed * randf_range(0.5, 1.5)
		var y_pos = floori(_drop_positions[x])
		var head = Color(0.8, 1, 0.8, rain_color.a * 2)
		_rain_image.set_pixel(x, y_pos, head)
		for t in range(1, 5):
			var ty = y_pos - t
			if ty >= 0 and ty < _texture_height:
				var fade = 1.0 - (t / 5.0)
				var trail = Color(0, 1, 0.2, rain_color.a * fade * 0.5)
				_rain_image.set_pixel(x, ty, trail)
	_rain_texture.update(_rain_image)

func set_alpha(alpha: float):
	rain_color.a = alpha
