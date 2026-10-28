# Remote session operation ordering

A remote DroidGuard session carries state between snapshots. Its HTTP operations
must finish in sequence: overlapping snapshots can consume that state out of
order, and closing during a snapshot can retire the server session before that
snapshot finishes.

`RemoteDroidGuardSession.begin`, `snapshot`, and `close` now synchronize on the
session instance. The existing HTTP timeouts still bound each network operation.
Other session instances remain independent. Closing waits for an in-flight
operation; already-open handle initialization and callback delivery logic are
unchanged.

## Executed checks

Source before the change: `4fec1b39d764f83e9e008503fab22cf1e7bded3b`.
The same session and HTTP source blobs remain on its later interop successor
`655881645bf44190134d204dce36f746446a06d9`.

The complete production session and HTTP classes were compiled directly with
cached Kotlin 2.2.21, JDK 17.0.20, and JVM target 1.8. A real JDK loopback HTTP
server held the first snapshot response behind a latch while another caller
attempted a snapshot or close. A separate session completed begin/snapshot/close
while the first remained held.

| Operation | Original source | Updated source |
| --- | --- | --- |
| Second snapshot while first is in flight | Reaches server before first response | Reaches server after first response |
| Close while snapshot is in flight | Reaches server before snapshot response | Reaches server after snapshot response |
| Separate session during held snapshot | Completes | Completes |

Original replay exited 1; updated replay exited 0. Both returned the expected
snapshot response after release. A 150 ms bounded observation followed the
second-caller start latch; no latency or sustained-throughput claim is made.

The maintained `RemoteDroidGuardSessionTest` was compiled with the same actual
production classes and JUnit 4.13.2, then executed directly: **4 tests, 0 failures**,
exit 0 in 0.351 seconds. Its two prior cases remain, with two focused ordering
regressions following the existing recording-connection style.

For the repository's Android test task, the corresponding selection is:

```sh
./gradlew :play-services-droidguard-core:testDebugUnitTest --tests org.microg.gms.droidguard.core.RemoteDroidGuardSessionTest
```

That Gradle command and an APK rebuild were not run for this change. These are
JVM session/HTTP checks; Android Binder, live remote-device Play Integrity and
physical Dott unlock/ride acceptance remain separate from this result.
