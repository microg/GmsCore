Maintainers,

Phase 2 validation tooling is now in place in this branch.

- `docs/juzi/rcs_log_extract.py`
- `docs/juzi/run_phase2_validation_bundle.sh`

Given a raw device logcat, the pipeline now produces:
- `rcs_report.md`
- `rcs_contracts.json`
- `rcs_patch_plan.md`
- `rcs_research_brief.md`
- `rcs_research_artifacts.zip`

I will post the first carrier-backed run output in this PR using that exact format.
