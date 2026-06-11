# ZeroDayMMO — Agent Guide

## Project Structure

```
zeroday-server/     Kotlin 17 + Ktor 2.3.7 WebSocket game server (Gradle 7.6)
zeroday-client-godot/  Godot 4.6 + C# (.NET 8.0) game client
config/             Server env config + startup script
vision-tool/        Vendored external MCP vision tool (not core game)
```

## Server

**Entrypoint:** `com.zeroday.ZeroDayServerKt` in `zeroday-server/src/main/kotlin/com/zeroday/ZeroDayServer.kt`

**Key commands (run from `zeroday-server/`):**
- `./gradlew run` — start dev server on `ws://localhost:8080/game`
- `./gradlew clean jar --no-daemon` — build fat JAR to `build/libs/zeroday-server.jar`
- `./run-local.sh` — build + run fat JAR locally (no Docker)
- `./deploy.sh` — build + deploy (mode: `local-docker`, `docker-remote`, or `systemd`)
- `./gradlew test` — run tests
- `docker compose up -d` — Docker container from built JAR

**Architecture:**
- `MessageRouter` builds a `Map<type, handler>` at init — O(1) dispatch, no reflection, no string-`when`
- `ServiceRegistry` is the single DI bag; use `ServiceRegistry.minimal(playerService, scope)` in tests
- One handler class per domain in `handler/handlers/`; add new request type by appending to handler's `handledTypes`
- Wire types in `protocol/MessageProtocol.kt` (both `RequestTypes` and `ResponseTypes`) — single source of truth
- `PlayerPersistence` writes `data/players.json` atomically (tmp + rename) every 60 s
- `ConnectionRegistry.enforces`: 2000 global cap, 8 per IP, 30 req/s gameplay limiter, 64 KB frame cap
- Resource regen is a single ticker (`ResourceRegenTicker`), not N per-connection coroutines
- Config via env vars: `ZERODAY_PORT`, `ZERODAY_DATA_DIR`, etc. (see `config/ServerConfig.kt`)

**Testing:** JUnit with `kotlin.test` assertions. 17 test files. Use `ServiceRegistry.minimal()` for test isolation.

## Client

**Entrypoint:** `res://Scenes/main.tscn` (set in `project.godot`)

**Key commands (run from `zeroday-client-godot/`):**
- `./install_dependencies.sh` — install Godot + .NET
- `./launcher.sh` — launch game
- `dotnet build` — build C# scripts
- Server URL in `server.cfg` (`[connection] url=ws://localhost:8080/game`)

**Rendering:** `gl_compatibility` (OpenGL 3.x / OpenGL ES), 1920x1080, `canvas_items` stretch mode. Clear color: black.

## Protocol

JSON over WebSocket at `ws://<host>:8080/game`.

```json
{"type": "login", "payload": {"username": "h4ck3r", "password": "..."}, "requestId": "abc-123"}
{"type": "login_success", "payload": {"...": "..."}, "timestamp": 1717500000000}
```

All request/response type strings live in `protocol/MessageProtocol.kt`.

**HTTP endpoints:** `GET /` (banner), `/health`, `/live`, `/ready`, `/stats`, `WS /game`.

## Known gotchas

- Server README references Unity (`zeroday-client`) but the actual client is Godot (`zeroday-client-godot`) — do not trust the README file listing over code
- `project.godot` has duplicate key bindings (ui_left/right mapped to both arrows and WASD) and missing `ui_up`/`ui_down` input actions
- `NetworkManager.Connect()` returns immediately; WebSocket is async — account for this in connection logic
- PlayerData properties use mixed casing — verify casing before serialization
- Vision-tool is vendored, not owned — edit its files only if explicitly asked
