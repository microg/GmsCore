#!/usr/bin/env python3
"""
Generate a concise research-style brief from contract map and patch plan.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description="Build research brief for Issue #2994.")
    parser.add_argument("--contracts", type=Path, required=True, help="Contracts JSON from rcs_contract_map_builder.py")
    parser.add_argument("--patch-plan", type=Path, required=True, help="Patch plan markdown from rcs_patch_suggester.py")
    parser.add_argument("-o", "--output", type=Path, required=True, help="Output markdown path")
    args = parser.parse_args()

    contract_data = json.loads(args.contracts.read_text(encoding="utf-8"))
    patch_plan = args.patch_plan.read_text(encoding="utf-8")
    contracts = contract_data.get("contracts", [])
    top = contracts[0] if contracts else None

    lines: list[str] = []
    lines.append("# Issue #2994 Research Brief")
    lines.append("")
    lines.append("## Objective")
    lines.append("Identify and patch the first authoritative RCS contract blocker in a reproducible, fail-closed way.")
    lines.append("")
    lines.append("## Dataset")
    lines.append(f"- source: `{contract_data.get('source', '')}`")
    lines.append(f"- total_rows: `{contract_data.get('total_rows', 0)}`")
    lines.append(f"- unique_contracts: `{len(contracts)}`")
    lines.append("")
    lines.append("## Top Observed Contract")
    if top:
        lines.append(f"- service: `{top.get('service')}`")
        lines.append(f"- token: `{top.get('token')}`")
        lines.append(f"- code: `{top.get('code')}`")
        lines.append(f"- repeated_count: `{top.get('count')}`")
        lines.append(f"- handled_seen: `{top.get('handled_seen')}`")
        lines.append(f"- details: `{top.get('details')}`")
    else:
        lines.append("- none")
    lines.append("")
    lines.append("## Next Patch Plan")
    lines.append("```md")
    lines.extend(patch_plan.rstrip().splitlines())
    lines.append("```")
    lines.append("")
    lines.append("## Maintainer Review Ask")
    lines.append("- Confirm that blocker ranking method is acceptable.")
    lines.append("- Confirm whether target row semantics align with expected provisioning flow.")

    args.output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

