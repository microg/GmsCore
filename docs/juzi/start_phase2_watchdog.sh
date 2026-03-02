#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JUZI_DIR="$ROOT_DIR/docs/juzi"
PID_FILE="$JUZI_DIR/.phase2_watchdog.pid"
LOG_FILE="$JUZI_DIR/.phase2_watchdog.log"
INTERVAL_SECONDS="${PHASE2_WATCHDOG_INTERVAL_SECONDS:-180}"

is_running() {
  if [[ ! -f "$PID_FILE" ]]; then
    return 1
  fi
  local pid
  pid="$(cat "$PID_FILE" || true)"
  [[ -n "${pid:-}" ]] && kill -0 "$pid" 2>/dev/null
}

if is_running; then
  pid="$(cat "$PID_FILE")"
  echo "watchdog already running pid=$pid interval=${INTERVAL_SECONDS}s"
  exit 0
fi

rm -f "$PID_FILE"
mkdir -p "$JUZI_DIR/phase2_inbox" "$JUZI_DIR/phase2_submissions"
touch "$LOG_FILE"

(
  while true; do
    ts="$(date '+%Y-%m-%d %H:%M:%S')"
    echo "[$ts] phase2 watchdog sweep begin"
    if ! bash "$JUZI_DIR/harvest_downloads_phase2.sh" "$HOME/Downloads" "$JUZI_DIR/phase2_inbox" "$JUZI_DIR/phase2_submissions"; then
      echo "[$ts] phase2 watchdog sweep failed"
    fi
    ts="$(date '+%Y-%m-%d %H:%M:%S')"
    echo "[$ts] phase2 watchdog sweep end"
    sleep "$INTERVAL_SECONDS"
  done
) >> "$LOG_FILE" 2>&1 &

pid="$!"
echo "$pid" > "$PID_FILE"
disown "$pid" 2>/dev/null || true
echo "watchdog started pid=$pid interval=${INTERVAL_SECONDS}s"
echo "log: $LOG_FILE"
