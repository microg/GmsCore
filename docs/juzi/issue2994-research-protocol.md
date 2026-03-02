# Issue #2994 Research Protocol (Maintainer-Facing)

## Research Question
Which exact RCS/CarrierAuth contract row is the first authoritative blocker preventing Google Messages from completing RCS setup in a non-root, locked-bootloader context?

## Hypotheses
1. The blocking point is a **binder contract incompleteness** row, not a single UI-state mismatch.
2. Repeated unhandled rows (`token + code + detail`) can be used as a deterministic blocker signal.
3. Narrow row-by-row completion is safer and more reviewable than broad synthetic success behavior.

## Method
1. Instrument `RcsService` and `CarrierAuthService` binder boundaries.
2. Capture run traces with:
   - trace id
   - token/code
   - caller uid/pid/package
   - handled/unhandled
3. Emit automatic blocker candidates when unhandled rows repeat.
4. Rank blocker rows and patch only rank-1 row in the next iteration.
5. For the selected rank-1 row, use controlled minimal completion mode (`COMPLETE_*_UNAVAILABLE`) before any broad contract expansion.
6. Adjust policy rows through runtime config (no source edit) to keep each iteration auditable.

## Reproducibility Artifacts
- `docs/juzi/rcs_trace_analyzer.py`
- `docs/juzi/rcs_contract_map_builder.py`
- `docs/juzi/rcs_patch_suggester.py`
- `docs/juzi/rcs_blocker_report_template.md`
- `docs/juzi/rcs_policy_overrides.example.json`
- `docs/juzi/run_rcs_research_from_latest_log.sh`
- `docs/juzi/rcs_log_extract.py`
- `docs/juzi/run_phase2_validation_bundle.sh`
- `docs/juzi/process_phase2_inbox.sh`
- `docs/juzi/summarize_phase2_submissions.py`
- `docs/juzi/harvest_downloads_phase2.sh`

## Runtime Policy Control
- Policy override path: `files/rcs_policy_overrides.json` (inside microG app sandbox).
- Supported keys:
  - `enableMinimalCompletion` (boolean)
  - `messagesClients` (string array)
  - `completionRows` (array of `{token, code}` or `{tokenContains, code}`)
- Default behavior remains fail-closed if no override file exists.

## Evaluation Criteria
- Deterministic blocker ranking from independent runs.
- Patch scope limited to target row and direct dependencies.
- No unconditional success responses.
- Unsupported rows remain fail-closed.
- Rank-1 completion does not introduce false-positive connected states.

## Expected Research Output
1. A blocker report naming first blocker row.
2. A minimal patch plan bound to that row.
3. A post-patch delta report showing whether blocker rank shifts or disappears.
