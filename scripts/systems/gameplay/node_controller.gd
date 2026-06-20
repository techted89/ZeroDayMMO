extends Node

# NodeController manages the IP-based world map, node ownership, and capture mechanics.
# Each IP tile can be owned, contested, or neutral, and provides stat bonuses.
#
# Node Types:
# - gateway: movement speed bonus
# - server: XP bonus
# - firewall: defense bonus
# - data: resource bonus
# - proxy: stealth/cooldown bonus
# - core: all stats bonus (rare, high-value)
# - empty: no bonus
#
# Usage: NodeController owns the map grid and handles node capture logic.
# Call claim_node(ip, owner, node_type) to assign ownership.

signal node_owner_changed(ip: String, old_owner: String, new_owner: String)
signal node_contested(ip: String, contestant: String)

@export var node_bonuses: Dictionary = {
	"gateway": {"speed": 5, "movement": 5},
	"server": {"xp": 10, "drop_rate": 5},
	"firewall": {"defense": 10, "shield": 5},
	"data": {"resources": 10, "luck": 5},
	"proxy": {"stealth": 5, "cooldown": 5},
	"core": {"all": 15},
	"empty": {}
}

var owned_nodes: Dictionary = {}  # {ip: {owner, type, last_capture_time}}
var contested_nodes: Dictionary = {}  # {ip: {contestant, progress}}

func _ready() -> void:
	process_mode = PROCESS_MODE_ALWAYS

# Claim a node at the given IP.
# node_type is optional; if omitted, it's auto-assigned based on IP octet.
func claim_node(ip: String, owner: String, node_type: String = "") -> bool:
	# Auto-assign node type based on IP
	if node_type.is_empty():
		node_type = _auto_assign_node_type(ip)
	
	# Determine if this is a valid claim
	if owned_nodes.has(ip) and owned_nodes[ip].owner == owner:
		return false  # Already owned by same player
	
	# Apply ownership
	var old_owner: String = owned_nodes.get(ip, {}).get("owner", "")
	owned_nodes[ip] = {"owner": owner, "type": node_type, "last_capture_time": Time.get_ticks_msec()}
	
	node_owner_changed.emit(ip, old_owner, owner)
	print("[NodeController] Node %s claimed by %s (type: %s)" % [ip, owner, node_type])
	return true

# Start a contested claim (used for PvP capture)
func start_contest(ip: String, contestant: String) -> void:
	contested_nodes[ip] = {"contestant": contestant, "progress": 0.0}
	node_contested.emit(ip, contestant)

# Progress a contested claim
func progress_contest(ip: String, delta: float) -> bool:
	if not contested_nodes.has(ip):
		return false
	var progress: float = contested_nodes[ip].progress + delta
	contested_nodes[ip].progress = progress
	var owner: String = owned_nodes.get(ip, {}).get("owner", "")
	if progress >= 100.0:
		# Capture complete
		claim_node(ip, contested_nodes[ip].contestant)
		contested_nodes.erase(ip)
		return true
	return false

# Get bonuses for a given IP (based on its node type)
func get_node_bonuses(ip: String) -> Dictionary:
	if not owned_nodes.has(ip):
		return {}
	var node_type: String = owned_nodes[ip].type
	return node_bonuses.get(node_type, {})

# Get all nodes owned by a player
func get_player_nodes(owner: String) -> Array[String]:
	var result: Array[String] = []
	for ip in owned_nodes:
		if owned_nodes[ip].owner == owner:
			result.append(ip)
	return result

# Auto-assign node type based on IP octets
func _auto_assign_node_type(ip: String) -> String:
	var octets = ip.split(".").map(func(s): return int(s))
	if octets.size() != 4:
		return "empty"
	# Use last octet to determine type rarity
	var last: int = octets[3]
	# Core nodes are rare (last octet divisible by 50)
	if last % 50 == 0:
		return "core"
	# Gateway nodes near local network
	if octets[0] == 127 or octets[0] == 10 or octets[0] == 192:
		if last < 10:
			return "gateway"
	# Server nodes (middle range)
	if last >= 10 and last < 50:
		return "server"
	# Firewall nodes (middle-high)
	if last >= 50 and last < 100:
		return "firewall"
	# Data nodes
	if last >= 100 and last < 150:
		return "data"
	# Proxy nodes
	if last >= 150 and last < 200:
		return "proxy"
	# Default to empty
	return "empty"