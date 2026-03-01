# Issue #2994 Deep Dive (RCS Bounty)

## 1) What maintainers actually care about
- Works on **locked bootloader** devices.
- No dependency on **root/Magisk**.
- Compatible with **current Google Messages**, not only one frozen old version.
- Must survive **real-world verification**, not only local mock success.

## 2) Why prior attempts are rejected
- Returned local `STATUS_OK` without proving end-to-end provisioning semantics.
- Hardcoded identity data (IMEI/IMSI/model) instead of a defensible compatibility path.
- Missing reproducible evidence matrix (device, ROM, app version, network, SIM profile, logs).
- Broad claims without clear failure boundaries and rollback behavior.

## 3) Current branch risks to fix before trust-building
- `RcsShimServices` must move from observation to contract completion one row at a time, otherwise it remains a diagnostic-only layer.
- Earlier branch variants included identity hardcoding and static provisioning replies; those patterns must stay removed.
- `IdentityFidoProxyActivity` changes are not directly tied to RCS provisioning acceptance criteria.

## 4) Breakthrough direction (engineering, not theater)
- Replace "success-forcing" behavior with **trace-first** architecture:
  - Add a recorder for binder transaction token, code, call order, response latency, and explicit failure reason.
  - Keep behavior deterministic and fail-closed when required dependency is absent.
- Implement **compatibility mediation**, not identity spoofing:
  - Route only known RCS service contracts.
  - Preserve original parcel semantics where possible.
  - Explicitly mark unsupported contract versions.
- Add strict evidence artifacts:
  - Device + ROM + Google Messages version + SIM/carrier + timestamp.
  - Log excerpts that show state transition chain, not only one terminal string.

## 5) Acceptance gates for a credible PR
- No hardcoded personal/device identifiers.
- No unconditional "STATUS_OK" responses.
- No unrelated subsystem edits bundled into RCS PR.
- Clear unsupported-scope statement.
- Reproducible verification table with at least one physical-SIM validation path.
