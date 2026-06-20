extends Node

# Manages spawning of new players based on local network IP.
# New players appear near the requesting player's local subnet.
# 
# Features:
# - Spawns at nearest safe node within the player's home /24 subnet.
# - Teleports new player to that IP via the NetworkManager or GameManager.
# - Generates a random starting node claim (gateway node) and informs the client.
# 
# Usage: call PlayerSpawnManager.spawn_player(username, target_ip=null) to create a new player.

@onready var node_controller: Node = null
@onready var world_root: Node = get_tree().get_root().get_node_or_null("Main2D")

func _ready() -> void:
	process_mode = PROCESS_MODE_ALWAYS
	# Find NodeController if needed
	if world_root:
		var potential = world_root.get_node_or_null("NodeController")
		if potential:
			node_controller = potential

# Spawn a new player with the given username.
# target_ip can be used for custom location (usually null to auto‑detect).
func spawn_player(username: String, target_ip: String = "") -> void:
	# Determine home subnet based on existing player or the one who triggered spawn.
	var source_ip: String = ""
	if world_root:
		var any_player = world_root.get_node_or_null("PlayerSpawner")
		if any_player and any_player.has_node("ZeroDayCore"):
			var core = any_player.get_node("ZeroDayCore") as Object
			if core and hasattr(core, "current_player") and core.current_player:
				source_ip = core.current_player.current_ip
		}
	
	# Fallback to localhost for initial tutorial spawns
	if source_ip.is_empty():
		source_ip = "127.0.0.1"
	
	# Compute a nearby IP for the new player.
	var new_ip: String = _compute_spawn_ip(source_ip, target_ip)
	
	# Create a new player object (reuse existing PlayerData logic).
	# Since this is the client side, we simulate a new player locally.
	var new_player_data: Dictionary = {
		"username": username,
		"level": 1,
		"experience": 0,
		"cpu": 30,
		"max_cpu": 30,
		"ram": 256,
		"max_ram": 256,
		"bandwidth": 10,
		"max_bandwidth": 10,
		"credits": 100,
			"reputation": 0,
		"current_ip": new_ip,
		"unlocked_commands": ["travel", "scan_ip", "build_base"],
		"active_tasks": [],
		"completed_tasks_count": 0,
		"experience_to_next": 100
	}
	
	# Update the client‑side state via ZeroDayCore.
	var core: Object = get_tree().get_root().get_node_or_null("ZeroDayCore")
	if core and hasattr(core, "update_player"):
		core.update_player(new_player_data)
		# Emit a notification to UI that a new player joined.
		if hasattr(core, "add_notification"):
			core.add_notification("SYSTEM", "New Player", "%s spawned at %s" % [username, new_ip])
		
	# Request the server (if connected) to create a matching player entry.
	# This is a stub – actual networking would be handled by NetworkManager.
	# For now we just log the spawn.
	print("[PlayerSpawnManager] New player '%s' spawned at IP %s" % [username, new_ip])
	
	# Claim a gateway node automatically for the newcomer (if possible).
	if node_controller and hasattr(node_controller, "claim_node"):
		node_controller.claim_node(new_ip, username, "gateway")

# Compute a safe spawn IP near source_ip.
# Heuristic: stay within the same /24 subnet and pick an unclaimed node IP.
func _compute_spawn_ip(source_ip: String, target_ip: String) -> String:
	if not target_ip.is_empty():
		return target_ip
	# Parse source IP into octets
	var octets = source_ip.split(".")
	if octets.size() != 4:
		return "127.0.0.2"  # fallback
	# Keep first three octets, try last octet 1‑254, avoid .0 (network) and .255 (broadcast)
	var base_ip: String = "%s.%s.%s" % [octets[0], octets[1], octets[2]]
	for i in range(1, 255):
		var candidate = "%s.%s" % [base_ip, str(i)]
		# In a real implementation you would query the server if candidate is available.
		# Stub: just pick an IP in the range.
		if i != 1:  # skip the original 127.0.0.1 (tutorial room) for now
			return candidate
	return "%s.2" % base_ip  # last fallback