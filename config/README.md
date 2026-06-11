# ZeroDayMMO Configuration

Single source of truth for all server, admin, and database configuration.

## Configuration Sources (priority order)

1. **Environment variables** — highest priority, override everything
2. **Godot ProjectSettings** — `project.godot` under `zeroday/*` keys (client)
3. **`.env` file** — loaded by server startup scripts
4. **`.conf` files** — HOCON format for the Kotlin/Ktor server
5. **`server.cfg`** — legacy Godot ConfigFile fallback

## Directory Structure

```
config/
├── .env                     # Environment variables (server + client)
├── server/
│   └── application.conf     # HOCON server config (Ktor)
├── admin/
│   └── admin.conf           # HOCON admin config
├── db/
│   └── database.conf        # HOCON database config
├── ServerConfig.kt          # Kotlin config object (server reads env vars)
├── AdminConfig.kt           # Kotlin admin config object
├── start_server.sh          # Startup script (loads .env)
└── README.md                # This file
```

## Godot Client Config

The Godot client reads config from `project.godot` under `zeroday/*` keys.
The `addons/zero_day_mmo/core/server_config.gd` autoload resolves config
in this order:

1. `OS.get_environment("ZERODAY_*")` — env var override
2. `ProjectSettings.get_setting("zeroday/*")` — project.godot defaults
3. `server.cfg` — legacy ConfigFile fallback

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ZERODAY_PORT` | `8080` | Server HTTP port |
| `ZERODAY_MAX_CONNECTIONS` | `2000` | Global connection cap |
| `ZERODAY_MAX_CONNECTIONS_PER_IP` | `8` | Per-IP connection cap |
| `ZERODAY_SERVER_NAME` | `ZeroDayMMO` | Server identity / MOTD |
| `ZERODAY_DATA_DIR` | `data` | Player data directory |
| `ZERODAY_SNAPSHOT_INTERVAL_MS` | `60000` | Persistence flush interval |
| `ZERODAY_IDLE_TIMEOUT_MS` | `300000` | Idle connection timeout |
| `ZERODAY_SWEEP_INTERVAL_MS` | `30000` | Watchdog sweep interval |
| `ZERODAY_REGEN_INTERVAL_MS` | `5000` | Resource regen tick |
| `ZERODAY_ADMIN_USERNAME` | `admin` | Admin panel username |
| `ZERODAY_ADMIN_PASSWORD` | `zeroday-admin-1337` | Admin panel password |
| `ZERODAY_ADMIN_TOKEN` | `zeroday-admin-secret-change-me` | Admin API token |
| `ZERODAY_ADMIN_PORT` | `8081` | Admin panel port |
| `ZERODAY_ADMIN_RATE_LIMIT` | `30` | Admin requests/s |
| `ZERODAY_LOG_RETENTION_HOURS` | `168` | Admin log retention (hours) |
| `ZERODAY_DB_URL` | `jdbc:sqlite:data/zeroday.db` | Database URL |
| `ZERODAY_DB_USER` | *(empty)* | Database username |
| `ZERODAY_DB_PASSWORD` | *(empty)* | Database password |

## Starting the Server

Use the `start_server.sh` script to start the server with the proper configuration:

```bash
cd config
./start_server.sh
```

Or run directly from the server directory:

```bash
cd zeroday-server
./gradlew run
```
