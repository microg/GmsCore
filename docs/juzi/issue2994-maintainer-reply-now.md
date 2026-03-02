Thanks for the clear questions.

Direct answers:

1. No, I have not yet validated this on a physical SIM-equipped device running a microG-capable custom ROM.
2. No, this patchset has not yet been validated end-to-end in that same carrier-backed physical environment.

Current PR scope is Phase 1 only: protocol-layer isolation and deterministic binder-contract analysis.
I am not claiming final carrier-backed RCS activation from this patchset yet.

What this PR currently contributes:
- binder instrumentation for RCS/CarrierAuth contract paths,
- ranked blocker evidence from repeated unhandled rows,
- minimal fail-closed patch iterations per `(token, code)` row.

I want to keep this PR active and move directly into Phase 2 validation.
If you share a preferred device/ROM matrix, I will align to it and report results in a structured format.
