#!/usr/bin/env python3
"""
Generate next minimal patch suggestion from RCS contract map.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path


def choose_target(contracts: list[dict]) -> dict | None:
    # Prioritize first unhandled observed contract with highest count and earliest trace.
    candidates = [
        c
        for c in contracts
        if not c.get("handled_seen", False)
        and c.get("token")
        and c.get("token") != "<null>"
        and any(d in c.get("details", {}) for d in ("observe_config_request", "observe_generic_request"))
    ]
    if not candidates:
        return None
    candidates.sort(key=lambda c: (c.get("first_trace_id", 10**9), -c.get("count", 0)))
    return candidates[0]


def decide_mode(target: dict) -> str:
    details = target.get("details", {})
    code = int(target.get("code", -1))
    if "observe_config_request" in details or code in (1, 2, 1001):
        return "CONFIG_MINIMAL_COMPLETION"
    return "GENERIC_MINIMAL_COMPLETION"


def build_markdown(source: Path, target: dict | None) -> str:
    lines: list[str] = []
    lines.append("# RCS Next Patch Suggestion")
    lines.append("")
    lines.append(f"- Source contract map: `{source}`")
    lines.append("")

    if target is None:
        lines.append("No actionable unhandled observed contract row found.")
        lines.append("Keep instrumentation-only mode and collect another run.")
        return "\n".join(lines)

    mode = decide_mode(target)
    token = target.get("token", "")
    code = int(target.get("code", -1))
    count = int(target.get("count", 0))
    first_trace_id = int(target.get("first_trace_id", -1))
    service = target.get("service", "rcs")

    lines.append("## Selected Target")
    lines.append(f"- service: `{service}`")
    lines.append(f"- token: `{token}`")
    lines.append(f"- code: `{code}`")
    lines.append(f"- first_trace_id: `{first_trace_id}`")
    lines.append(f"- repeated_count: `{count}`")
    lines.append(f"- suggested_mode: `{mode}`")
    lines.append("")

    lines.append("## Minimal Kotlin Patch Direction")
    lines.append("Update `RcsContractPolicy` with an explicit row rule, e.g.:")
    lines.append("")
    lines.append("```kotlin")
    lines.append("if (row.token == \"TOKEN_HERE\" && row.code == CODE_HERE) {")
    lines.append("    return ContractDecision(")
    lines.append("        mode = ContractDecisionMode.OBSERVE_CONFIG,")
    lines.append("        detail = \"targeted_contract_row\",")
    lines.append("        handled = true // only when response semantics are implemented")
    lines.append("    )")
    lines.append("}")
    lines.append("```")
    lines.append("")
    lines.append("## Guardrails")
    lines.append("- Implement only this row + direct dependencies.")
    lines.append("- Keep all other rows fail-closed.")
    lines.append("- Do not add broad token wildcard handling.")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="Suggest next minimal RCS patch from contract map.")
    parser.add_argument("input", type=Path, help="Input contract map JSON")
    parser.add_argument("-o", "--output", type=Path, required=True, help="Output markdown file")
    args = parser.parse_args()

    data = json.loads(args.input.read_text(encoding="utf-8"))
    contracts = data.get("contracts", [])
    target = choose_target(contracts)
    out = build_markdown(args.input, target)
    args.output.write_text(out + "\n", encoding="utf-8")
    print(f"wrote {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

