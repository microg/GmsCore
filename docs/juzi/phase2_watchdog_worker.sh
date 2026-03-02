#!/usr/bin/env bash
set -euo pipefail

JUZI_DIR="${1:?missing juzi dir}"
INTERVAL_SECONDS="${2:-180}"
DOWNLOADS_DIR="${HOME}/Downloads"

while true; do
  ts="$(date '+%Y-%m-%d %H:%M:%S')"
  echo "[$ts] phase2 watchdog sweep begin"
  if ! bash "$JUZI_DIR/harvest_downloads_phase2.sh" "$DOWNLOADS_DIR" "$JUZI_DIR/phase2_inbox" "$JUZI_DIR/phase2_submissions"; then
    echo "[$ts] phase2 watchdog sweep failed"
  fi
  ts="$(date '+%Y-%m-%d %H:%M:%S')"
  echo "[$ts] phase2 watchdog sweep end"
  sleep "$INTERVAL_SECONDS"
done
