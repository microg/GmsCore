# PR #3294: RCS Contract Witness and Minimal Compatibility Strategy

## Summary
This revision intentionally avoids synthetic provisioning success paths and introduces a contract-first debugging layer for `RCS` and `CarrierAuth`.

## What changed
- Added binder-level contract witness for `RCS` and `CarrierAuth` services:
  - interface token
  - transaction code
  - caller package / uid / pid
  - payload size
  - deterministic trace id
  - handled/unhandled decision
- Added policy-driven routing (`RcsContractPolicy`) to keep behavior explicit and auditable.
- Unknown/unsupported paths remain fail-closed (`handled=false`) by design.

## Why this approach
Issue #2994 has repeatedly failed with broad mock-based responses that do not survive real verification.
This change takes the opposite path:
- instrument first
- identify first blocker row `(token, code, detail)`
- implement only minimal required completion next

## Non-goals in this revision
- No claim of full end-to-end RCS provisioning success.
- No hardcoded identity payloads.
- No unconditional success responses.

## Reproducibility
Tooling included:
- `docs/juzi/rcs_trace_analyzer.py`
- `docs/juzi/rcs_contract_map_builder.py`
- `docs/juzi/browserstack-trace-runbook.md`

These artifacts are intended to produce a maintainer-reviewable blocker report and reduce speculation.

