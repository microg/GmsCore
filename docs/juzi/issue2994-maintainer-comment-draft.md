Hi maintainers,

I reviewed the failed attempts on this bounty and I am adjusting the implementation strategy to be evidence-driven rather than mock-driven.

For PR #3294, I am removing success-forcing behavior and focusing on a compatibility + tracing path:

1. Binder contract tracing for RCS/provisioning calls (token, transaction code, call order, response semantics).
2. Fail-closed behavior for unsupported contract versions (no fake `STATUS_OK` on unknown paths).
3. A reproducible validation matrix (device, ROM, Messages version, SIM/carrier, logs).

Before I finalize the next update, I want to align with your review expectations on three points:

- Which concrete signals do you consider sufficient to prove real RCS readiness (beyond UI state text)?
- For the bounty scope, is one fully documented modern-device success path acceptable as phase 1, followed by broader compatibility in phase 2?
- Are there specific binder interfaces or state transitions you want explicitly logged in the first reviewable revision?

I will keep the next revision narrow, testable, and directly tied to the acceptance criteria in #2994.

