#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JUZI_DIR="$ROOT_DIR/docs/juzi"
DOWNLOADS_DIR="${1:-$HOME/Downloads}"
INBOX_DIR="${2:-$JUZI_DIR/phase2_inbox}"
OUT_DIR="${3:-$JUZI_DIR/phase2_submissions}"

mkdir -p "$INBOX_DIR" "$OUT_DIR"

copied=0
for f in "$DOWNLOADS_DIR"/*.log; do
  if [[ ! -e "$f" ]]; then
    continue
  fi
  base="$(basename "$f" .log)"
  # Avoid re-importing files already copied to inbox.
  if [[ -f "$INBOX_DIR/$base.log" ]]; then
    continue
  fi
  # Ignore tiny logs.
  size="$(wc -c < "$f" | tr -d ' ')"
  if [[ "$size" -lt 512 ]]; then
    continue
  fi
  cp "$f" "$INBOX_DIR/$base.log"
  if [[ -f "$DOWNLOADS_DIR/$base.json" ]]; then
    cp "$DOWNLOADS_DIR/$base.json" "$INBOX_DIR/$base.json"
  fi
  copied=$((copied + 1))
done

echo "harvested logs: $copied"
bash "$JUZI_DIR/process_phase2_inbox.sh" "$INBOX_DIR" "$OUT_DIR"
