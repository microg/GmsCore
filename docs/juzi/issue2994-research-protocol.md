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

## Reproducibility Artifacts
- `docs/juzi/rcs_trace_analyzer.py`
- `docs/juzi/rcs_contract_map_builder.py`
- `docs/juzi/rcs_patch_suggester.py`
- `docs/juzi/rcs_blocker_report_template.md`

## Evaluation Criteria
- Deterministic blocker ranking from independent runs.
- Patch scope limited to target row and direct dependencies.
- No unconditional success responses.
- Unsupported rows remain fail-closed.

## Expected Research Output
1. A blocker report naming first blocker row.
2. A minimal patch plan bound to that row.
3. A post-patch delta report showing whether blocker rank shifts or disappears.

