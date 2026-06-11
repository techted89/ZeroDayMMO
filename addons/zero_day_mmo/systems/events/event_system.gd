extends Node

signal player_logged_in(player_data: Dictionary)
signal player_logged_out
signal player_leveled_up(new_level: int, stat_points: int)
signal player_credits_changed(new_balance: int, delta: int)
signal player_resources_changed(cpu: float, ram: float, bandwidth: float)
signal player_hp_changed(current_hp: float, max_hp: float)
signal player_xp_changed(current_xp: float, xp_to_next: float)
signal combat_hit(target_id: String, damage: int, is_critical: bool)
signal combat_heal(target_id: String, amount: int)
signal command_received(command: String, args: Array[String])
signal command_result(result: Dictionary)
signal server_message(message: String, message_type: String)
signal connection_state_changed(state: String, detail: String)
signal task_updated(task_id: String, status: String, progress: float)
signal task_completed(task_id: String, reward: Dictionary)
signal inventory_updated(items: Array)
signal notification_received(title: String, message: String, notification_type: String)
signal error_occurred(code: String, message: String)

func clear():
	var sigs = [
		player_logged_in, player_logged_out, player_leveled_up,
		player_credits_changed, player_resources_changed, player_hp_changed,
		player_xp_changed, combat_hit, combat_heal, command_received,
		command_result, server_message, connection_state_changed,
		task_updated, task_completed, inventory_updated,
		notification_received, error_occurred
	]
	for sig in sigs:
		for conn in sig.get_connections():
			sig.disconnect(conn.callable)
