# Cast media reply admission

A tracked SEEK or STOP could receive a MEDIA_STATUS response for another native media item. The controller rejected the response, but first replaced the current native ID, logical item ID, playback state, position and duration. The matching request ID did not make that other item the requested item.

The controller now checks a positive native-ID mismatch before changing state. The remaining response handling stays in its previous order. An authoritative empty status still clears ended playback; empty STOP acknowledgements, matching item replies, LOAD replies and unsolicited updates retain their behavior.

Google's [media message contract](https://developers.google.com/cast/docs/media/messages) distinguishes the request ID used for response correlation from the media session ID identifying a particular playback.

## Executed check — 2026-10-04

Source baseline: `8ab006657480b789354f9ee3f14ee63875c6c2e9`.

| Exact source | Controller blob | Result |
| --- | --- | --- |
| Before | `d615486989a895cabaf0374a35ac9b474251a375` | Compiled; 14 passed, 2 failed |
| After | `ed549ff958a2479e396ba494a6c074f89e6493a2` | Compiled; 16 passed, 0 failures |

Both runs used the same maintained `RemotePlaybackResponseOwnershipTest` blob `8b8216324b105d9afb160d04e8e3669bfcb0810c`. Two added cases exercise mismatched current SEEK/STOP replies and assert that the complete current item remains intact with exactly one error callback, including repeat delivery. One added control preserves current-item empty-status clearing. The previous 13 cases are unchanged.

Execution used the complete controller source, OpenJDK 17.0.20.1, Java 8 target, JUnit 4.13.2, AndroidX and Robolectric 4.14.1 / API 28. The controller and maintained test were compiled with javac and run through JUnitCore using retained compiled peer modules. Android Intent, Bundle and MediaItemStatus executed through Robolectric. No replacement Android classes or copied controller implementation were used.

This is a focused controller run. It is not a new complete Gradle build, APK, Binder/transport integration, performance benchmark, physical Chromecast demonstration, sponsor acceptance or payout. The original PR and BountyHub submission retain those remaining acceptance steps.

The normal maintained-module selector for subsequent integration is:
```sh
./gradlew :play-services-cast-core:testDebugUnitTest --tests org.microg.gms.cast.RemotePlaybackResponseOwnershipTest
```
That Gradle command was not executed for this receipt.

## Receiver application exit — 2026-10-04

The combined parent `0b54bf4a4b04f24cac9262f6a875e91545155b59` added queue/session support and retained the reply-admission repair above. Its `onApplicationDisconnected` callback was empty: a closed receiver application left its native media identity and already-sent control callbacks alive until a later reply or timeout.

The callback now clears the ended native item and snapshots/removes pending controls before completing each with an error. Their scheduled timeouts are cancelled. The logical remote session, selected device connection and pending PLAY/START_SESSION are retained: a successful replacement launch can close the previous application before its own reply arrives. A callback belonging to a replaced device connection changes nothing. Removing controls before invoking callbacks also preserves new requests submitted reentrantly by a callback.

| Exact source | Controller blob | Same maintained test class |
| --- | --- | --- |
| Combined parent | `95f7d43739c914f4640afa038243cf4f8e2a96f2` | 17 passed, 2 failed |
| Application-exit repair | `69ec0bec35db65bb2bcf02bd7ac8832cd142f19b` | 19 passed, 0 failures |

Both complete controller versions compiled and ran with test blob `1af6e7b5374bf319ffe3ae3f44108a7d2b17275e` through the same retained Java/AndroidX/Robolectric API 28 runner described above. The three additional methods cover immediate native-state/control completion, inert old-connection callbacks, and preserved launch/reentrant requests. The earlier cases remain present; these overlapping results are not additive.

This establishes callback completion without waiting for the existing 10-second media-control timer. It does not measure phone/network latency. No new Gradle build, APK or physical receiver acceptance is claimed.


## Start-session receiver confirmation — 2026-10-04

The controller at `510fc4570783b7754af6db5acc8ce01d5cb272a9` returned an ACTIVE result merely because the device channel was connected. It could skip launching or joining the requested receiver application, including an explicit relaunch and its language option.

Removing that four-line shortcut routes connected requests through the existing application launch/join path. A receiver application confirmation completes the start; an application failure reports an error. Existing session-ID validation and result format remain in place.

| Exact source | Controller blob | Same maintained session/queue test class |
| --- | --- | --- |
| Before | `69ec0bec35db65bb2bcf02bd7ac8832cd142f19b` | 8 passed, 2 failed |
| Receiver-confirmed start | `c419e06a1f1bdf208c1187be82152365b1e81897` | 10 passed, 0 failures |

Both complete controller sources compiled and executed with `RemotePlaybackSessionQueueTest` blob `add099052234e3291b034aa167bfe3c322418d17`. The former premature-success test now waits for an actual GET_STATUS/RECEIVER_STATUS exchange through the session callbacks. One additional case checks explicit LAUNCH application/language fields and a receiver LAUNCH_ERROR without false success. The eight unrelated existing cases are retained.

Execution used the same Java/AndroidX/Robolectric API 28 runner, plus retained real CastDeviceSession, CastChannel and generated Wire classes with an in-memory channel output, Wire 6.4.6 and Okio 3.17.0. Cast core dependency SHA-256: `876c08b282f0e9037f68f322844f4b589bd11e9e1dba8cc952efd444c68eb035`. This exercised receiver message framing and callback completion without a socket; the retained Kotlin peer classes are not a new compilation of the current combined branch. The current launch/attach source was also inspected for repeated joins.

The publication parent advances to `622e725f42d568d4c8ff87de42a7abb61497a3f1`, whose intervening change affects Kotlin close-callback isolation, leaving this controller and test preimages intact. Exact-app completion admission remains a separate coordinated source change. No full Gradle build, physical receiver acceptance, performance benchmark or award is claimed.

The maintained selector for subsequent integration is `./gradlew :play-services-cast-core:testDebugUnitTest --tests org.microg.gms.cast.RemotePlaybackSessionQueueTest`; that Gradle task was not executed for this receipt.
