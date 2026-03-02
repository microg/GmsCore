#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JUZI_DIR="$ROOT_DIR/docs/juzi"
PIPELINE_SCRIPT="$JUZI_DIR/run_rcs_research_pipeline.sh"

if [[ ! -x "$PIPELINE_SCRIPT" ]]; then
  echo "pipeline script missing: $PIPELINE_SCRIPT" >&2
  exit 1
fi

INPUT_PATH="${1:-}"
OUT_DIR="${2:-$JUZI_DIR}"

pick_latest_log() {
  local candidates
  candidates="$(
    (
      find "$ROOT_DIR" -maxdepth 4 -type f \( -name "*.log" -o -name "*.txt" \) 2>/dev/null
      if [[ -d "$HOME/Downloads" ]]; then
        find "$HOME/Downloads" -maxdepth 3 -type f \( -name "*.log" -o -name "*.txt" \) 2>/dev/null
      fi
    ) | while IFS= read -r f; do
      if rg -q "trace_decision|RcsApiService|CarrierAuthService|blocker_summary" "$f" 2>/dev/null; then
        echo "$f"
      fi
    done
  )"

  if [[ -z "$candidates" ]]; then
    return 1
  fi

  # shellcheck disable=SC2016
  echo "$candidates" | while IFS= read -r f; do
    stat -f "%m|%N" "$f"
  done | sort -t'|' -k1,1nr | head -n1 | cut -d'|' -f2-
}

if [[ -z "$INPUT_PATH" ]]; then
  if ! INPUT_PATH="$(pick_latest_log)"; then
    echo "no trace log found (need trace_decision/RcsApiService lines)" >&2
    exit 1
  fi
fi

if [[ ! -f "$INPUT_PATH" ]]; then
  echo "input not found: $INPUT_PATH" >&2
  exit 1
fi

echo "using log: $INPUT_PATH"
bash "$PIPELINE_SCRIPT" "$INPUT_PATH" "$OUT_DIR"
echo "done: $OUT_DIR/rcs_report.md"
echo "done: $OUT_DIR/rcs_contracts.json"
echo "done: $OUT_DIR/rcs_patch_plan.md"
echo "done: $OUT_DIR/rcs_research_brief.md"
