# RCS Provisioning Support for microG GmsCore

Deliverable bundle for the Constellation/Asterism backend work that lets Google
Messages complete RCS provisioning on a rootless microG setup (issue #2994).

## Contents

| File | Description |
| --- | --- |
| `0001-rcs-implement-Constellation-and-Asterism-with-DroidG.patch` | `git format-patch` of the full change set (108 files). |
| `com.google.android.gms-252432035.apk` | Debug build of the `vtmDefault` flavor. |
| `APK.sha256` | SHA-256 checksum of the APK. |
| `CHANGED_FILES.txt` | Commit header plus the complete changed-file list. |
| `README.md` | This document. |

### Apply the patch

```bash
cd /path/to/GmsCore
git am /path/to/0001-rcs-implement-Constellation-and-Asterism-with-DroidG.patch
```

### Verify the APK

```bash
shasum -a 256 -c APK.sha256
```

## Source

- Branch: `feat/rcs-constellation-asterism`
- Commit: `1e448ceb` — *rcs: implement Constellation and Asterism with DroidGuard fixes*
- Base: microG GmsCore `master` (`a03b082d`)
- Upstream reference: microG PR #3784 (Constellation + Asterism + DroidGuard fixes)

## What changed

### New backend services

- `play-services-constellation/` (public API AIDL + parcelables) and
  `play-services-constellation/core/`:
  - `ConstellationApiService` / `IConstellationApiService` implementation.
  - Wire-protocol protos and request builders.
  - Phone-number verification: TS.43 EAP-AKA entitlement, carrier-ID, MO/MT and
    registered SMS verifiers, challenge processor, auth manager, state store.
  - Phone-number-verification preferences UI.
- `play-services-asterism/` and `play-services-asterism/core/`:
  - `AsterismApiService` / `IAsterismApiService` implementation.
  - On-demand consent get/set handlers.

### Wiring and configuration

- `settings.gradle` and `play-services-core/build.gradle`: module includes and
  dependencies.
- `play-services-core/src/main/AndroidManifest.xml`: removed the Asterism and
  Constellation actions from `DummyService`; each service now registers its own
  `START` intent filter:
  - `com.google.android.gms.constellation.service.START`
  - `com.google.android.gms.asterism.service.START`
- `GmsService`: `CONSTELLATION (155)` and `ASTERISM (199)` were already present;
  no routing change was required.
- `SettingsContract.kt` / `SettingsProvider.kt`: new `Constellation` settings
  namespace (verification enabled, last package, last-used timestamp,
  last-successful).
- `PhenotypeService.kt`: Messages MLS provisioning flags and the IMS
  `RcsProvisioning__min_gmscore_version_for_upi_without_acs_fallback_met` flag.
- `SelfCheckFragment.java`: requests `READ_PHONE_NUMBERS`, `READ_SMS`,
  `SEND_SMS` alongside the existing phone/SMS permissions.
- `SettingsFragment.kt`, `nav_settings.xml`, `preferences_start.xml`: "Phone
  number verification" settings entry and navigation destination.

### DroidGuard and Instance ID fixes

- `DroidGuardVmCache.kt`: stock-compatible `dg_cache` directory name and
  uppercase VM cache keys.
- `HandleProxyFactory.kt`, `NetworkHandleProxyFactory.kt`: use the cache fix and
  report Google account presence (`GET_ACCOUNTS`).
- `DroidGuardInitReply.java`: tolerate partially-null init replies.
- `InstanceID.requestToken()`: real register-token flow instead of
  `UnsupportedOperationException`.
- Added DroidGuard unit tests and the Kotlin test source set.

No stubs remain in the new modules. The patch is rootless: it relies only on
standard Android APIs and microG's system-signature permissions.

## Build and test validation

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

./gradlew :play-services-asterism:assembleDebug \
          :play-services-constellation:assembleDebug \
          :play-services-asterism-core:assembleDebug \
          :play-services-constellation-core:assembleDebug \
          :play-services-core:assembleVtmDefaultDebug

./gradlew :play-services-constellation-core:testDebugUnitTest \
          :play-services-droidguard:test
```

Results:

- All five assemble tasks: **BUILD SUCCESSFUL**.
- Constellation unit tests: 14 tests, 0 failures
  (SyncRequestPhoneNumberResolver 5, Fips186Prf 3, RcsAutoConsent 2,
  MtSmsVerifier 4).
- DroidGuard tests: `DroidGuardVmCacheTest` (3) + `DroidGuardInitReplyTest` (3),
  0 failures in both debug and release.
- Merged `vtmDefaultDebug` manifest: 125 services, no duplicate service
  entries; exactly one Asterism and one Constellation service with the expected
  intent filters; neither action remains on `DummyService`.

## Device test steps (rootless)

1. Install the APK. This requires native signature spoofing provided by your
   ROM or microG companion (no root/Magisk):
   ```bash
   adb install -r com.google.android.gms-252432035.apk
   ```
2. Open **microG Settings → Self-Check** and grant:
   `READ_PHONE_NUMBERS`, `READ_PHONE_STATE`, `READ_SMS`, `RECEIVE_SMS`,
   `SEND_SMS`.
3. Open **microG Settings → Phone number verification**, enable it, and allow
   `com.google.android.apps.messaging`.
4. Start log capture, then open **Messages → Settings → RCS chats →
   Enable/verify**:
   ```bash
   adb logcat -c
   adb logcat -s C11NApiService AsterismApiService GetIidToken \
                    GetPnvCapabilities VerifyPhoneNumber Ts43Verifier \
                    EapAkaService MtSmsVerifier MoSmsVerifier DroidGuard
   ```
5. Confirm that:
   - `C11NApiService` registers/binds the Constellation Binder,
   - `AsterismApiService` returns consent,
   - `GetPnvCapabilities` and `GetIidToken` return results,
   - Messages RCS setup completes without hanging.
   `dumpsys activity services com.google.android.gms` should show both services
   bound under their `START` actions.

## Notes

- The APK is the `vtmDefault` flavor debug build. Build the flavor matching your
  device if needed (`mapbox`/`hms`).
- The bundle intentionally excludes the unrelated Nearby/Sharing work that may
  still be present in the local GmsCore working tree.
- On non-carrier builds, SMS-based phone-number verification may require a
  carrier that supports TS.43/EAP-AKA; the added Phenotype flag allows UPI
  verification without the unusable ACS fallback.
