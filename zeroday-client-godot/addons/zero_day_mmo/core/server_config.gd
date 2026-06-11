extends Node

const DEFAULT_URL: String = "ws://localhost:8080/game"

static func get_server_url() -> String:
	var env_url = EnvironmentVariables.get_or_fail("ZERODAY_SERVER_URL")
	if env_url:
		return env_url.strip_edges()

	var project_url = ProjectSettings.get_setting("zeroday/server_url", "")
	if project_url:
		return project_url.strip_edges()

	var cfg = ConfigFile.new()
	var err = cfg.load("user://server.cfg")
	if err == OK:
		var url = cfg.get_value("connection", "url", "")
		if url:
			return url.strip_edges()

	err = cfg.load("res://server.cfg")
	if err == OK:
		var url = cfg.get_value("connection", "url", "")
		if url:
			return url.strip_edges()

	return DEFAULT_URL

static func set_server_url(url: String) -> void:
	if url.is_empty(): return
	var cfg = ConfigFile.new()
	cfg.set_value("connection", "url", url.strip_edges())
	cfg.save("user://server.cfg")

static func get_server_name() -> String:
	return EnvironmentVariables.get_or_fail("ZERODAY_SERVER_NAME") if EnvironmentVariables.has("ZERODAY_SERVER_NAME") else "ZeroDayMMO"
