Maintainers,

Quick progress update on #2994:

- I moved the RCS shim to a contract-first workflow:
  - binder row tracing (`token`, `code`, caller package/uid/pid, handled/unhandled),
  - automatic repeated-blocker detection (`blocker_candidate`),
  - blocker ranking for deterministic patch prioritization.

- I also added a narrow completion mode for rank-1 blocker rows:
  - `COMPLETE_*_UNAVAILABLE` is applied only to selected contract rows,
  - all other rows remain fail-closed.
- I added a runtime policy layer for the row-selection logic:
  - completion rows and client allowlist are now externalized via `rcs_policy_overrides.json`,
  - this allows deterministic iteration without repeatedly changing core routing code,
  - default behavior remains strict and fail-closed when no override file is present.
- I fixed the research pipeline parser so it accepts both full `trace ...` rows and lightweight `trace_decision ...` rows:
  - blocker ranking and contract maps are now generated from current instrumentation logs without manual reformatting.

This is intentionally not a broad success stub.
The goal is to produce reproducible blocker evidence, then patch exactly one contract row at a time.

Next step I am preparing:
- post the current top-ranked blocker row from a fresh run,
- submit a minimal row patch bound to that blocker and direct dependencies only.
