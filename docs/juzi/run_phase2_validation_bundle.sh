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
cp "$RAW_LOG" "$OUT_DIR/raw_logcat.log"

python3 "$JUZI_DIR/rcs_log_extract.py" "$RAW_LOG" -o "$FILTERED_LOG"
bash "$JUZI_DIR/run_rcs_research_pipeline.sh" "$FILTERED_LOG" "$OUT_DIR"
bash "$JUZI_DIR/package_research_artifacts.sh" "$OUT_DIR"

echo "phase2 bundle ready:"
echo "  $OUT_DIR/rcs_report.md"
echo "  $OUT_DIR/rcs_contracts.json"
echo "  $OUT_DIR/rcs_patch_plan.md"
echo "  $OUT_DIR/rcs_research_brief.md"
echo "  $OUT_DIR/rcs_research_artifacts.zip"
