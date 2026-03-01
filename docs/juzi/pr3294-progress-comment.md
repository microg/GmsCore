Maintainers,

Quick progress update on #2994:

- I moved the RCS shim to a contract-first workflow:
  - binder row tracing (`token`, `code`, caller package/uid/pid, handled/unhandled),
  - automatic repeated-blocker detection (`blocker_candidate`),
  - blocker ranking for deterministic patch prioritization.

- I also added a narrow completion mode for rank-1 blocker rows:
  - `COMPLETE_*_UNAVAILABLE` is applied only to selected contract rows,
  - all other rows remain fail-closed.

This is intentionally not a broad success stub.
The goal is to produce reproducible blocker evidence, then patch exactly one contract row at a time.

Next step I am preparing:
- post the first ranked blocker row from the latest run,
- submit a minimal completion patch bound to that row and direct dependencies only.

