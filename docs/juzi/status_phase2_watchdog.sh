#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JUZI_DIR="$ROOT_DIR/docs/juzi"
PID_FILE="$JUZI_DIR/.phase2_watchdog.pid"
LOG_FILE="$JUZI_DIR/.phase2_watchdog.log"

if [[ ! -f "$PID_FILE" ]]; then
  echo "watchdog status: stopped"
  [[ -f "$LOG_FILE" ]] && echo "log: $LOG_FILE"
  exit 0
fi

pid="$(cat "$PID_FILE" || true)"
if [[ -z "${pid:-}" ]]; then
  rm -f "$PID_FILE"
  echo "watchdog status: stopped (stale pid file cleaned)"
  [[ -f "$LOG_FILE" ]] && echo "log: $LOG_FILE"
  exit 0
fi

if ! kill -0 "$pid" 2>/dev/null; then
  rm -f "$PID_FILE"
  echo "watchdog status: stopped (stale pid file cleaned)"
  [[ -f "$LOG_FILE" ]] && echo "log: $LOG_FILE"
  exit 0
fi

echo "watchdog status: running pid=$pid"
if [[ -f "$LOG_FILE" ]]; then
  echo "log: $LOG_FILE"
  echo "--- last 20 lines ---"
  tail -n 20 "$LOG_FILE"
fi
