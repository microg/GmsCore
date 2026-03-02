#!/usr/bin/env python3
"""
Parse microG RCS binder traces from logcat text and emit a concise markdown report.
"""

from __future__ import annotations

import argparse
import re
from collections import Counter
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

TRACE_DECISION_RE = re.compile(
    r"trace_decision\s+id=(?P<trace_id>\d+)\s+"
    r"detail=(?P<detail>\S+)\s+"
    r"handled=(?P<handled>true|false)\s+"
    r"token=(?P<token>.*?)\s+"
    r"code=(?P<code>-?\d+)"
)

TAG_RE = re.compile(r"\b(RcsApiService|CarrierAuthService)\b")

BLOCKER_RE = re.compile(
    r"blocker_candidate\s+"
    r"service=(?P<service>\S+)\s+"
    r"caller=(?P<caller>\S+)\s+"
    r"token=(?P<token>\S+)\s+"
    r"code=(?P<code>-?\d+)\s+"
    r"detail=(?P<detail>\S+)\s+"
    r"repeated=(?P<repeated>\d+)"
)

BLOCKER_SUMMARY_RE = re.compile(
    r"blocker_summary\s+"
    r"rank=(?P<rank>\d+)\s+"
    r"repeated=(?P<repeated>\d+)\s+"
    r"first_trace=(?P<first_trace>\d+)\s+"
    r"last_trace=(?P<last_trace>\d+)\s+"
    r"service=(?P<service>\S+)\s+"
    r"caller=(?P<caller>\S+)\s+"
    r"token=(?P<token>\S+)\s+"
    r"code=(?P<code>-?\d+)\s+"
    r"detail=(?P<detail>\S+)"
)


@dataclass
class TraceRecord:
    line_no: int
    trace_id: int
    service: str
    caller: str
    uid: int
    pid: int
    code: int
    flags: int
    size: int
    token: str
    detail: str
    handled: bool
    elapsed_ms: int


def parse_records(text: str) -> list[TraceRecord]:
    out: list[TraceRecord] = []
    for i, line in enumerate(text.splitlines(), start=1):
        m = TRACE_RE.search(line)
        if not m:
            m2 = TRACE_DECISION_RE.search(line)
            if not m2:
                continue
            out.append(
                TraceRecord(
                    line_no=i,
                    trace_id=int(m2.group("trace_id")),
                    service=infer_service(line),
                    caller="<unknown>",
                    uid=-1,
                    pid=-1,
                    code=int(m2.group("code")),
                    flags=-1,
                    size=-1,
                    token=(m2.group("token") or "").strip(),
                    detail=m2.group("detail"),
                    handled=(m2.group("handled") == "true"),
                    elapsed_ms=-1,
                )
            )
            continue
        out.append(
            TraceRecord(
                line_no=i,
                trace_id=int(m.group("trace_id")),
                service=m.group("service"),
                caller=m.group("caller"),
                uid=int(m.group("uid")),
                pid=int(m.group("pid")),
                code=int(m.group("code")),
                flags=int(m.group("flags")),
                size=int(m.group("size")),
                token=(m.group("token") or "").strip(),
                detail=m.group("detail"),
                handled=(m.group("handled") == "true"),
                elapsed_ms=int(m.group("t")),
            )
        )
    return out


def infer_service(line: str) -> str:
    m = TAG_RE.search(line)
    if not m:
        return "unknown"
    return "rcs" if m.group(1) == "RcsApiService" else "carrier_auth"


def first_blocking_candidate(records: list[TraceRecord]) -> TraceRecord | None:
    for rec in records:
        if rec.detail in {"observe_config_request", "observe_generic_request"}:
            return rec
    return None


def build_report(records: list[TraceRecord], source: Path) -> str:
    lines: list[str] = []
    lines.append("# RCS Trace Report")
    lines.append("")
    lines.append(f"- Source: `{source}`")
    lines.append(f"- Parsed trace rows: **{len(records)}**")
    lines.append("")

    if not records:
        lines.append("No RCS trace rows found. Ensure logcat includes `RcsApiService` traces.")
        return "\n".join(lines)

    by_detail = Counter(rec.detail for rec in records)
    by_contract = Counter((rec.service, rec.token, rec.code, rec.detail, rec.handled) for rec in records)

    lines.append("## Detail Distribution")
    for detail, count in by_detail.most_common():
        lines.append(f"- `{detail}`: {count}")
    lines.append("")

    lines.append("## Top Contract Rows")
    for (service, token, code, detail, handled), count in by_contract.most_common(10):
        token_preview = token if len(token) <= 96 else token[:93] + "..."
        lines.append(
            f"- `{service}` code=`{code}` detail=`{detail}` handled=`{handled}` token=`{token_preview}` -> {count}"
        )
    lines.append("")

    blocker_lines = []
    blocker_summaries = []
    for line in source.read_text(encoding="utf-8", errors="replace").splitlines():
        m = BLOCKER_RE.search(line)
        if m:
            blocker_lines.append(
                (
                    m.group("service"),
                    m.group("caller"),
                    m.group("token"),
                    int(m.group("code")),
                    m.group("detail"),
                    int(m.group("repeated")),
                )
            )
        s = BLOCKER_SUMMARY_RE.search(line)
        if s:
            blocker_summaries.append(
                (
                    int(s.group("rank")),
                    int(s.group("repeated")),
                    int(s.group("first_trace")),
                    int(s.group("last_trace")),
                    s.group("service"),
                    s.group("caller"),
                    s.group("token"),
                    int(s.group("code")),
                    s.group("detail"),
                )
            )
    if blocker_lines:
        lines.append("## Auto Blocker Signals")
        for service, caller, token, code, detail, repeated in blocker_lines[-5:]:
            lines.append(
                f"- service=`{service}` caller=`{caller}` token=`{token}` code=`{code}` detail=`{detail}` repeated=`{repeated}`"
            )
        lines.append("")
    if blocker_summaries:
        lines.append("## Blocker Ranking (Service-Side)")
        for rank, repeated, first_trace, last_trace, service, caller, token, code, detail in blocker_summaries:
            lines.append(
                f"- rank=`{rank}` repeated=`{repeated}` first_trace=`{first_trace}` last_trace=`{last_trace}` service=`{service}` caller=`{caller}` token=`{token}` code=`{code}` detail=`{detail}`"
            )
        lines.append("")

    blocker = first_blocking_candidate(records)
    lines.append("## First Blocking Candidate")
    if blocker is None:
        lines.append("- Not detected.")
    else:
        lines.append(
            "- "
            f"trace_id=`{blocker.trace_id}` line=`{blocker.line_no}` service=`{blocker.service}` "
            f"caller=`{blocker.caller}` uid=`{blocker.uid}` pid=`{blocker.pid}` "
            f"code=`{blocker.code}` detail=`{blocker.detail}` handled=`{blocker.handled}` token=`{blocker.token}`"
        )
    lines.append("")

    lines.append("## Suggested Next Step")
    if blocker is None:
        lines.append("- Capture a fresh run with `RcsApiService` tag visible and rerun analyzer.")
    else:
        lines.append(
            "- Implement/adjust only this exact `(token, code)` contract path first; "
            "avoid broad success stubs."
        )
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="Analyze microG RCS binder trace logs.")
    parser.add_argument("input", type=Path, help="Input logcat text file")
    parser.add_argument(
        "-o",
        "--output",
        type=Path,
        help="Output markdown report path (default: print stdout)",
    )
    args = parser.parse_args()

    text = args.input.read_text(encoding="utf-8", errors="replace")
    records = parse_records(text)
    report = build_report(records, args.input)

    if args.output:
        args.output.write_text(report + "\n", encoding="utf-8")
    else:
        print(report)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
