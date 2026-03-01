# PR #3294 Technical Brief (RCS)

## What this revision changes
- Introduces contract-level tracing for RCS and CarrierAuth binder calls:
  - caller package / uid / pid
  - transaction code / flags / payload size
  - interface token
  - deterministic trace id
- Replaces synthetic success behavior with explicit unavailable/failure semantics.
- Adds reproducible BrowserStack trace runbook and a local analyzer to pinpoint first blocking contract row.

## Why this is different from prior failed attempts
- Does not claim RCS success through static XML or forced status responses.
- Treats unknown/unsupported paths as unsupported, not success.
- Produces evidence maintainers can inspect and reproduce.

## Expected outcome
- A clear first blocking candidate `(token, code, detail)` from real runs.
- Narrow, reviewable next-step implementation focused on that exact contract row.

## Known limits
- This revision is an instrumentation + contract-hardening step.
- End-to-end provisioning is intentionally not claimed without trace-backed proof.

