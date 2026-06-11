#!/usr/bin/env bash
# ZeroDayMMO server deployment script.
# Builds the fat JAR, then deploys to the VPS either as a Docker container
# or as a systemd service. Idempotent — safe to re-run.
set -euo pipefail

# ---- config ----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DEPLOY_MODE="${DEPLOY_MODE:-docker}"   # "docker" or "systemd"
IMAGE_NAME="${IMAGE_NAME:-zeroday-server:1.0.0}"
CONTAINER_NAME="${CONTAINER_NAME:-zeroday-server}"
REMOTE_HOST="${REMOTE_HOST:-}"         # e.g. "user@1.2.3.4"
REMOTE_DIR="${REMOTE_DIR:-/opt/zeroday}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-60}" # seconds to wait for /health

# ---- helpers ----
log()  { printf "\033[1;36m[deploy]\033[0m %s\n" "$*" >&2; }
err()  { printf "\033[1;31m[error]\033[0m %s\n" "$*" >&2; }
warn() { printf "\033[1;33m[warn ]\033[0m %s\n" "$*" >&2; }
ok()   { printf "\033[1;32m[ok   ]\033[0m %s\n" "$*" >&2; }

# ---- preflight ----
log "ZeroDayMMO deploy starting (mode=$DEPLOY_MODE)"
command -v gradle >/dev/null 2>&1 || command -v ./gradlew >/dev/null 2>&1 \
  || { err "gradle is required (or use ./gradlew)"; exit 1; }
[[ -f build.gradle.kts ]] || { err "run from zeroday-server/ root"; exit 1; }

GRADLE_BIN="$(command -v gradle || echo ./gradlew)"

# ---- 1) build ----
log "Building fat JAR with $GRADLE_BIN"
$GRADLE_BIN clean jar --console=plain --no-daemon

JAR="build/libs/zeroday-server.jar"
[[ -f "$JAR" ]] || { err "build did not produce $JAR"; exit 1; }
ok "Built $JAR ($(du -h "$JAR" | cut -f1))"

# ---- 2) deploy ----
case "$DEPLOY_MODE" in
  local-docker)
    log "Building Docker image $IMAGE_NAME"
    docker build -t "$IMAGE_NAME" .
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
      log "Removing existing container $CONTAINER_NAME"
      docker rm -f "$CONTAINER_NAME" >/dev/null
    fi
    log "Starting container"
    docker compose up -d
    ok "Container up. Tail: docker logs -f $CONTAINER_NAME"
    ;;

  docker-remote)
    [[ -n "$REMOTE_HOST" ]] || { err "REMOTE_HOST required for docker-remote"; exit 1; }
    log "Building image locally"
    docker build -t "$IMAGE_NAME" .
    log "Saving image to tarball"
    docker save "$IMAGE_NAME" | gzip > /tmp/zeroday-server.tar.gz
    log "Copying to $REMOTE_HOST"
    scp /tmp/zeroday-server.tar.gz docker-compose.yml logback.xml \
        "$REMOTE_HOST:/tmp/"
    rm -f /tmp/zeroday-server.tar.gz
    log "Loading and starting on remote"
    ssh "$REMOTE_HOST" <<'REMOTE'
      set -e
      cd /opt/zeroday 2>/dev/null || mkdir -p /opt/zeroday && cd /opt/zeroday
      docker load -i /tmp/zeroday-server.tar.gz
      docker compose pull --ignore-pull-failures || true
      docker compose up -d --force-recreate
      rm -f /tmp/zeroday-server.tar.gz /tmp/docker-compose.yml /tmp/logback.xml
REMOTE
    ok "Deployed to $REMOTE_HOST"
    ;;

  systemd)
    log "Preparing systemd deploy bundle"
    BUNDLE="/tmp/zeroday-deploy-$$"
    mkdir -p "$BUNDLE/data" "$BUNDLE/logs"
    cp "$JAR" "$BUNDLE/zeroday-server.jar"
    cp logback.xml "$BUNDLE/logback.xml"
    cp deploy/zeroday-server.service "$BUNDLE/zeroday-server.service"
    tar -czf "$BUNDLE.tgz" -C "$BUNDLE" .

    if [[ -n "$REMOTE_HOST" ]]; then
      log "Uploading to $REMOTE_HOST:$REMOTE_DIR"
      ssh "$REMOTE_HOST" "mkdir -p $REMOTE_DIR/data $REMOTE_DIR/logs"
      scp "$BUNDLE.tgz" "$REMOTE_HOST:/tmp/"
      ssh "$REMOTE_HOST" <<REMOTE
        set -e
        sudo tar -xzf /tmp/zeroday-deploy.tgz -C $REMOTE_DIR
        sudo cp $REMOTE_DIR/zeroday-server.service /etc/systemd/system/zeroday-server.service
        sudo systemctl daemon-reload
        sudo systemctl enable zeroday-server
        sudo systemctl restart zeroday-server
        rm -f /tmp/zeroday-deploy.tgz
REMOTE
      rm -rf "$BUNDLE" "$BUNDLE.tgz"
    else
      log "Installing locally"
      cp "$BUNDLE/zeroday-server.jar" "$REMOTE_DIR/zeroday-server.jar" 2>/dev/null \
        || sudo cp "$BUNDLE/zeroday-server.jar" "$REMOTE_DIR/zeroday-server.jar"
      cp "$BUNDLE/logback.xml" "$REMOTE_DIR/logback.xml" 2>/dev/null \
        || sudo cp "$BUNDLE/logback.xml" "$REMOTE_DIR/logback.xml"
      cp "$BUNDLE/zeroday-server.service" /etc/systemd/system/zeroday-server.service 2>/dev/null \
        || sudo cp "$BUNDLE/zeroday-server.service" /etc/systemd/system/zeroday-server.service
      systemctl daemon-reload 2>/dev/null || sudo systemctl daemon-reload
      systemctl enable zeroday-server 2>/dev/null || sudo systemctl enable zeroday-server
      systemctl restart zeroday-server 2>/dev/null || sudo systemctl restart zeroday-server
      rm -rf "$BUNDLE" "$BUNDLE.tgz"
    fi
    ok "systemd service installed and started"
    ;;

  *)
    err "Unknown DEPLOY_MODE: $DEPLOY_MODE (use: local-docker|docker-remote|systemd)"
    exit 1
    ;;
esac

# ---- 3) wait for /health ----
HOST_PORT="${ZERODAY_PORT:-8080}"
HEALTH_URL="http://localhost:${HOST_PORT}/health"
if [[ -n "$REMOTE_HOST" ]]; then
  HEALTH_URL="http://${REMOTE_HOST#*@}:${HOST_PORT}/health"
fi

log "Waiting for $HEALTH_URL (timeout ${HEALTH_TIMEOUT}s)"
for ((i = 0; i < HEALTH_TIMEOUT; i += 2)); do
  if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then
    ok "Server is healthy"
    curl -s "$HEALTH_URL" || true
    echo
    exit 0
  fi
  sleep 2
done

err "Server did not become healthy within ${HEALTH_TIMEOUT}s"
case "$DEPLOY_MODE" in
  *docker*) warn "Check: docker logs $CONTAINER_NAME" ;;
  systemd)  warn "Check: journalctl -u zeroday-server -n 100" ;;
esac
exit 1
