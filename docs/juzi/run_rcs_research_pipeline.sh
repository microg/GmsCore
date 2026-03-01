#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <logcat.txt> [output_dir]"
  exit 2
fi

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
LOG_PATH="$1"
OUT_DIR="${2:-$ROOT_DIR/docs/juzi/output}"
mkdir -p "$OUT_DIR"

REPORT_MD="$OUT_DIR/rcs_report.md"
CONTRACTS_JSON="$OUT_DIR/rcs_contracts.json"
PATCH_PLAN_MD="$OUT_DIR/rcs_patch_plan.md"
RESEARCH_BRIEF_MD="$OUT_DIR/rcs_research_brief.md"

python3 "$ROOT_DIR/docs/juzi/rcs_trace_analyzer.py" "$LOG_PATH" -o "$REPORT_MD"
python3 "$ROOT_DIR/docs/juzi/rcs_contract_map_builder.py" "$LOG_PATH" -o "$CONTRACTS_JSON"
python3 "$ROOT_DIR/docs/juzi/rcs_patch_suggester.py" "$CONTRACTS_JSON" -o "$PATCH_PLAN_MD"
python3 "$ROOT_DIR/docs/juzi/rcs_research_brief.py" --contracts "$CONTRACTS_JSON" --patch-plan "$PATCH_PLAN_MD" -o "$RESEARCH_BRIEF_MD"

echo "Generated:"
echo "  $REPORT_MD"
echo "  $CONTRACTS_JSON"
echo "  $PATCH_PLAN_MD"
echo "  $RESEARCH_BRIEF_MD"

