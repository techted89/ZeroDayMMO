#!/usr/bin/env bash
# Build the fat JAR and run it locally. No Docker, no systemd.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
GRADLE_BIN="$(command -v gradle || echo ./gradlew)"
echo "Building with $GRADLE_BIN..."
$GRADLE_BIN clean jar --console=plain --no-daemon
JAR="build/libs/zeroday-server.jar"
[[ -f "$JAR" ]] || { echo "Build failed: $JAR missing" >&2; exit 1; }
echo "Starting $JAR..."
exec java \
  -Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 \
  -Dlogback.configurationFile="$SCRIPT_DIR/logback.xml" \
  -jar "$JAR" "$@"
