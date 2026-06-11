# ZeroDayMMO Server Deployment

Production deployment guide for the ZeroDayMMO WebSocket game server.

## Overview

The server is a Kotlin 17 / Ktor 2.3 application that:
- Listens on `ws://0.0.0.0:8080/game` (WebSocket)
- Exposes `/health`, `/live`, `/ready`, `/stats` on HTTP
- Persists player data to `data/players.json` atomically every 60s
- Targets 1k–2k concurrent WebSocket connections per process

## Quick start (Docker, local)

```bash
cd zeroday-server
./deploy.sh                          # builds + runs via docker-compose
curl http://localhost:8080/health    # → {"status":"ok","players":0}
```

## Quick start (systemd, VPS)

```bash
cd zeroday-server
sudo ./deploy.sh                     # DEPLOY_MODE=systemd is the default if not set
sudo journalctl -u zeroday-server -f
```

## Quick start (remote Docker)

```bash
REMOTE_HOST=user@1.2.3.4 \
  DEPLOY_MODE=docker-remote \
  ./deploy.sh
```

## Configuration (environment variables)

| Variable                          | Default          | Description |
|-----------------------------------|------------------|-------------|
| `ZERODAY_PORT`                    | `8080`           | HTTP/WebSocket listen port |
| `ZERODAY_SERVER_NAME`             | `ZeroDayMMO`     | MOTD / banner name |
| `ZERODAY_MAX_CONNECTIONS`         | `2000`           | Global connection cap |
| `ZERODAY_MAX_CONNECTIONS_PER_IP`  | `8`              | Per-IP connection cap |
| `ZERODAY_IDLE_TIMEOUT_MS`         | `300000`         | Idle connection close (5 min) |
| `ZERODAY_SWEEP_INTERVAL_MS`       | `30000`          | Watchdog sweep interval |
| `ZERODAY_SNAPSHOT_INTERVAL_MS`    | `60000`          | Player data flush interval |
| `ZERODAY_REGEN_INTERVAL_MS`       | `5000`           | Resource regen tick |
| `ZERODAY_DATA_DIR`                | `data`           | Persistence directory |
| `LOG_LEVEL`                       | `INFO`           | Root log level |
| `LOG_DIR`                         | `logs`           | Log output directory |
| `JAVA_OPTS`                       | *(see below)*    | JVM options |

Default `JAVA_OPTS`: `-Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100`

## Resource requirements

For a 1k-CCU (concurrent users) target on a single VPS:

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| CPU      | 1 vCPU  | 2 vCPU      |
| RAM      | 512 MB  | 1 GB        |
| Disk     | 1 GB    | 5 GB (logs + snapshots) |
| Network  | 100 Mbps | 1 Gbps    |

JVM footprint: ~200–350 MB RSS at 1k CCU. Each WebSocket connection is ~10–20 KB.

## Endpoints

| Endpoint   | Method | Purpose |
|------------|--------|---------|
| `/`        | GET    | ASCII banner + connection info |
| `/live`    | GET    | Liveness probe (returns 200 if process alive) |
| `/ready`   | GET    | Readiness probe + online count |
| `/health`  | GET    | Health check (used by Docker HEALTHCHECK) |
| `/stats`   | GET    | Full diagnostics (online, connections, persistence, watchdog) |
| `/game`    | WS     | Game WebSocket endpoint |

## WebSocket protocol

```jsonc
// Client → server
{
  "type": "login",
  "payload": { "username": "h4ck3r", "password": "..." },
  "requestId": "abc-123"  // optional, enables idempotent retries
}

// Server → client
{
  "type": "login_success",
  "payload": { "player": {...}, "sessionId": "..." },
  "timestamp": 1717500000000
}
```

All request/response type strings are defined in `protocol/MessageProtocol.kt`.

## Operational notes

### Logs
- `logs/zeroday-server.log` — rolling, 50 MB × 14 days, 2 GB cap
- `logs/audit.log` — security events (auth, rate-limit hits), 50 MB × 90 days, 5 GB cap
- `journalctl -u zeroday-server` when running under systemd

### Persistence
- `data/players.json` is rewritten atomically (tmp + rename) every 60 s
- Bcrypt password hashes are stored; never log or expose them
- A corrupt or missing snapshot is recovered gracefully on boot

### Graceful shutdown
- SIGTERM → 10 s grace, 30 s hard timeout
- The shutdown hook stops the Ktor engine, cancels background loops, and saves a final snapshot

### Security
- Sliding-window rate limit: 30 req/s per player, 5 req/min per IP+username for auth
- Hard 64 KB cap on inbound WebSocket frames
- 24-byte `SecureRandom` session IDs (192 bits of entropy)
- Bcrypt password hashing (`org.mindrot:jbcrypt`)

## Godot client configuration

The Godot client reads the server URL from a config file at `user://server.cfg`:

```ini
[connection]
url=ws://your-vps-host:8080/game
```

Or set `ZERODAY_SERVER_URL` before launching. The `NetworkManager` will fall back to `ws://localhost:8080/game` for local development.
