Maintainers,

I think the key blocker in #2994 is being approached from the wrong angle.
This does not look like a "single provisioning response" issue. It looks like a contract-completeness issue across the RCS + CarrierAuth binder boundary.

Most previous attempts tried to force a terminal state. That hides the real blocker and fails under review.

I am taking a different route:

1. Instrument exact contract rows (`token`, `transaction code`, call order, caller uid/pid).
2. Identify the first blocking row in the real flow.
3. Implement only that row + direct dependencies, keep unknown paths fail-closed.

No unconditional success stubs, no inflated compatibility claims.

If this direction matches your expectations, I will post the first blocker row report in the PR and keep the next patch narrowly scoped to that contract.

