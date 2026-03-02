#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JUZI_DIR="$ROOT_DIR/docs/juzi"
DOWNLOADS_DIR="${1:-$HOME/Downloads}"
INBOX_DIR="${2:-$JUZI_DIR/phase2_inbox}"
OUT_DIR="${3:-$JUZI_DIR/phase2_submissions}"
MIN_LOG_BYTES="${PHASE2_MIN_LOG_BYTES:-64}"

mkdir -p "$INBOX_DIR" "$OUT_DIR"

copied=0
import_one() {
  local src_log="$1"
  local src_json="${2:-}"
  local base size dest_log
  base="$(basename "$src_log" .log)"
  dest_log="$INBOX_DIR/$base.log"

  case "$base" in
    .*|*~) return 0 ;;
  esac

  size="$(wc -c < "$src_log" | tr -d ' ')"
  if [[ "$size" -lt "$MIN_LOG_BYTES" ]]; then
    return 0
  fi

  if [[ -f "$dest_log" && "$src_log" -ot "$dest_log" ]]; then
    return 0
  fi

  cp "$src_log" "$dest_log"
  if [[ -n "$src_json" && -f "$src_json" ]]; then
    cp "$src_json" "$INBOX_DIR/$base.json"
  fi
  copied=$((copied + 1))
}

shopt -s nullglob
for f in "$DOWNLOADS_DIR"/*.log; do
  base="$(basename "$f" .log)"
  import_one "$f" "$DOWNLOADS_DIR/$base.json"
done

for z in "$DOWNLOADS_DIR"/*.zip; do
  z_name="$(basename "$z")"
  z_lower="$(printf '%s' "$z_name" | tr '[:upper:]' '[:lower:]')"
  case "$z_lower" in
    *phase2*|*rcs*|*logcat*) ;;
    *) continue ;;
  esac

  z_key="$(printf '%s' "$z_name" | tr -c '[:alnum:]._-' '_')"
  z_marker="$INBOX_DIR/.zip_imported_${z_key}.stamp"
  z_info="$(stat -f '%m:%z' "$z")"
  if [[ -f "$z_marker" && "$(cat "$z_marker")" == "$z_info" ]]; then
    continue
  fi

  tmp_dir="$(mktemp -d)"
  if ! unzip -q -o "$z" -d "$tmp_dir"; then
    rm -rf "$tmp_dir"
    continue
  fi

  copied_before="$copied"

  while IFS= read -r log_file; do
    log_dir="$(dirname "$log_file")"
    base="$(basename "$log_file" .log)"
    import_one "$log_file" "$log_dir/$base.json"
  done < <(find "$tmp_dir" -type f -name "*.log")

  if [[ "$copied" -gt "$copied_before" ]]; then
    echo "$z_info" > "$z_marker"
  fi
  rm -rf "$tmp_dir"
done
shopt -u nullglob

echo "harvested logs: $copied"
bash "$JUZI_DIR/process_phase2_inbox.sh" "$INBOX_DIR" "$OUT_DIR"
