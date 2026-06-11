extends Node
## Central configuration loader for ZeroDayMMO.
##
## Reads from ProjectSettings (zeroday/* keys defined in project.godot),
## with environment variable overrides and legacy server.cfg fallback.
## This is the single source of truth for all client-side config.

const DEFAULT_URL: String = "ws://localhost:8080/game"

# ---- Server connection ----

static func get_server_url() -> String:
	var env_url = _env_str("ZERODAY_SERVER_URL")
	if env_url:
		return env_url

	var ps_url = ProjectSettings.get_setting("zeroday/server/url", "")
	if ps_url:
		return ps_url.strip_edges()

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
	if url.is_empty():
		return
	var cfg = ConfigFile.new()
	cfg.set_value("connection", "url", url.strip_edges())
	cfg.save("user://server.cfg")

# ---- Server settings ----

static func get_server_name() -> String:
	return _env_str("ZERODAY_SERVER_NAME") or ProjectSettings.get_setting("zeroday/server/name", "ZeroDayMMO")

static func get_server_port() -> int:
	return _env_int("ZERODAY_PORT") or ProjectSettings.get_setting("zeroday/server/port", 8080)

static func get_max_connections() -> int:
	return _env_int("ZERODAY_MAX_CONNECTIONS") or ProjectSettings.get_setting("zeroday/server/max_connections", 2000)

static func get_max_connections_per_ip() -> int:
	return _env_int("ZERODAY_MAX_CONNECTIONS_PER_IP") or ProjectSettings.get_setting("zeroday/server/max_connections_per_ip", 8)

static func get_max_players() -> int:
	return _env_int("ZERODAY_MAX_PLAYERS") or ProjectSettings.get_setting("zeroday/server/max_players", 1000)

# ---- Player defaults ----

static func get_starting_cpu() -> int:
	return ProjectSettings.get_setting("zeroday/server/starting_cpu", 100)

static func get_starting_ram() -> int:
	return ProjectSettings.get_setting("zeroday/server/starting_ram", 256)

static func get_starting_bandwidth() -> int:
	return ProjectSettings.get_setting("zeroday/server/starting_bandwidth", 50)

static func get_starting_credits() -> int:
	return ProjectSettings.get_setting("zeroday/server/starting_credits", 0)

# ---- Persistence ----

static func get_data_dir() -> String:
	return _env_str("ZERODAY_DATA_DIR") or ProjectSettings.get_setting("zeroday/data_dir", "data")

static func get_snapshot_interval_ms() -> int:
	return _env_int("ZERODAY_SNAPSHOT_INTERVAL_MS") or ProjectSettings.get_setting("zeroday/server/snapshot_interval_ms", 60000)

# ---- Watchdog ----

static func get_idle_timeout_ms() -> int:
	return _env_int("ZERODAY_IDLE_TIMEOUT_MS") or ProjectSettings.get_setting("zeroday/server/idle_timeout_ms", 300000)

static func get_sweep_interval_ms() -> int:
	return _env_int("ZERODAY_SWEEP_INTERVAL_MS") or ProjectSettings.get_setting("zeroday/server/sweep_interval_ms", 30000)

# ---- Resource regen ----

static func get_regen_interval_ms() -> int:
	return _env_int("ZERODAY_REGEN_INTERVAL_MS") or ProjectSettings.get_setting("zeroday/server/regen_interval_ms", 5000)

# ---- Admin settings ----

static func get_admin_username() -> String:
	return _env_str("ZERODAY_ADMIN_USERNAME") or ProjectSettings.get_setting("zeroday/admin/username", "admin")

static func get_admin_password() -> String:
	return _env_str("ZERODAY_ADMIN_PASSWORD") or ProjectSettings.get_setting("zeroday/admin/password", "zeroday-admin-1337")

static func get_admin_token() -> String:
	return _env_str("ZERODAY_ADMIN_TOKEN") or ProjectSettings.get_setting("zeroday/admin/token", "zeroday-admin-secret-change-me")

static func get_admin_port() -> int:
	return _env_int("ZERODAY_ADMIN_PORT") or ProjectSettings.get_setting("zeroday/admin/port", 8081)

static func get_admin_rate_limit() -> int:
	return _env_int("ZERODAY_ADMIN_RATE_LIMIT") or ProjectSettings.get_setting("zeroday/admin/rate_limit", 30)

static func get_log_retention_hours() -> int:
	return _env_int("ZERODAY_LOG_RETENTION_HOURS") or ProjectSettings.get_setting("zeroday/admin/log_retention_hours", 168)

# ---- Database settings ----

static func get_database_url() -> String:
	return _env_str("ZERODAY_DB_URL") or ProjectSettings.get_setting("zeroday/database/url", "jdbc:sqlite:data/zeroday.db")

static func get_database_user() -> String:
	return _env_str("ZERODAY_DB_USER") or ProjectSettings.get_setting("zeroday/database/user", "")

static func get_database_password() -> String:
	return _env_str("ZERODAY_DB_PASSWORD") or ProjectSettings.get_setting("zeroday/database/password", "")

static func get_database_max_pool_size() -> int:
	return ProjectSettings.get_setting("zeroday/database/max_pool_size", 10)

static func get_database_connection_timeout() -> int:
	return ProjectSettings.get_setting("zeroday/database/connection_timeout", 30000)

# ---- Helpers ----

static func _env_str(key: String) -> String:
	var val = OS.get_environment(key)
	if val != null and not val.strip_edges().is_empty():
		return val.strip_edges()
	return ""

static func _env_int(key: String) -> int:
	var val = OS.get_environment(key)
	if val != null and val.strip_edges().is_valid_int():
		return val.strip_edges().to_int()
	return 0
