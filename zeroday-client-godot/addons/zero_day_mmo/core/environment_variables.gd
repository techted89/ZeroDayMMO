extends Node
## Thin wrapper around OS.get_environment() for autoload convenience.

static func has(key: String) -> bool:
	var val = OS.get_environment(key)
	return val != null and not val.strip_edges().is_empty()

static func get_or_fail(key: String) -> String:
	var val = OS.get_environment(key)
	if val != null and not val.strip_edges().is_empty():
		return val.strip_edges()
	return ""

static func get_int(key: String, default_value: int = 0) -> int:
	var val = OS.get_environment(key)
	if val != null and val.strip_edges().is_valid_int():
		return val.strip_edges().to_int()
	return default_value

static func get_float(key: String, default_value: float = 0.0) -> float:
	var val = OS.get_environment(key)
	if val != null and val.strip_edges().is_valid_float():
		return val.strip_edges().to_float()
	return default_value
