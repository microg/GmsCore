#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JUZI_DIR="$ROOT_DIR/docs/juzi"
PID_FILE="$JUZI_DIR/.phase2_watchdog.pid"
LOG_FILE="$JUZI_DIR/.phase2_watchdog.log"
INTERVAL_SECONDS="${PHASE2_WATCHDOG_INTERVAL_SECONDS:-180}"
WORKER_SCRIPT="$JUZI_DIR/phase2_watchdog_worker.sh"

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

nohup bash "$WORKER_SCRIPT" "$JUZI_DIR" "$INTERVAL_SECONDS" >> "$LOG_FILE" 2>&1 &

pid="$!"
echo "$pid" > "$PID_FILE"
echo "watchdog started pid=$pid interval=${INTERVAL_SECONDS}s"
echo "log: $LOG_FILE"
