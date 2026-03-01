Maintainers,

For #2994, I am treating this as a contract-completeness research problem, not a UI-state workaround.

Working hypothesis:
- the setup loop is caused by one or more missing/incorrect binder contract rows at the RCS + CarrierAuth boundary.

Method in this revision:
1. instrument binder rows (`token`, `code`, caller package/uid/pid, handled/unhandled),
2. detect repeated unhandled rows as `blocker_candidate`,
3. rank blockers and patch only rank-1 row in the next iteration.

I am intentionally not claiming end-to-end success in this step.
The goal is to produce reproducible blocker evidence and a minimal patch target that can be reviewed objectively.

Artifacts generated from a run:
- blocker trace report
- contract map JSON
- next patch suggestion
- research brief

If this review direction is acceptable, I will post the first rank-1 blocker row and submit a narrow completion patch for that exact `(token, code)` pair.

