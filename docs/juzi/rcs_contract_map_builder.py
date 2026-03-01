#!/usr/bin/env python3
"""
Build a contract map (token+code priority list) from RCS trace logs.
"""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path


TRACE_RE = re.compile(
    r"trace id=(?P<trace_id>\d+)\s+"
    r"service=(?P<service>\S+)\s+"
    r"caller=(?P<caller>\S+)\s+"
    r"uid=(?P<uid>-?\d+)\s+"
    r"pid=(?P<pid>-?\d+)\s+"
    r"code=(?P<code>-?\d+)\s+"
    r"flags=(?P<flags>-?\d+)\s+"
    r"size=(?P<size>-?\d+)\s+"
    r"token=(?P<token>.*?)\s+"
    r"detail=(?P<detail>\S+)\s+"
    r"handled=(?P<handled>true|false)\s+"
    r"t=(?P<t>-?\d+)"
)


@dataclass(frozen=True)
class Key:
    service: str
    token: str
    code: int


def main() -> int:
    parser = argparse.ArgumentParser(description="Build token+code contract map from trace logs.")
    parser.add_argument("input", type=Path, help="Input logcat text file")
    parser.add_argument("-o", "--output", type=Path, required=True, help="Output JSON file")
    args = parser.parse_args()

    text = args.input.read_text(encoding="utf-8", errors="replace")
    rows = []
    for line in text.splitlines():
        m = TRACE_RE.search(line)
        if not m:
            continue
        rows.append(
            {
                "trace_id": int(m.group("trace_id")),
                "service": m.group("service"),
                "token": (m.group("token") or "").strip(),
                "code": int(m.group("code")),
                "detail": m.group("detail"),
                "handled": m.group("handled") == "true",
            }
        )

    index: dict[Key, dict] = {}
    order: list[Key] = []
    for r in rows:
        key = Key(r["service"], r["token"], r["code"])
        if key not in index:
            index[key] = {
                "service": r["service"],
                "token": r["token"],
                "code": r["code"],
                "count": 0,
                "first_trace_id": r["trace_id"],
                "details": {},
                "handled_seen": False,
            }
            order.append(key)
        item = index[key]
        item["count"] += 1
        item["details"][r["detail"]] = item["details"].get(r["detail"], 0) + 1
        item["handled_seen"] = item["handled_seen"] or r["handled"]

    ordered = sorted(
        (index[k] for k in order),
        key=lambda x: (x["first_trace_id"], -x["count"]),
    )

    payload = {
        "source": str(args.input),
        "total_rows": len(rows),
        "contracts": ordered,
    }
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {args.output} contracts={len(ordered)} rows={len(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

