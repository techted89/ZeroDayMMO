extends Control

# GDScript Skill Tree UI
# Displays skill tree and progression

signal skill_tree_ready()

var _skill_tree: SkillTree
var _skill_points_label: Label
var _skill_level_label: Label

func _ready():
    _setup_ui()

func _setup_ui():
    # Create skill points label
    _skill_points_label = Label.new()
    _skill_points_label.text = "Skill Points: 0"
    _skill_points_label.position = Vector2(100, 100)
    add_child(_skill_points_label)
    
    # Create skill level label
    _skill_level_label = Label.new()
    _skill_level_label.text = "Level: 1"
    _skill_level_label.position = Vector2(100, 150)
    add_child(_skill_level_label)
    
    # Create skill tree node
    var tree_node = TreeNode.new()
    tree_node.position = Vector2(50, 50)
    add_child(tree_node)
    
    # Connect signals
    _skill_tree.connect("skill_activated", _on_skill_activated)
    _skill_tree.connect("skill_level_up", _on_skill_level_up)
    _skill_tree.connect("skill_points_changed", _on_skill_points_changed)
    emit_signal("skill_tree_ready")

func _on_skill_activated(skill_name: String) -> void:
    print("Skill activated: %s" % skill_name)
    
    # Update UI
    ui.append_terminal_output("Activated: %s" % skill_name, "#00FF00")

func _on_skill_level_up(skill_name: String, level: int) -> void:
    print("Skill level up: %s to level %d" % (skill_name, level))
    
    # Update UI
    _skill_level_label.text = "Level: %d" % level
    ui.append_terminal_output("Level up: %s to level %d" % (skill_name, level), "#00FF00")

func _on_skill_points_changed(points: int) -> void:
    _skill_points_label.text = "Skill Points: %d" % points
    
    # Update UI
    ui.append_terminal_output("Skill Points: %d" % points, "#FFFF00")

func show_skill_tree() -> void:
    _skill_tree = SkillTree.new()
    add_child(_skill_tree)