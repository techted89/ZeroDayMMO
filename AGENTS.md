# ZeroDayMMO — Agent Guide

## Project layout

```
<repo-root>/              Godot 4.6 project root (project.godot is HERE)
├── addons/zero_day_mmo/  GDScript game code (plugin) — NOT C#
├── zero-server/           Kotlin 17 + Ktor 2.3.7 WebSocket server (Gradle 7.6)
├── config/                (empty — config lives in project.godot [zeroday] section)
├── Scenes/main.tscn       Game entrypoint (set in project.godot)
├── Shaders/               CRT terminal shader
├── vision-tool/           Vendored external MCP vision tool (not core game)
├── export_presets.cfg     Linux / Windows / macOS / Web / Android exports
├── data/                  Runtime server data (players.json, zeroday.db)
└── generated_sprites/     Spritesheet JPGs for terminal display
```

Key fact: `zeroday-client-godot/` is empty. `zeroday-server/` does not exist.
The Godot project lives at the repo root; the plugin under `addons/zero_day_mmo/` contains all client logic in GDScript.

## Server (zero-server/)

**Entrypoint:** `com.zeroday.ZeroDayServerKt` in `zero-server/src/main/kotlin/com/zeroday/ZeroDayServer.kt`

**Commands (run from `zero-server/`):**
- `./gradlew run` — dev server on `ws://localhost:8080/game`
- `./gradlew clean jar --no-daemon` — fat JAR to `build/libs/zeroday-server.jar`
- `./run-local.sh` — build + run fat JAR (no Docker)
- `./deploy.sh` — deploy (mode: `local-docker`, `docker-remote`, or `systemd`)
- `./gradlew test` — run tests
- `docker compose up -d` — Docker from built JAR

**Architecture:**
- `MessageRouter` builds `Map<type, handler>` at init — O(1) dispatch, no reflection
- `ServiceRegistry` is the DI bag; use `ServiceRegistry.minimal(playerService, scope)` in tests
- One handler class per domain in `handler/handlers/`; add type by appending to handler's `handledTypes`
- Wire types in `protocol/MessageProtocol.kt` — single source of truth
- `PlayerPersistence` writes `data/players.json` atomically (tmp + rename) every 60 s
- `ConnectionRegistry` enforces: 2000 global cap, 8 per IP, 30 req/s limiter, 64 KB frame cap
- Resource regen is a single ticker (`ResourceRegenTicker`)
- Config: env vars + `ServerConfig.kt`. Also settable via `project.godot` `[zeroday]` section defaults

**Testing:** JUnit + `kotlin.test`. Use `ServiceRegistry.minimal()` for test isolation.

## Client (GDScript in addons/zero_day_mmo/)

**Entrypoint:** `res://Scenes/main.tscn`

**Autoloads** (global singletons registered in `project.godot`):
- `ZeroDayCore` — game state, player data signals
- `NetworkManager` — WebSocketPeer connection
- `GameManager` — command execution, login flow
- `UIManager` — UI routing
- `ServerConfig` — URL resolution
- `TerminalParser` — command parsing
- `EnvironmentVariables` — env var overrides

**Key signals on ZeroDayCore:** `player_updated`, `login_successful`, `connection_state_changed`, `level_up`, `command_executed`

**Key directories:**
- `core/` — singletons, data_classes.gd (PlayerData, GameState enum)
- `core/ui/` — SkillTreeUI
- `systems/terminal/` — terminal_core, terminal_parser, autocomplete
- `systems/effects/` — screen_effects, digital_rain, combat_fx
- `systems/events/` — event_system, task_controller
- `systems/ui/` — hud_controller, login_panel, notification_center, side_panel
- `ui/` — InventoryPanel

**PlayerData** (GDScript Dictionary-backed class in `data_classes.gd`): uses snake_case properties (`max_cpu`, `unlocked_commands`, `experience_to_next`). Always deserialize server responses using `PlayerData.new(data)`.

**Network:** `NetworkManager` uses GDScript `WebSocketPeer` with signal-based connection callbacks (`connected`, `connection_failed`, `disconnected`, `message_received`). Connection is async — listen for `connected` signal rather than polling.

**Rendering:** `gl_compatibility` (OpenGL 3.x / OpenGL ES), 1920x1080, `canvas_items` stretch. Clear color: black. The `node25d` plugin is enabled.

**Input actions:** All defined in root `project.godot` `[input]` section: `ui_accept`, `ui_cancel`, `ui_left`/`ui_right`/`ui_up`/`ui_down` (arrows + WASD), `mobile_mode`, `mobile_touch`.

## Protocol

JSON over WebSocket at `ws://<host>:8080/game`.
Request/response type strings defined in `zero-server/.../protocol/MessageProtocol.kt`.

```json
{"type": "login", "payload": {"username": "h4ck3r", "password": "..."}, "requestId": "abc-123"}
{"type": "login_success", "payload": {"...": "..."}, "timestamp": 1717500000000}
```

**HTTP endpoints:** `GET /` (banner), `/health`, `/live`, `/ready`, `/stats`, `WS /game`.

## Gotchas

- `zeroday-client-godot/` is empty — the actual client plugin is `addons/zero_day_mmo/`
- Server README references Unity (`zero-client/`) but the actual client is Godot — trust `project.godot`, not README
- `zero-client/` and `zero-admin/` exist at root but are legacy/unused
- Only one scene exists (`Scenes/main.tscn`) — new gameplay scenes must be created
- `TO-DO.md` is empty; `README.md` is blank
- Vision-tool is vendored — edit its files only if explicitly asked
- `export_presets.cfg` has presets for Linux, Windows, macOS, Web, Android
- The `ZeroDayMMO.csproj` (Godot.NET.Sdk) at root has no `.cs` source files — game is pure GDScript
