# Cast operation completion after shutdown

Connect, launch, join, stop and leave now complete their existing failure callback when the session executor rejects admission. This covers an already-stopped executor and shutdown between the admission check and execute. Accepted work keeps its existing queued behavior; rejection callbacks run on the caller thread, as the text and binary send rejection callbacks already do. This is not automatic session reconnection.

## Executed comparison

[Run 37192494263](https://github.com/woahwhattheheck/GmsCore/actions/runs/37192494263) compiled the complete CastDeviceSession and CastChannel, real generated Wire protocol classes, the new operation-completion tests, and the existing send-completion tests. The only Android host fixture supplies Build.VERSION constants; channel output is an in-memory stream, not a physical receiver.

- Baseline commit: `4609569f0e5a53e7b321ee93f23a078c6f4cecd6`; session blob `87b5bb5f0e95aa0be7c7d46274b1cde04f290dfe`.
- Tested repaired session blob: `b0216ce05143ff33d945427cac71896a6f5beb0a`.
- New test blob: `a4984b2310f5dfff63c5a5fc6fb7a2afc8bb5528`.
- Before: 15 tests, 10 failures, 5 passing controls. After: 15 passed, JUnit time 0.100 seconds.
- The ten cases cover both rejection boundaries for each of the five operations. Controls retain four existing text/binary send cases and completion of three already-admitted application requests exactly once when the real channel closes.
- Kotlin compiler 2.2.21, Temurin 17.0.20.1+1, JUnit 4.13.2, Wire 6.4.6, org.json 20231013. Complete dependency hashes, compiler logs, raw assertions, source and patch are in the artifact.

[Artifact 11299587215](https://github.com/woahwhattheheck/GmsCore/actions/runs/37192494263/artifacts/11299587215), ZIP SHA-256 `df6b5a7f7ff770fa4a9467e5544e1dcc100d37dfaaf2a0e6ef44ae4fc305dca4`. The downloaded ZIP was independently rehashed. Artifact retention ends October 18, 2026. The [reproduction controller](https://github.com/woahwhattheheck/GmsCore/tree/2d65c3af0712436b3071340d29635b63a182ec6e) contains run.py, patch.py and the focused workflow. An earlier run stopped before compilation because the patch helper assumed LF rather than the source's CRLF; the corrected helper preserves source line endings.

## Integration boundary

Publication preserves the subsequent JoinOptions connection-type change `5de13aa4` and AIDL documentation `637a1678`. The current session source was reconstructed and checked against its exact Git blob `44b12986c6f1521a5fa9fa5f8583b793489de5c8` before adding only the five rejection callbacks. Published composed source is `8d0278db08cdb3c6c2aa6d99c9d758f2d6b82554`; the new join overload and connection-type forwarding remain intact. The 15-test execution above is deliberately pinned to the earlier source comparison, not claimed as a second full run on this composition.

This result does not establish Binder/parcel execution, an Android build, phone behavior, a real receiver or the outstanding physical-receiver proof video requested for PR #3845. The unchanged pending-close control also disproves an earlier suspicion that ordinary explicit close necessarily loses already-admitted operations; no drain or timer rewrite was made.
