#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JUZI_DIR="$ROOT_DIR/docs/juzi"
INBOX_DIR="${1:-$JUZI_DIR/phase2_inbox}"
OUT_ROOT="${2:-$JUZI_DIR/phase2_submissions}"

mkdir -p "$INBOX_DIR" "$OUT_ROOT"

has_any=0
for f in "$INBOX_DIR"/*.log; do
  if [[ ! -e "$f" ]]; then
    continue
  fi
  has_any=1
  base="$(basename "$f" .log)"
  meta="$INBOX_DIR/$base.json"
  out_dir="$OUT_ROOT/$base"
  mkdir -p "$out_dir"

  cp "$f" "$out_dir/raw_logcat.log"
  if [[ -f "$meta" ]]; then
    cp "$meta" "$out_dir/metadata.json"
  else
    cat > "$out_dir/metadata.json" <<'EOF'
{
  "tester_id": "",
  "device": "",
  "rom": "",
  "android": "",
  "carrier_country": "",
  "messages_version": "",
  "microg_commit": "",
  "result_state": "",
  "time_to_state_seconds": 0,
  "notes": "metadata missing in inbox"
}
EOF
  fi

  bash "$JUZI_DIR/run_phase2_validation_bundle.sh" "$out_dir/raw_logcat.log" "$out_dir"
done

if [[ "$has_any" -eq 0 ]]; then
  echo "no log files found in $INBOX_DIR"
  exit 0
fi

python3 "$JUZI_DIR/summarize_phase2_submissions.py" "$OUT_ROOT" -o "$OUT_ROOT/index.md"
echo "phase2 submissions index: $OUT_ROOT/index.md"
