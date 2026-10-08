# Dott callback delivery before remote cleanup

Target: microg/GmsCore PR #3841, owned fork `woahwhattheheck/GmsCore`, branch `luna2-dott-session-transport-2851-20261001`.

Base commit: `7a648d5fe5f073c61ef7b202aef4c74dd329f4be`. The original `DroidGuardServiceImpl.kt` blob is `e8336a1fb3a613a03d214fd50255b84cb2c5a364`.

## Change

`guardWithRequest` now attempts its result callback before closing the handle. An outer `finally` still closes the handle if result generation or callback delivery throws. Existing snapshot/init fallback, callback exception logging, and executor rejection handling remain in place.

The original method waited for `RemoteDroidGuardSession.close()` and its HTTP response before delivering a completed result. Because the service calls its local handle object directly, the AIDL declaration of `close` as `oneway` does not make that call asynchronous.

## Focused replay

The same fixture compiled and executed the complete original and repaired production service, plus the current production remote handle, initialization, session, HTTP, and fallback classes. It used a real loopback HTTP server and disposable Android/AIDL/context/preference stubs selecting Network mode.

| Behavior | Original | Repaired |
| --- | --- | --- |
| Successful result delivered while close response remains latched | Fail | Pass |
| Fallback result delivered while close response remains latched | Fail | Pass |
| Throwing callback attempted once; handle closed once | Pass | Pass |
| Rejected executor produces exactly one fallback callback | Pass | Pass |

The two ordering cases use latches, not a timing threshold. Each still verifies one callback, one close, and the expected result bytes.

## Controlled latency

The loopback server deliberately delayed each close response by 300 ms. Time was measured from writing the snapshot response to invoking the result callback. Three samples were run per version; timing values are observations, not test pass criteria.

| Version | Sample 1 | Sample 2 | Sample 3 | Median |
| --- | ---: | ---: | ---: | ---: |
| Original | 395.867 ms | 391.153 ms | 385.121 ms | 391.153 ms |
| Repaired | 42.407 ms | 41.999 ms | 45.822 ms | 42.407 ms |

## Scope of the evidence

This demonstrates callback ordering and latency in the stated controlled scenario. Cleanup still occupies the same executor worker, so it does not demonstrate improved sustained throughput. A blocking callback also postpones cleanup until it returns. The replay does not exercise Android Binder, Embedded mode, a production remote server, or physical Dott unlock/ride acceptance. It used JDK 17 and Kotlin 2.0.21; full repository-toolchain and Android compatibility require the project build.

The reusable fixture and execution logs are provided separately from the one-file production repair. No production helper, new worker, Gradle change, or copied source fixture is required in the sponsor PR.

## Reproduction package

- [Replay package](https://github.com/woahwhattheheck/GmsCore/blob/1a837e41545f3b45bf4844eb86af999fdd98f167/docs/validation/dott-callback-order/dott3841-callback-replay.tar.gz?raw=true)
- [Requirements and commands](https://github.com/woahwhattheheck/GmsCore/blob/1a837e41545f3b45bf4844eb86af999fdd98f167/docs/validation/dott-callback-order/README.md)
- [Recorded results and production source hashes](https://github.com/woahwhattheheck/GmsCore/blob/1a837e41545f3b45bf4844eb86af999fdd98f167/docs/validation/dott-callback-order/replay-results.json)

Archive SHA-256: `84f9ce66a7178528a9953561ae63a6fc7550156f3c6c4b4208c8cdba7f96eeb3`.

The existing [integrated Android report](https://github.com/woahwhattheheck/GmsCore/blob/f0f8d34ced70fc6caa3ec370374fd4598941145e/docs/validation/dott-2851-integrated-2026-10-04.md) identifies the separately tested pre-change source and APK. The callback-order change is supported by the JVM replay described here.
