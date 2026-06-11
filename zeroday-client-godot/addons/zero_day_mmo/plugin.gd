@tool
extends EditorPlugin

var dock: Control

func _enter_tree():
	print("[ZeroDayMMO Plugin] Autoloads registered via project.godot")

func _exit_tree():
	print("[ZeroDayMMO Plugin] Autoloads managed by project.godot")
