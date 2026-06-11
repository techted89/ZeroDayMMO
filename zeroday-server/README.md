# ZeroDay: Hack MMORPG

An online multiplayer hacking simulator with simulated CLI, player leveling, storyline progression, network exploration, and cooperative/competitive task systems.

## Architecture

### Backend (Kotlin + Ktor)
```
zeroday-server/
├── build.gradle.kts          # Gradle build config
├── src/main/kotlin/com/zeroday/
│   ├── ZeroDayServer.kt      # Entry point, Ktor server setup, DI wiring
│   ├── config/ServerConfig.kt
│   ├── protocol/             # Wire format & typed request/response names
│   │   └── MessageProtocol.kt
│   ├── model/
│   │   ├── Player.kt         # Player state, snapshot
│   │   ├── Command.kt        # CLI command registry (31 commands)
│   │   ├── Event.kt          # Storyline events (7 storylines)
│   │   ├── Task.kt           # Task/contract system (15 templates)
│   │   ├── NetworkNode.kt    # Network topology (24 nodes)
│   │   ├── Achievement.kt    # Achievement catalog + progress
│   │   ├── Notification.kt   # Bounded inbox (cap=100)
│   │   ├── Challenge.kt      # Daily/weekly challenges
│   │   └── Skill.kt          # Skill tree (4 trees, 12 skills, 3 tiers)
│   ├── service/              # Game logic, no transport awareness
│   │   ├── PlayerService.kt
│   │   ├── CommandService.kt
│   │   ├── EventService.kt
│   │   ├── TaskService.kt
│   │   ├── NetworkService.kt
│   │   ├── FactionService.kt
│   │   ├── ResearchService.kt
│   │   ├── ContractGenerator.kt
│   │   ├── HackToLearnService.kt
│   │   ├── WorldEventService.kt
│   │   ├── AdRewardService.kt
│   │   ├── AchievementService.kt
│   │   ├── NotificationService.kt
│   │   ├── ChallengeService.kt
│   │   ├── SkillService.kt
│   │   ├── GameEventBus.kt           # In-process pub/sub for game events
│   │   ├── PlayerPersistence.kt      # Atomic JSON snapshot to disk
│   │   ├── ResourceRegenTicker.kt    # Single-tick resource regen
│   │   └── ServerStats.kt            # /stats response shape
│   ├── handler/              # Transport (WebSocket) layer
│   │   ├── WebSocketHandler.kt   # Slim per-connection lifecycle
│   │   ├── MessageRouter.kt      # O(1) dispatch, idempotency, rate-limit
│   │   ├── MessageHandler.kt     # Handler interface
│   │   ├── HandlerContext.kt     # Per-message context
│   │   ├── ServiceRegistry.kt    # Single DI bag for handlers
│   │   ├── ConnectionRegistry.kt # Sessions, auth, broadcast, per-IP cap
│   │   ├── JsonExt.kt            # Payload extraction helpers
│   │   └── handlers/             # One file per domain
│   │       ├── AuthHandler.kt
│   │       ├── PlayerHandler.kt
│   │       ├── CommandHandler.kt
│   │       ├── StorylineHandler.kt
│   │       ├── TaskHandler.kt
│   │       ├── NetworkHandler.kt
│   │       ├── FactionHandler.kt
│   │       ├── ResearchHandler.kt
│   │       ├── NexusHandler.kt
│   │       ├── KMapHandler.kt
│   │       ├── WorldEventHandler.kt
│   │       ├── AdHandler.kt
│   │       ├── AchievementHandler.kt
│   │       ├── ChallengeHandler.kt
│   │       ├── NotificationHandler.kt
│   │       ├── SkillHandler.kt
│   │       └── ProfileHandler.kt
│   ├── security/
│   │   ├── PasswordHasher.kt     # BCrypt (was plaintext!)
│   │   ├── BcryptPasswordHasher.kt
│   │   ├── RateLimiter.kt        # Sliding-window in-memory limiter
│   │   ├── InputValidation.kt    # Username/password/text rules
│   │   └── AuditLog.kt           # Structured security-event logger
│   ├── util/AppScope.kt          # Application-wide SupervisorJob scope
│   └── zdscript/                 # Nexus scripting language
│       ├── ZDScriptModels.kt
│       └── ZDScriptEngine.kt
└── src/test/kotlin/com/zeroday/  # Unit tests
    ├── security/
    ├── service/
    └── handler/
```

### Client (Unity C#)
```
zeroday-client/Assets/Scripts/
├── Managers/
│   ├── GameManager.cs        # Game state, event coordination
│   ├── NetworkManager.cs     # WebSocket client, message queue
│   └── UIManager.cs          # UI routing hub
├── Terminal/
│   ├── TerminalCore.cs       # Terminal emulator with scrolling, history, colors
│   └── TerminalParser.cs     # Command parsing with flags/args
├── Player/
│   └── SkillTree.cs          # 18 skills across 6 categories
├── Network/
│   └── NetworkMap.cs         # Visual node map with connections
├── Events/
│   ├── EventSystem.cs        # Dynamic event notifications
│   └── TaskController.cs     # Task list, details, accept/complete
├── UI/
│   ├── HUDController.cs      # Player stats, resources, tasks overlay
│   ├── LoginPanel.cs         # Auth panel
│   ├── FactionPanel.cs       # Faction guild management
│   ├── ResearchLab.cs        # Zero-Day crafting lab
│   ├── NexusScriptEditor.cs  # NexusScript code editor
│   └── KnowledgeMap.cs       # K-Map fragment discovery tree
└── Network/
    └── NetworkMap.cs         # Visual node map with connections
```

### Backend architecture notes

- **Message protocol** lives in `protocol/MessageProtocol.kt`. The `RequestTypes` and `ResponseTypes` objects are the single source of truth for the wire strings; nothing else in the codebase should hard-code them.
- **Handlers are small.** Each domain (auth, tasks, factions, …) has its own `MessageHandler` implementation in `handler/handlers/`. Adding a new request type is a one-line change to that handler's `handledTypes` set.
- **Router is O(1).** `MessageRouter` builds a `Map<type, handler>` at construction time. No reflection, no string-`when` chains.
- **No more god class.** The original `WebSocketHandler` was 646 lines of routing + business logic. It is now a slim connection-lifecycle class that delegates every message to the router.
- **DI is a single bag.** `ServiceRegistry` is the only thing handlers take; this keeps handler constructors small and trivial to mock in tests.
- **Per-connection state is explicit.** `ConnectionRegistry` tracks sessions, authenticated player ids, and per-connection coroutine jobs (resource regen). Closing a session cancels its jobs and unregisters it in one place.
- **No coroutine leaks.** The resource-regeneration loop is now `services.appScope.launch` with the `Job` stored on the `ConnectionEntry`, so it gets cancelled on disconnect.
- **Passwords are hashed.** `PlayerService` requires a `PasswordHasher`; `BcryptPasswordHasher` is wired in production, but the interface makes it trivial to swap in a fake for tests.
- **Auth is rate-limited.** `RateLimiter` is a sliding-window in-memory limiter used by `AuthHandler` for both register and login.

### Production-readiness layer

- **Defense in depth.** The router and registry apply multiple, independent safeguards:
  - `MessageRouter` enforces a per-player command rate limit (30 req / 1 s) and a hard 64 KB cap on inbound text frames. Oversized frames close the socket with `TOO_BIG`.
  - `ConnectionRegistry` enforces a global cap (`MAX_CONNECTIONS=2000`) and a per-IP cap (`MAX_CONNECTIONS_PER_IP=8`). Excess connections are rejected with `TRY_AGAIN_LATER` *before* they reach a handler.
  - `AuthHandler` runs a 5-req-per-minute limiter keyed by IP+username, separate from the gameplay limiter.
  - `InputValidation` rejects bad usernames/passwords *before* the rate limiter so a script spraying "abc" as username doesn't burn our budget.
  - `MessageRouter` supports a `requestId` field on the envelope for **idempotent retries**: replays inside a 30 s window return the cached response without re-running the handler.
- **Cryptographic session IDs.** `ConnectionRegistry` issues 24-byte `SecureRandom` tokens base64url-encoded — ~192 bits of entropy, not derivable from any player id.
- **Audit log.** Every login attempt, registration, and rate-limit hit goes through `AuditLog` to a dedicated "AUDIT" logger (route it to its own file/syslog appender in `logback.xml`).
- **Player persistence.** `PlayerPersistence` writes the player table to `data/players.json` every 60 s, atomically via temp-file + rename. On boot, `runBlocking { restore() }` rehydrates the table. Transient fields (`isOnline`, `lastLevelNotified`) are reset on restore.
- **Operational endpoints.** `GET /health` (liveness), `GET /stats` (online players, total connections, last snapshot info).
- **Game event bus.** `GameEventBus` is a fire-and-forget pub/sub where listeners run **concurrently** in their own coroutines — a slow listener cannot stall the producer or other listeners. The lock is held only long enough to snapshot the listener list.
- **In-process notification inbox.** `NotificationService` keeps a per-player ring buffer (default 100 entries) so the client can render an unread badge. Oldest *read* entries are dropped first; unread entries are never silently lost.

### Throughput / latency optimisations

- **Per-player locks.** `PlayerService` uses a `ConcurrentHashMap` of `Mutex` keyed by player id, not a single global mutex. Two players' mutations now run in parallel — contention dropped from O(players²) to O(1) per operation.
- **Single-tick resource regen.** `ResourceRegenTicker` is one background coroutine that calls `regenerateAllOnline()` every 5 s, instead of N coroutines (one per online player) each with their own `delay` loop. Removes N timer wakeups per cycle.
- **Coalesced broadcasts.** `ConnectionRegistry.broadcast` encodes the JSON once and dispatches the same `Frame.Text` in parallel coroutines — a slow client cannot block the others.
- **Snapshot cache.** `PlayerService.getSnapshot` returns a cached `PlayerSnapshot` for [snapshotTtlMs] (default 250 ms) on hot paths like `/health` and leaderboard queries; the cache is invalidated on every mutation.
- **Coalesced notifications.** When multiple achievements or challenges complete in the same `GameEvent`, the wiring pushes a *single* notification with a `count` field instead of one per unlock. `NotificationService.pushCoalescing` is the lower-level primitive for ad-hoc debouncing.
- **Optimised reads.** `getOnlinePlayers` is a non-locking `ConcurrentHashMap` iteration; the new `getOnlinePlayersPage(offset, limit)` returns a slice for leaderboard queries. `getAllPlayers` no longer takes a mutex.
- **Username index.** `usernameIndex: ConcurrentHashMap<String, String>` makes `getPlayerByUsername` an O(1) lookup instead of a full-table scan, which is critical for `/login` throughput.

## Features

### Player Leveling System
- XP earned from tasks, storyline completion, and events
- Level-based unlocks: higher levels grant access to new commands and network segments
- Resource regeneration (CPU, RAM, Bandwidth) over time

### CLI Command System (31 Commands)
- **Basic (5):** help, scan, connect, whoami, status
- **Recon (4):** nmap, traceroute, whois, sniff
- **Network (5):** ifconfig, ping, ssh, ftp, botnet, overload
- **System (2):** ls, cat, ai-assist
- **Exploit (6):** exploit, bruteforce, sqlmap, backdoor, worm, rootkit, dnshijack, zero-day
- **Defense (4):** firewall, trace, honeypot
- **Stealth (3):** spoof, proxy
- **Crypto (3):** decrypt, encrypt, crack

### Storyline Progression (7 Storylines)
1. **Welcome to the Matrix** (Lv.1) - Basic terminal use
2. **Network Horizons** (Lv.2) - Reconnaissance
3. **First Blood** (Lv.4) - Exploitation basics
4. **The Darknet** (Lv.6) - Task marketplace access
5. **Cryptographic Shadows** (Lv.8) - Crypto/stealth
6. **Advanced Persistent Threat** (Lv.10) - Backdoors, worms
7. **Elite Status** (Lv.14) - Botnets, zero-days, AI

### Network Simulation (24 Nodes)
- Realistic topology: localhost → corporate → darknet → military → satellite
- 8 node types: Router, Server, Workstation, Database, Firewall, Darknet, IoT, Satellite
- Vulnerabilities on each node for exploitation
- Network discovery through storyline progression

### Task System (15 Templates x 6 Difficulties)
- **Difficulties:** Trivial, Easy, Medium, Hard, Expert, Legendary
- **Types:** Penetration Test, Data Theft, Service Disruption, Defense, Crypto, Social Engineering, Botnet, Recon, Forensics, Bug Bounty
- Solo or party play (up to 4 players, 1.2x XP bonus)
- Dynamic task generation based on player level

### Resource System
- CPU: Consumed by command execution (5-60 per command)
- RAM: Memory for running operations
- Bandwidth: Network throughput
- Credits: Currency from tasks
- Reputation: Social standing for darknet access

## Setup

### Backend
```bash
cd /root/ZeroDayMMO/zeroday-server
./gradlew run
```
Server starts on `ws://localhost:8080/game`

### Client (Unity)
1. Open `/root/ZeroDayMMO/zeroday-client` in Unity (2022.3+)
2. Install TextMeshPro if needed
3. Install Newtonsoft.Json via Package Manager
4. Set the server URL in NetworkManager.cs
5. Run the scene

## WebSocket API

| Type | Payload | Response |
|------|---------|----------|
| `register` | `{username, password}` | `register_success` |
| `login` | `{username, password}` | `login_success` |
| `command` | `{input}` | `command_result` |
| `get_status` | `{}` | `status` |
| `get_storylines` | `{}` | `storylines` |
| `start_storyline` | `{storyline_id}` | `storyline_started` |
| `advance_story` | `{}` | `story_advanced` |
| `get_tasks` | `{}` | `tasks` |
| `accept_task` | `{task_instance_id}` | `task_accepted` |
| `complete_task` | `{task_instance_id}` | `task_completed` |
| `get_network` | `{}` | `network` |
| `scan_subnet` | `{subnet}` | `subnet_scanned` |
| `get_leaderboard` | `{}` | `leaderboard` |
