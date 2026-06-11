extends Node

signal connected()
signal connection_failed(error_message: String)
signal disconnected(reason: String)
signal message_received(json_string: String)

var _socket: WebSocketPeer = WebSocketPeer.new()
var _message_queue: Array[String] = []
var _mutex: Mutex = Mutex.new()
var _is_connected: bool = false
var _is_connecting: bool = false
var _connection_start_time: float = 0.0
var _pending_url: String = ""
var _reconnect_attempts: int = 0
var max_reconnect_attempts: int = 3
var connection_timeout_ms: float = 10000.0

func _ready():
	process_mode = PROCESS_MODE_ALWAYS

func _process(_delta: float) -> void:
	if _socket == null: return

	_socket.poll()
	var state = _socket.get_ready_state()

	match state:
		WebSocketPeer.STATE_OPEN:
			if _is_connecting:
				_is_connecting = false
				_is_connected = true
				_reconnect_attempts = 0
				connected.emit()
			while _socket.get_available_packet_count() > 0:
				var packet = _socket.get_packet()
				var msg = packet.get_string_from_utf8()
				message_received.emit(msg)
				_mutex.lock()
				_message_queue.append(msg)
				_mutex.unlock()

		WebSocketPeer.STATE_CONNECTING:
			if _is_connecting:
				var elapsed = Time.get_ticks_msec() - _connection_start_time
				if elapsed > connection_timeout_ms:
					_socket.close()
					_is_connecting = false
					_is_connected = false
					connection_failed.emit("Connection timeout after %d seconds" % [int(connection_timeout_ms / 1000.0)])

		WebSocketPeer.STATE_CLOSED:
			if _is_connecting:
				_is_connecting = false
				_is_connected = false
				var code = _socket.get_close_code()
				var reason = _socket.get_close_reason()
				var msg = reason if reason else ("Connection closed with code %d" % code)
				connection_failed.emit(msg)
			elif _is_connected:
				var code = _socket.get_close_code()
				var reason = _socket.get_close_reason()
				_is_connected = false
				disconnected.emit(reason if reason else ("Disconnected (code %d)" % code))

func connect_to_server(url: String) -> int:
	if _is_connecting or _is_connected:
		return OK

	_pending_url = url
	var error = _socket.connect_to_url(url)
	if error == OK:
		_is_connecting = true
		_connection_start_time = Time.get_ticks_msec()
	return error

func send(message: String) -> void:
	if _socket.get_ready_state() != WebSocketPeer.STATE_OPEN: return
	_socket.put_packet(message.to_utf8_buffer())

func get_next_message() -> String:
	_mutex.lock()
	var msg: String = ""
	if _message_queue.size() > 0:
		msg = _message_queue.pop_front()
	_mutex.unlock()
	return msg

func has_message() -> bool:
	_mutex.lock()
	var result = _message_queue.size() > 0
	_mutex.unlock()
	return result

func disconnect_from_server() -> void:
	if _socket and _socket.get_ready_state() == WebSocketPeer.STATE_OPEN:
		_socket.close()
	_is_connected = false
	_is_connecting = false

func send_and_wait(message: Dictionary, timeout_ms: float = 10000.0) -> Dictionary:
	var json_str = JSON.stringify(message)
	send(json_str)
	var start = Time.get_ticks_msec() / 1000.0
	while (Time.get_ticks_msec() / 1000.0) - start < (timeout_ms / 1000.0):
		var msg = get_next_message()
		if msg:
			var json = JSON.new()
			var parse_err = json.parse(msg)
			if parse_err == OK and typeof(json.data) == TYPE_DICTIONARY:
				return json.data
			return {}
		await get_tree().process_frame
	return {}

func login(username: String, password: String) -> Dictionary:
	return await send_and_wait({
		"type": "login",
		"username": username,
		"password": password
	})

func register_player(username: String, password: String) -> Dictionary:
	return await send_and_wait({
		"type": "register",
		"username": username,
		"password": password
	})

func execute_command(input: String) -> Dictionary:
	return await send_and_wait({
		"type": "command",
		"input": input
	})

func get_status() -> Dictionary:
	return await send_and_wait({"type": "get_status"})

func get_storylines() -> Dictionary:
	return await send_and_wait({"type": "get_storylines"})

func start_storyline(storyline_id: String) -> Dictionary:
	return await send_and_wait({
		"type": "start_storyline",
		"storyline_id": storyline_id
	})

func advance_story() -> Dictionary:
	return await send_and_wait({"type": "advance_story"})

func get_tasks() -> Dictionary:
	return await send_and_wait({"type": "get_tasks"})

func accept_task(task_instance_id: String) -> Dictionary:
	return await send_and_wait({
		"type": "accept_task",
		"task_instance_id": task_instance_id
	})

func complete_task(task_instance_id: String) -> Dictionary:
	return await send_and_wait({
		"type": "complete_task",
		"task_instance_id": task_instance_id
	})

func get_network() -> Dictionary:
	return await send_and_wait({"type": "get_network"})

func scan_subnet(subnet: String) -> Dictionary:
	return await send_and_wait({
		"type": "scan_subnet",
		"subnet": subnet
	})
