# Issue #2994 New Viewpoint (What Others Missed)

## Core thesis
This is not a "device spoofing" problem.
This is a **contract-completeness** problem between Google Messages and microG at the RCS + CarrierAuth boundary.

## Why most attempts fail
- They try to force terminal state (`Connected`) instead of implementing required intermediate contracts.
- They treat provisioning as a single response, while real flow is a multi-step state machine with strict call ordering.
- They cannot show which exact `(interface token, transaction code)` is the first hard blocker.

## New technical angle
Build a **Contract Completion Layer** (CCL), not a mock-success layer:

1. **Contract Witness**
   - Record exact binder contract rows:
     - token
     - code
     - call order
     - caller uid/pid
   - Output deterministic traces.
   - Emit automatic `blocker_candidate` signals when the same unhandled row repeats.

2. **Minimal Completion**
   - Implement only the first blocking contract row and its direct dependencies.
   - Keep all unknown rows fail-closed.
   - No unconditional `STATUS_OK`.

3. **Version Drift Guard**
   - Detect token/code drift across Google Messages versions.
   - Mark unsupported variants explicitly instead of pretending compatibility.

## Why this is valuable to maintainers
- Gives a reproducible path to reason about real failures.
- Reduces review risk by avoiding broad or deceptive behavior.
- Produces a mergeable progression:
  - instrumentation -> first contract completion -> compatibility expansion.

## Deliverable shape for next PR update
- RCS/CarrierAuth trace instrumentation (already in branch).
- First blocking row implementation only (strictly scoped).
- Trace report attached in PR with blocker row + result.
- Known limits section (no inflated claims).
