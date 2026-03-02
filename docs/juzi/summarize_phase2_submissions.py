#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path


def load_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8", errors="replace"))
    except Exception:
        return {}


def main() -> int:
    parser = argparse.ArgumentParser(description="Summarize phase2 submission outputs")
    parser.add_argument("root", type=Path, help="phase2_submissions root")
    parser.add_argument("-o", "--output", type=Path, required=True, help="markdown summary path")
    args = parser.parse_args()

    rows = []
    if args.root.exists():
        for d in sorted(p for p in args.root.iterdir() if p.is_dir()):
            meta = load_json(d / "metadata.json")
            report = d / "rcs_report.md"
            contracts = d / "rcs_contracts.json"
            patch_plan = d / "rcs_patch_plan.md"
            zip_file = d / "rcs_research_artifacts.zip"
            rows.append(
                {
                    "name": d.name,
                    "tester_id": meta.get("tester_id", ""),
                    "device": meta.get("device", ""),
                    "rom": meta.get("rom", ""),
                    "android": meta.get("android", ""),
                    "carrier_country": meta.get("carrier_country", ""),
                    "result_state": meta.get("result_state", ""),
                    "report": report.exists(),
                    "contracts": contracts.exists(),
                    "patch_plan": patch_plan.exists(),
                    "zip": zip_file.exists(),
                }
            )

    lines = ["# Phase 2 Submission Index", ""]
    lines.append(f"- Total submissions: **{len(rows)}**")
    lines.append("")
    if not rows:
        lines.append("No submissions found.")
    else:
        lines.append("| Submission | Tester | Device | ROM | Android | Carrier | State | Artifacts |")
        lines.append("|---|---|---|---|---|---|---|---|")
        for r in rows:
            artifacts = []
            if r["report"]:
                artifacts.append("report")
            if r["contracts"]:
                artifacts.append("contracts")
            if r["patch_plan"]:
                artifacts.append("plan")
            if r["zip"]:
                artifacts.append("zip")
            lines.append(
                f"| {r['name']} | {r['tester_id']} | {r['device']} | {r['rom']} | {r['android']} | {r['carrier_country']} | {r['result_state']} | {', '.join(artifacts)} |"
            )

    args.output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
