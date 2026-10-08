# Leave application and reconnect

## Behavior

After a successful `leaveApplication`, the device controller used to retain
`attachedApplicationId` and `attachedSessionId`. A later channel drop therefore
queued a join of the application the client had explicitly left.

`SessionCallbacks.onLeaveApplicationResult` now clears those reconnect identities
on success before delivering the original listener result. A failed leave retains
the identities for recovery. The existing current-session callback guard remains
in place, and the device connection can still reconnect after a successful leave.

## Focused execution

The maintained `CastDeviceControllerReconnectTest` has two additional lifecycle
cases: successful leave must not queue an application rejoin after a connection
loss; failed leave must retain the existing rejoin behavior. Its four existing
reconnect cases remain unchanged.

The same six cases were compiled and executed against both controller versions:

| Controller | Result | Successful-leave rejoin queue |
| --- | --- | --- |
| Baseline `85c7ce282908fe1b2aa89302ae37905905cec550` | 5 pass, 1 failure | 1 unwanted join |
| Changed `683ea62ca1bda672d24614f9c2893d4e596e2188` | 6 pass, 0 failures | 0 joins |

Baseline failure: `A successful leave must not queue another application join
expected:<0> but was:<1>`. The unsuccessful-leave control retains one queued join.
Both cases retain the original listener result and device-reconnected callback.

Execution used OpenJDK 17.0.20.1, Kotlin 2.2.21 targeting JVM 1.8, JUnit 4.13.2 and
Robolectric 4.14.1 with an instrumented Android API 28 runtime. The complete
current `CastChannel.kt`, `CastDeviceSession.kt`, `CastDeviceControllerImpl.kt`
and reconnect test were compiled together. Current `JoinOptions.java` was
compiled at Java 8 compatibility because the retained dependency jar predates its
connection-type accessor. Generated Cast protobuf classes, Wire 6.4.6, Okio 3.17.0
and the remaining Android/SDK dependencies were reused from the existing build.
Robolectric resolved the instrumented SDKs from local files with offline mode;
no dependencies or provider data were downloaded.

The source baseline was PR head `0b54bf4a4b04f24cac9262f6a875e91545155b59`.
The controller and preexisting test blobs remained unchanged at integration head
`38c43772910ae1d9b32813f9266f69f5974b3d90`.

The test invokes the production session callbacks with its existing controlled
executor. It proves the controller's leave/reconnect behavior and callback
results. It does not open a receiver socket or establish TLS, physical-device,
APK/full-build, or performance acceptance. The existing receiver demonstration
and submission process remain separate.
