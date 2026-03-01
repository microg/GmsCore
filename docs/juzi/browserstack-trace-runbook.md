# BrowserStack Trace Runbook (Issue #2994)

## Objective
Produce maintainers-grade evidence for where RCS setup fails, without fake success paths.

## Steps
1. Start a fresh BrowserStack Android session.
2. Install the test APK once.
3. Open Google Messages and trigger RCS setup flow.
4. Open Developer Tools > Logcat.
5. Set app/package filter to `org.microg.gms`.
6. Keep logs running while reproducing setup state changes.
7. Export/copy log text to a local file (example: `rcs_run_01.log`).

## Local analysis
Run:

```bash
cd "/Users/wolaoposongwodediannao/Downloads/codex ai工作文件/ready_for_dispatch/microg_gmscore"
python3 docs/juzi/rcs_trace_analyzer.py /path/to/rcs_run_01.log -o docs/juzi/rcs_run_01_report.md
python3 docs/juzi/rcs_contract_map_builder.py /path/to/rcs_run_01.log -o docs/juzi/rcs_run_01_contracts.json
python3 docs/juzi/rcs_patch_suggester.py docs/juzi/rcs_run_01_contracts.json -o docs/juzi/rcs_run_01_patch_plan.md
```

## What to share in PR
- Device + Android version.
- Google Messages version.
- Whether SIM/carrier profile is present.
- Output of `docs/juzi/rcs_run_01_report.md`.
- Output of `docs/juzi/rcs_run_01_contracts.json`.
- Output of `docs/juzi/rcs_run_01_patch_plan.md`.
- Exact first blocking candidate `(token, code, detail)`.

## Reject patterns to avoid
- Claiming "Connected" with no trace-backed transition chain.
- Any unconditional success parcel response.
- Identity hardcoding that cannot be justified or reproduced.
