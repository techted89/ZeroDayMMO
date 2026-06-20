extends Node

var _tasks: Array[Dictionary] = []
var _active_task_id: String = ""

signal task_list_updated(tasks: Array)
signal active_task_changed(task_id: String)

func add_task(task_data: Dictionary) -> Dictionary:
	var task = {
		"id": task_data.get("id", "task_" + str(Time.get_ticks_msec())),
		"title": task_data.get("title", "Unknown Task"),
		"description": task_data.get("description", ""),
		"type": task_data.get("type", "generic"),
		"status": task_data.get("status", "available"),
		"reward_credits": task_data.get("reward_credits", 0),
		"reward_xp": task_data.get("reward_xp", 0),
		"progress": task_data.get("progress", 0.0),
		"progress_max": task_data.get("progress_max", 1.0),
		"dependencies": task_data.get("dependencies", []),
		"optional": task_data.get("optional", false),
	}
	_tasks.append(task)
	task_list_updated.emit(_tasks)
	return task

func remove_task(task_id: String) -> bool:
	var idx = _find_task_index(task_id)
	if idx < 0:
		return false
	_tasks.remove_at(idx)
	if _active_task_id == task_id:
		_active_task_id = ""
		active_task_changed.emit("")
	task_list_updated.emit(_tasks)
	return true

func set_active(task_id: String) -> bool:
	if _find_task_index(task_id) < 0:
		return false
	_active_task_id = task_id
	active_task_changed.emit(task_id)
	return true

func update_progress(task_id: String, progress: float) -> bool:
	var idx = _find_task_index(task_id)
	if idx < 0:
		return false
	_tasks[idx].progress = progress
	if progress >= _tasks[idx].progress_max:
		_tasks[idx].status = "completed"
	task_list_updated.emit(_tasks)
	return true

func complete_task(task_id: String) -> bool:
	var idx = _find_task_index(task_id)
	if idx < 0:
		return false
	_tasks[idx].status = "completed"
	_tasks[idx].progress = _tasks[idx].progress_max
	task_list_updated.emit(_tasks)
	return true

func get_active_task() -> Dictionary:
	if _active_task_id.is_empty():
		return {}
	for task in _tasks:
		if task.id == _active_task_id:
			return task
	return {}

func get_tasks() -> Array[Dictionary]:
	return _tasks.duplicate()

func get_tasks_by_status(status: String) -> Array[Dictionary]:
	var result: Array[Dictionary] = []
	for task in _tasks:
		if task.status == status:
			result.append(task)
	return result

func get_tasks_by_type(type: String) -> Array[Dictionary]:
	var result: Array[Dictionary] = []
	for task in _tasks:
		if task.type == type:
			result.append(task)
	return result

func has_task(task_id: String) -> bool:
	return _find_task_index(task_id) >= 0

func clear():
	_tasks.clear()
	_active_task_id = ""
	task_list_updated.emit([])
	active_task_changed.emit("")

func _find_task_index(task_id: String) -> int:
	for i in _tasks.size():
		if _tasks[i].id == task_id:
			return i
	return -1
