#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JUZI_DIR="$ROOT_DIR/docs/juzi"

RAW_LOG="${1:-}"
OUT_DIR="${2:-$JUZI_DIR/phase2_output}"

if [[ -z "$RAW_LOG" ]]; then
  echo "usage: $0 <raw_logcat_file> [output_dir]" >&2
  exit 1
fi
if [[ ! -f "$RAW_LOG" ]]; then
  echo "raw log not found: $RAW_LOG" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"

FILTERED_LOG="$OUT_DIR/phase2_filtered.log"
RAW_ABS="$(python3 - <<'PY' "$RAW_LOG"
import os, sys
print(os.path.abspath(sys.argv[1]))
PY
)"
DEST_RAW="$OUT_DIR/raw_logcat.log"
DEST_ABS="$(python3 - <<'PY' "$DEST_RAW"
import os, sys
print(os.path.abspath(sys.argv[1]))
PY
)"
if [[ "$RAW_ABS" != "$DEST_ABS" ]]; then
  cp "$RAW_LOG" "$DEST_RAW"
fi

python3 "$JUZI_DIR/rcs_log_extract.py" "$RAW_LOG" -o "$FILTERED_LOG"
bash "$JUZI_DIR/run_rcs_research_pipeline.sh" "$FILTERED_LOG" "$OUT_DIR"
bash "$JUZI_DIR/package_research_artifacts.sh" "$OUT_DIR"

echo "phase2 bundle ready:"
echo "  $OUT_DIR/rcs_report.md"
echo "  $OUT_DIR/rcs_contracts.json"
echo "  $OUT_DIR/rcs_patch_plan.md"
echo "  $OUT_DIR/rcs_research_brief.md"
echo "  $OUT_DIR/rcs_research_artifacts.zip"
