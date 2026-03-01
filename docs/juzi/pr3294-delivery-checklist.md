# PR #3294 Delivery Checklist

## Scope discipline
- [ ] RCS-related files only.
- [ ] Remove unrelated auth/identity edits from this PR.
- [ ] No UI-only or cosmetic commit noise.

## Technical correctness
- [ ] No hardcoded IMEI/IMSI/line number payloads.
- [ ] No unconditional `STATUS_OK` response path.
- [ ] Unsupported interface/version returns explicit failure with trace id.
- [ ] Parcel read/write order matches expected contract.

## Evidence quality
- [ ] Include a state-transition trace, not just final app screenshot.
- [ ] Include device/ROM/Messages/SIM metadata.
- [ ] Include at least one negative-path log (expected failure handling).
- [ ] Include rollback/safety note if upstream contract changes.

## Reviewability
- [ ] One concern per commit (small and reviewable diffs).
- [ ] Commit messages explain "why", not only "what".
- [ ] PR body lists known limitations explicitly.
- [ ] Comment tone stays technical, no hype language.

