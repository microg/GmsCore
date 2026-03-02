# Issue #2994 Phase 2 Validation Matrix

## Goal
Validate whether the current patchset changes real RCS setup behavior on physical SIM-backed devices.

## Test Matrix (minimum)
- Device: Pixel 6 / Pixel 7 / Samsung S22 (at least one required for first pass)
- Android: 13 or 14
- ROM: one microG-capable ROM
- microG build: patched build from this PR branch
- Google Messages: current production version
- SIM: active, carrier-provisioned for RCS region

## Execution Steps
1. Flash/install ROM and microG baseline.
2. Install patched microG build.
3. Install/clear data for Google Messages.
4. Trigger RCS setup from Messages settings.
5. Capture logs for tags:
`RcsApiService`, `CarrierAuthService`, and related provisioning tags.
6. Record end state in Messages:
`Connected` / `Setting up` / explicit error.

## Acceptance Criteria
- Pass:
Messages reaches stable RCS `Connected` with no repeated rank-1 unhandled blocker row in logs.
- Partial:
Setup progresses but stalls with a new ranked blocker row.
- Fail:
No setup progression and same blocker row persists.

## Required Report Fields
- Device model
- ROM name + version
- Android version
- Carrier + country
- Messages version
- Result state
- Top blocker row (if any): `token`, `code`, `detail`, repeat count
- Log excerpt lines

