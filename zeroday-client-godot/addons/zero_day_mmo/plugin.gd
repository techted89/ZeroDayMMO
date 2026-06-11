@tool
extends EditorPlugin

var dock: Control

func _enter_tree():
	add_autoload_singleton("ZeroDayCore", "res://addons/zero_day_mmo/core/zero_day_core.gd")
	add_autoload_singleton("NetworkManager", "res://addons/zero_day_mmo/core/network_manager.gd")
	add_autoload_singleton("GameManager", "res://addons/zero_day_mmo/core/game_manager.gd")
	add_autoload_singleton("UIManager", "res://addons/zero_day_mmo/core/ui_manager.gd")
	add_autoload_singleton("ServerConfig", "res://addons/zero_day_mmo/core/server_config.gd")
	add_autoload_singleton("TerminalParser", "res://addons/zero_day_mmo/systems/terminal/terminal_parser.gd")
	print("[ZeroDayMMO Plugin] Registered 6 autoload singletons")

func _exit_tree():
	remove_autoload_singleton("ZeroDayCore")
	remove_autoload_singleton("NetworkManager")
	remove_autoload_singleton("GameManager")
	remove_autoload_singleton("UIManager")
	remove_autoload_singleton("ServerConfig")
	remove_autoload_singleton("TerminalParser")
	print("[ZeroDayMMO Plugin] Removed autoload singletons")
