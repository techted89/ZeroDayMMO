# ZeroDayMMO - Game Specification

## Project Overview

ZeroDayMMO is a massively multiplayer online hacking simulator where players interact with a cyberpunk-themed world through a realistic CLI terminal interface. The game features an IPv4-based world system (4.3 billion addresses as claimable tiles), skill-based progression, faction warfare, and cooperative/competitive gameplay.

**Tech Stack:**
- **Client**: Godot 4.x with C# (.NET 8.0)
- **Server**: Kotlin + Ktor (WebSocket-based)
- **Protocol**: JSON over WebSocket

---

## Current State Assessment

### Implemented Systems

#### Client (Godot C#)
- Terminal interface with typewriter effects, CRT shader, command history
- Player movement with sprite animations (8 animation states)
- IPv4 world visualization system
- Hacking terminal simulation (25+ commands)
- Network scanner and exploit database
- UI systems: HUD, Login, Inventory, Auction House, Factions, Research Lab, Knowledge Map, Nexus Script Editor, Notifications, Daily Rewards, Boss Fight Panel
- Game modes: Arena, Defense, Career (White Hat/Black Hat)
- Equipment system with rarity tiers
- World map with zone travel and quests
- Hideout/base building system
- Script chain automation system
- Visual effects: Digital rain, screen shake, combat FX, level-up FX

#### Server (Kotlin)
- WebSocket server with O(1) message routing
- 31 CLI commands across 8 categories
- 7 storylines with level-gated progression
- 24-node network topology
- Task/contract system (15 templates x 6 difficulties)
- Faction system with territory control
- Research tree
- Achievement system
- Challenge system (daily/weekly)
- Skill tree (4 trees, 12 skills, 3 tiers)
- Player persistence (atomic JSON snapshots)
- Security: BCrypt passwords, rate limiting, input validation, audit logging
- Admin panel with ban/cheat detection
- Auction house service
- Coop boss service
- Career system (White Hat/Black Hat paths)
- World zone service with travel mechanics
- Heat/cascade system for criminal actions

### Identified Issues

#### Critical
1. **Client-Server Protocol Mismatch**: Client and server use different command sets and message formats
2. **Dual Client Confusion**: Server README references Unity client (`zeroday-client`) but actual client is Godot (`zeroday-client-godot`)
3. **Missing Input Mappings**: `project.godot` lacks input action definitions (ui_right, ui_left, etc.)
4. **Async Connection Bug**: `NetworkManager.Connect()` returns immediately but WebSocket connection is asynchronous

#### High Priority
5. **PlayerData Property Inconsistency**: Mixed casing (`Level`/`level`, `Username`/`username`)
6. **Incomplete System Integration**: Many systems exist but aren't connected to main game loop
7. **No Tutorial/Onboarding**: New players have no guided introduction
8. **Missing Error Handling**: Many network operations fail silently

#### Medium Priority
9. **Resource Regeneration Not Synced**: Client-side regen doesn't match server timing
10. **No Reconnection Logic**: Connection drops require manual reconnect
11. **Sprite Animation Gaps**: Missing animations for some states (left-facing uses flip, no diagonal)
12. **Terminal Auto-Complete Incomplete**: Doesn't integrate with all command categories

#### Low Priority
13. **Performance**: Large string operations in terminal output
14. **Code Organization**: Some scripts have mixed responsibilities
15. **Documentation**: Inline comments sparse in some areas

---

## Functional Requirements

### FR-001: Fix Client-Server Protocol Alignment
**Priority**: Critical  
**Obligation**: MUST

The client and server must use consistent message formats and command definitions.

**Scenarios:**
- All 31 server commands must have corresponding client handlers
- Message types must match exactly (request/response pairs)
- Player data serialization must be consistent

**Acceptance Criteria:**
- Client can successfully execute all server commands
- No "unknown message type" errors in logs
- Player state syncs correctly after each command

---

### FR-002: Fix Input Mapping Configuration
**Priority**: Critical  
**Obligation**: MUST

The Godot project must have all required input actions defined.

**Scenarios:**
- Movement controls (arrow keys/WASD) work
- Attack action (Space) works
- Stealth toggle (Tab) works
- All UI shortcuts function

**Acceptance Criteria:**
- `project.godot` contains input map section
- All referenced actions in code are defined
- Player can move and interact immediately on launch

---

### FR-003: Fix Async WebSocket Connection
**Priority**: Critical  
**Obligation**: MUST

NetworkManager must properly handle asynchronous WebSocket connections.

**Scenarios:**
- Connection state is tracked accurately
- Connection failures are reported to user
- Reconnection attempts work correctly

**Acceptance Criteria:**
- `Connect()` returns actual connection result
- Connection state changes trigger appropriate UI updates
- Timeout handling prevents hanging

---

### FR-004: Implement Tutorial/Onboarding System
**Priority**: High  
**Obligation**: SHOULD

New players must receive guided introduction to game mechanics.

**Scenarios:**
- First-time players see tutorial prompts
- Tutorial covers: terminal basics, commands, resources, progression
- Tutorial can be skipped or revisited

**Acceptance Criteria:**
- Tutorial triggers on first login
- At least 5 tutorial steps covering core mechanics
- Tutorial state persists across sessions

---

### FR-005: Improve Gameplay Feedback
**Priority**: High  
**Obligation**: SHOULD

Players must receive clear feedback for all actions.

**Scenarios:**
- Successful actions show positive feedback (visual + text)
- Failed actions explain why they failed
- Level-ups and achievements have celebratory effects
- Resource changes are clearly indicated

**Acceptance Criteria:**
- All command executions show result feedback
- Resource costs are shown before execution
- Level-up notifications are prominent
- Error messages are actionable

---

### FR-006: Add Game Balance System
**Priority**: High  
**Obligation**: SHOULD

Game economy and progression must be balanced for engaging gameplay.

**Scenarios:**
- Resource costs scale appropriately with level
- XP rewards match effort required
- Shop prices are balanced against income rates
- Difficulty progression is smooth

**Acceptance Criteria:**
- Configuration file for balance values
- No single strategy dominates progression
- New players can progress without grinding
- End-game content remains challenging

---

### FR-007: Implement Robust Error Handling
**Priority**: High  
**Obligation**: MUST

All operations must handle errors gracefully.

**Scenarios:**
- Network errors show user-friendly messages
- Invalid commands provide helpful suggestions
- Server errors don't crash the client
- Timeout situations are handled

**Acceptance Criteria:**
- No unhandled exceptions in production code
- All error paths show user feedback
- Logging captures error context
- Recovery paths exist where possible

---

### FR-008: Add Auto-Reconnection
**Priority**: Medium  
**Obligation**: SHOULD

Client should automatically attempt to reconnect on connection loss.

**Scenarios:**
- Connection drop triggers reconnection attempts
- User is informed of reconnection status
- Game state is preserved where possible
- Exponential backoff prevents server spam

**Acceptance Criteria:**
- Up to 3 automatic reconnection attempts
- User sees connection status during attempts
- Successful reconnection restores session
- Failed reconnection shows clear error

---

### FR-009: Enhance Terminal Experience
**Priority**: Medium  
**Obligation**: SHOULD

Terminal interface should feel responsive and helpful.

**Scenarios:**
- Auto-complete works for all command categories
- Command suggestions appear contextually
- Output is formatted consistently
- Long outputs are paginated

**Acceptance Criteria:**
- Tab completion works for commands and arguments
- Help text is comprehensive
- Output formatting is consistent
- No output truncation issues

---

### FR-010: Integrate All Game Systems
**Priority**: Medium  
**Obligation**: SHOULD

All implemented systems must be accessible and functional.

**Scenarios:**
- All UI panels can be opened via commands
- Systems interact correctly (e.g., equipment affects combat)
- Progress in one system affects others
- No orphaned/unreachable code paths

**Acceptance Criteria:**
- Every implemented system is accessible
- Cross-system interactions work correctly
- No dead UI buttons or commands
- System state persists correctly

---

## Non-Functional Requirements

### NFR-001: Performance
- Terminal output must render within 100ms
- WebSocket messages must process within 50ms
- Frame rate must maintain 60 FPS during normal gameplay
- Memory usage must stay under 500MB

### NFR-002: Security
- All passwords must be hashed with BCrypt
- Rate limiting must prevent spam attacks
- Input validation must reject malicious input
- Session tokens must be cryptographically secure

### NFR-003: Scalability
- Server must support 2000 concurrent connections
- Per-IP connection limit of 8
- Message queue must handle burst traffic
- Database operations must not block main thread

### NFR-004: Maintainability
- Code must follow C#/Kotlin conventions
- Public APIs must be documented
- Tests must cover critical paths
- Configuration must be externalized

---

## Architecture Decisions

### AD-001: Single Source of Truth for Commands
Command definitions will live in the server (`Command.kt`) and be synced to client on login. Client will not hard-code command lists.

### AD-002: Event-Driven Architecture
Game events will flow through a central event bus. Systems will subscribe to relevant events rather than polling.

### AD-003: Configuration-Driven Balance
All balance values (costs, rewards, cooldowns) will be in configuration files, not hard-coded.

### AD-004: Graceful Degradation
When systems fail, the game should continue functioning with reduced capability rather than crashing.

---

## Implementation Phases

### Phase 1: Critical Fixes
1. Fix input mappings in project.godot
2. Fix async WebSocket connection handling
3. Align client-server protocol
4. Add basic error handling

### Phase 2: Core Gameplay Improvements
5. Implement tutorial system
6. Improve gameplay feedback
7. Add auto-reconnection
8. Enhance terminal experience

### Phase 3: System Integration
9. Connect all game systems
10. Implement game balance configuration
11. Add cross-system interactions
12. Performance optimization

### Phase 4: Polish
13. Visual effects improvements
14. Sound effects integration
15. Documentation updates
16. Testing and bug fixes

---

## Success Metrics

- All critical issues resolved
- New player can complete tutorial without confusion
- All 31 commands work end-to-end
- No crashes during normal gameplay session
- Connection stability > 99% during testing
- Frame rate maintains 60 FPS

---

## Files Reference

### Client Key Files
- `zeroday-client-godot/project.godot` - Project configuration
- `zeroday-client-godot/Scripts/Managers/GameManager.cs` - Main game coordinator
- `zeroday-client-godot/Scripts/Managers/NetworkManager.cs` - WebSocket client
- `zeroday-client-godot/Scripts/Managers/UIManager.cs` - UI routing
- `zeroday-client-godot/Scripts/Terminal/TerminalCore.cs` - Terminal emulator
- `zeroday-client-godot/Scripts/Terminal/TerminalParser.cs` - Command parsing
- `zeroday-client-godot/Scripts/Player/PlayerController.cs` - Player movement

### Server Key Files
- `zeroday-server/src/main/kotlin/com/zeroday/ZeroDayServer.kt` - Server entry point
- `zeroday-server/src/main/kotlin/com/zeroday/model/Command.kt` - Command definitions
- `zeroday-server/src/main/kotlin/com/zeroday/handler/MessageRouter.kt` - Message routing
- `zeroday-server/src/main/kotlin/com/zeroday/service/PlayerService.kt` - Player logic
- `zeroday-server/src/main/kotlin/com/zeroday/service/CommandService.kt` - Command execution

### Documentation
- `HACKING_TERMINAL_SYSTEM.md` - Terminal system documentation
- `IPv4_WORLD_SYSTEM.md` - World system documentation
- `SPRITE_INTEGRATION.md` - Sprite integration guide
- `zeroday-server/README.md` - Server architecture
