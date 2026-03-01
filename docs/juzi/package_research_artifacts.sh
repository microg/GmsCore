#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <output_dir_from_pipeline> [zip_path]"
  exit 2
fi

OUT_DIR="$1"
ZIP_PATH="${2:-$OUT_DIR/rcs_research_artifacts.zip}"

for f in \
  "$OUT_DIR/rcs_report.md" \
  "$OUT_DIR/rcs_contracts.json" \
  "$OUT_DIR/rcs_patch_plan.md" \
  "$OUT_DIR/rcs_research_brief.md"; do
  if [ ! -f "$f" ]; then
    echo "Missing required artifact: $f"
    exit 3
  fi
done

cd "$OUT_DIR"
zip -q -r "$ZIP_PATH" rcs_report.md rcs_contracts.json rcs_patch_plan.md rcs_research_brief.md
echo "Packaged: $ZIP_PATH"

