#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JUZI_DIR="$ROOT_DIR/docs/juzi"
PID_FILE="$JUZI_DIR/.phase2_watchdog.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "watchdog not running (no pid file)"
  exit 0
fi

pid="$(cat "$PID_FILE" || true)"
if [[ -z "${pid:-}" ]]; then
  rm -f "$PID_FILE"
  echo "watchdog not running (empty pid file cleaned)"
  exit 0
fi

if ! kill -0 "$pid" 2>/dev/null; then
  rm -f "$PID_FILE"
  echo "watchdog not running (stale pid file cleaned)"
  exit 0
fi

kill "$pid" 2>/dev/null || true
for _ in 1 2 3 4 5; do
  if ! kill -0 "$pid" 2>/dev/null; then
    break
  fi
  sleep 1
done

if kill -0 "$pid" 2>/dev/null; then
  kill -9 "$pid" 2>/dev/null || true
fi

rm -f "$PID_FILE"
echo "watchdog stopped pid=$pid"
