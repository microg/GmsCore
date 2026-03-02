#!/usr/bin/env python3
"""
Extract RCS-relevant lines from large Android logcat dumps.
"""

from __future__ import annotations

import argparse
import re
from pathlib import Path

DEFAULT_PATTERNS = [
    r"RcsApiService",
    r"CarrierAuthService",
    r"trace_decision",
    r"blocker_candidate",
    r"blocker_summary",
    r"\bRCS\b",
    r"\brcs\b",
    r"\bJibe\b",
    r"Provision",
    r"SIP/200",
    r"SIP/403",
]


def main() -> int:
    parser = argparse.ArgumentParser(description="Extract RCS-related logcat lines.")
    parser.add_argument("input", type=Path, help="Raw logcat input file")
    parser.add_argument("-o", "--output", type=Path, required=True, help="Filtered output file")
    parser.add_argument(
        "--pattern",
        action="append",
        default=[],
        help="Additional regex pattern (repeatable)",
    )
    args = parser.parse_args()

    if not args.input.exists():
        raise SystemExit(f"input not found: {args.input}")

    patterns = DEFAULT_PATTERNS + args.pattern
    regexes = [re.compile(p) for p in patterns]

    kept = []
    for line in args.input.read_text(encoding="utf-8", errors="replace").splitlines():
        if any(r.search(line) for r in regexes):
            kept.append(line)

    args.output.write_text("\n".join(kept) + ("\n" if kept else ""), encoding="utf-8")
    print(f"wrote {args.output} lines={len(kept)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
