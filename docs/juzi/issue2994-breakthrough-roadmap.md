# Issue #2994 - Technical Breakthrough Roadmap

## Goal
Deliver a solution that is technically real (can be reproduced), not UI-level simulation.

## Hard reality
If RCS fails due server-side trust gates (carrier/Jibe policy + app integrity coupling), local binder stubs alone will never be sufficient.
So the first breakthrough is not "force connected", but "pinpoint the first authoritative rejection point with proof".

## Breakthrough Hypothesis
The bottleneck is likely one of these:
1. Contract mismatch between Google Messages and microG RCS/CarrierAuth service behavior.
2. Provisioning dependency on upstream trust signal that is not satisfied in current environment.
3. State machine regression where client remains in setup loop because one mandatory transition callback is missing.

## Engineering strategy (what we can actually ship)

### Phase A - Contract Witness (must-have)
- Add precise binder-level tracing:
  - interface token
  - transaction code
  - caller uid/pid
  - call order + timestamp
  - response mode (unavailable/passthrough)
- Add blocker detector:
  - if the same unhandled `(token, code, detail)` repeats, emit `blocker_candidate`.
- Output a deterministic trace set from a real run.

Success criteria:
- We can produce a single ordered trace showing exactly where setup stalls.

### Phase B - Compatibility Adapter (minimal, reviewable)
- Implement only required transactions verified by Phase A.
- No synthetic "all good" response.
- Unsupported versions fail closed with explicit traceable reason.

Success criteria:
- No fake status path.
- Behavior is contract-specific and version-scoped.

### Phase C - Evidence Matrix (maintainer-grade)
- Each run includes:
  - device model + Android version
  - Google Messages version
  - SIM/carrier status
  - key trace excerpt
  - observed UI state
- Include at least one negative path and explain why it fails.

Success criteria:
- Maintainers can reproduce and reason about acceptance/rejection on their side.

## What not to do (auto-reject patterns)
- Hardcoded IMEI/IMSI/device identity payloads.
- Unconditional `STATUS_OK` parcel replies.
- Bundling unrelated auth/system changes in the same PR.
- Claiming end-to-end support without trace-backed evidence.

## PR narrative that can win trust
1. "We instrumented first, then implemented the minimum compatible path."
2. "This revision does not fake provisioning success."
3. "Here is the exact trace where setup blocks today, and here is the targeted adapter behavior for that point."
4. "Known limits are explicit."
