# Dott #2851 integrated build receipt

The existing DroidGuard contribution in [PR #3841](https://github.com/microg/GmsCore/pull/3841) completed the three requested regression classes and one VtmDefaultDebug APK build on 2026-10-04.

## Executed source

- Product commit: `7a648d5fe5f073c61ef7b202aef4c74dd329f4be`.
- Product tree: `24896870026d0edb04989c9627b4a1dc0586670d`.
- Workflow/control commit: `3e8f39598ae7d76c053ffac80c64070b1f508b3b`.
- [Workflow run 37193312230](https://github.com/woahwhattheheck/GmsCore/actions/runs/37193312230), attempt 1, job `111409876131`: **success**.

The workflow checked out and verified the product commit and tree. It then applied only the retained session lifecycle regression patch v2 to `RemoteDroidGuardSessionTest.kt`. Patch SHA256: `497af7ad875d46645c85eff4f2d02711678dc0513fbf70f4359ccc7636b7fc24` (5,001 bytes). The test blob changed from `608b7680149c62640c91fdf136661384ff6294b7` to `f300e7fad7a6a9649d6c591f36a701b6bf7ee80b`. No production file changed in the execution checkout.

These results describe that product revision plus the stated test-only patch. Subsequent source commits require their own appropriate validation; this receipt does not relabel the APK as a build of a later head.

## Command and environment

```sh
./gradlew --no-daemon --console=plain --max-workers=1 --no-parallel \
  '-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=768m' \
  -Pkotlin.compiler.execution.strategy=in-process \
  :play-services-droidguard-core:testDebugUnitTest \
  --tests org.microg.gms.droidguard.core.RemoteDroidGuardInitializationTest \
  --tests org.microg.gms.droidguard.core.RemoteDroidGuardSessionTest \
  --tests org.microg.gms.droidguard.core.RemoteDroidGuardHttpClientTest \
  assembleVtmDefaultDebug
```

The runner was Ubuntu 24.04.5 with Gradle 8.13, Temurin/OpenJDK `17.0.20.1`, Android API 35, and build-tools 35.0.0. `GRADLE_MICROG_VERSION_WITHOUT_GIT=1` was set. The source build configuration was used without a Kotlin or production-source compatibility override.

## Results

| Test class | Cases | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `RemoteDroidGuardInitializationTest` | 5 | 0 | 0 | 0 |
| `RemoteDroidGuardSessionTest` | 5 | 0 | 0 | 0 |
| `RemoteDroidGuardHttpClientTest` | 2 | 0 | 0 | 0 |
| **Total** | **12** | **0** | **0** | **0** |

The Gradle log records `:play-services-droidguard-core:testDebugUnitTest` as executed, without a `FROM-CACHE` or `UP-TO-DATE` suffix. The collector required all three expected classes and all 12 cases, rejected skips, and found no problems. Across the complete build, Gradle reported 1,749 actionable tasks: 1,220 executed and 529 from cache.

The command exited 0. Gradle reported `BUILD SUCCESSFUL in 14m 17s`; the surrounding command receipt measured 858.87 seconds. These are build elapsed times, not an application performance benchmark.

## APK and retained evidence

- APK: `com.google.android.gms-252432000.apk`, variant `vtmDefaultDebug`.
- APK size: **107,313,927 bytes**.
- APK SHA256: `d40be530096efce543222918b062abfe2310e1508fddb0393c2603ed302e5749`.
- [Artifact 11300192593](https://github.com/woahwhattheheck/GmsCore/actions/runs/37193312230/artifacts/11300192593) contains the APK, Gradle log, source and patch receipts, actual JUnit XML, summary, toolchain log, and checksums.
- Artifact ZIP: **102,840,134 bytes**; SHA256 `4e970c513674419a2279a8b3bf3ac2a9929ffe629010eacd61662048b4259786`.
- GitHub reports artifact expiry at **2026-11-03 10:02:33 UTC**. This committed receipt preserves source identity, results, and hashes.

The [machine-readable receipt](dott-2851-integrated-2026-10-04.json) includes each JUnit XML file's byte count and SHA256.

## Scope

This completes the requested software integration run for the stated revision and test patch. It does not establish Binder/device execution, a real remote Play Integrity service result, a Dott unlock/ride, sponsor acceptance, or payment. The existing contribution and submission remain PR #3841.
