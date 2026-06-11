class_name ZeroDayData

enum GameState {
	DISCONNECTED,
	CONNECTING,
	LOGIN,
	TERMINAL,
	LOADING
}

class PlayerData:
    var id: String
    var username: String
    var level: int
    var experience: int
    var experience_to_next: int
    var cpu: int
    var max_cpu: int
    var ram: int
    var max_ram: int
    var bandwidth: int
    var max_bandwidth: int
    var credits: int
    var reputation: int
    var unlocked_commands: Array[String]
    var inventory: Array[InventoryItemData]
    var skill_points: int
    
    # Inventory methods
    func equip_item(item_name: String) -> void:
        print("Equipping item: %s" % item_name)
        
    func use_item(item_name: String) -> void:
        print("Using item: %s" % item_name)
        
    # Skill methods
    func gain_skill_points(points: int) -> void:
        skill_points += points
        print("Gained %d skill points" % points)
	var discovered_nodes_count: int
	var active_tasks: Array[TaskData]
	var completed_tasks_count: int
	var current_storyline: String
	var storyline_progress: int
	var faction_id: String
	var inventory: Array[InventoryItemData]
	var active_zero_day_exploits: int
	var firewall_boost: int
	var world_event_participation: int
	var unlocked_achievement_count: int
	var skill_points: int
	var unlocked_skill_count: int
	var unread_notifications: int
	var active_challenge_count: int
	var career_path: String
	var heat_level: int
	var notoriety: int
	var justice_points: int
	var bounty_price: int
	var current_zone_id: String
	var breakthrough_multiplier: float
	var login_streak: int
	var total_logins: int

	func _init(data: Dictionary = {}):
		id = data.get("id", "")
		username = data.get("username", "")
		level = data.get("level", 1)
		experience = data.get("experience", 0)
		experience_to_next = data.get("experience_to_next", 100)
		cpu = data.get("cpu", 100)
		max_cpu = data.get("max_cpu", 100)
		ram = data.get("ram", 128)
		max_ram = data.get("max_ram", 128)
		bandwidth = data.get("bandwidth", 50)
		max_bandwidth = data.get("max_bandwidth", 50)
		credits = data.get("credits", 0)
		reputation = data.get("reputation", 0)
		prestige_level = data.get("prestige_level", 0)
		prestige_points = data.get("prestige_points", 0)
		unlocked_commands = data.get("unlocked_commands", [])
		discovered_nodes_count = data.get("discovered_nodes_count", 0)
		completed_tasks_count = data.get("completed_tasks_count", 0)
		current_storyline = data.get("current_storyline", "")
		storyline_progress = data.get("storyline_progress", 0)
		faction_id = data.get("faction_id", "")
		active_zero_day_exploits = data.get("active_zero_day_exploits", 0)
		firewall_boost = data.get("firewall_boost", 0)
		world_event_participation = data.get("world_event_participation", 0)
		unlocked_achievement_count = data.get("unlocked_achievement_count", 0)
		skill_points = data.get("skill_points", 0)
		unlocked_skill_count = data.get("unlocked_skill_count", 0)
		unread_notifications = data.get("unread_notifications", 0)
		active_challenge_count = data.get("active_challenge_count", 0)
		career_path = data.get("career_path", "")
		heat_level = data.get("heat_level", 0)
		notoriety = data.get("notoriety", 0)
		justice_points = data.get("justice_points", 0)
		bounty_price = data.get("bounty_price", 0)
		current_zone_id = data.get("current_zone_id", "")
		breakthrough_multiplier = data.get("breakthrough_multiplier", 1.0)
		login_streak = data.get("login_streak", 0)
		total_logins = data.get("total_logins", 0)
    if data.has("inventory"):
        for item in data.inventory:
            inventory.append(InventoryItemData.new(item))
    
    # Initialize inventory slots
    for i in range(9):
        inventory.append(InventoryItemData.new({"name": "Empty Slot %d" % (i + 1), "quantity": 0}))
    
    # Initialize skill points
    skill_points = 0

	func regenerate_resources():
		cpu = mini(cpu + 2, max_cpu)
		ram = mini(ram + 4, max_ram)

	func get_level_progress() -> float:
		if experience_to_next > 0:
			return float(experience) / float(experience_to_next)
		return 0.0

class TaskData:
	var task_id: String
	var instance_id: String
	var title: String
	var description: String
	var difficulty: String
	var target_type: String
	var target_ip: String
	var objective: String
	var time_limit_ms: int
	var status: String

	func _init(data: Dictionary = {}):
		task_id = data.get("task_id", "")
		instance_id = data.get("instance_id", "")
		title = data.get("title", "")
		description = data.get("description", "")
		difficulty = data.get("difficulty", "")
		target_type = data.get("target_type", "")
		target_ip = data.get("target_ip", "")
		objective = data.get("objective", "")
		time_limit_ms = data.get("time_limit_ms", 0)
		status = data.get("status", "")

class InventoryItemData:
	var id: String
	var type: String
	var name: String
	var description: String
	var quantity: int
	var rarity: String
	var data: Dictionary

	func _init(d: Dictionary = {}):
		id = d.get("id", "")
		type = d.get("type", "")
		name = d.get("name", "")
		description = d.get("description", "")
		quantity = d.get("quantity", 1)
		rarity = d.get("rarity", "common")
		data = d.get("data", {})

class NotificationEntry:
	var type: String
	var title: String
	var description: String
	var timestamp: float
	var is_read: bool
	var callback: Callable

	func _init(t: String, ti: String, desc: String, cb: Callable = Callable()):
		type = t
		title = ti
		description = desc
		timestamp = Time.get_unix_time_from_system()
		is_read = false
		callback = cb
