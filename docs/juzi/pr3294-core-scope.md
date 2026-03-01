# PR #3294 Core Scope (Keep It Credible)

## Include in PR
- `play-services-core/src/main/kotlin/org/microg/gms/rcs/RcsShimServices.kt`
- `play-services-core/src/main/kotlin/org/microg/gms/rcs/RcsContractPolicy.kt`
- `docs/juzi/issue2994-breakthrough-roadmap.md`
- `docs/juzi/browserstack-trace-runbook.md`
- `docs/juzi/rcs_trace_analyzer.py`
- `docs/juzi/rcs_contract_map_builder.py`

## Exclude from this PR (separate later)
- `play-services-core/src/main/kotlin/org/microg/gms/auth/credentials/identity/IdentityFidoProxyActivity.kt`
- `play-services-core/src/main/AndroidManifest.xml` launcher additions
- `vending-app/src/main/AndroidManifest.xml` launcher additions
- `play-services-core/src/main/kotlin/org/microg/gms/LauncherActivity.kt`
- `vending-app/src/main/kotlin/com/android/vending/LauncherActivity.kt`

## Why
Issue #2994 is reviewed as an RCS correctness task. Unrelated auth/launcher edits dilute trust and make rejection more likely.
