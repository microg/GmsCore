# Implement Constellation and Asterism services with DroidGuard fixes for RCS provisioning

## Summary

This PR adds the missing Google Play Services backends required for Google
Messages to complete RCS provisioning on microG: the Constellation
(phone-number verification) and Asterism (on-demand consent) services, plus the
DroidGuard and Instance ID fixes that provisioning depends on. It is fully
rootless and uses only standard Android APIs and microG's system-signature
permissions.

Closes #2994.

## Motivation

Google Messages stalls during RCS setup because microG does not expose the
Constellation and Asterism services. Messages binds
`com.google.android.gms.constellation.service.START` and
`com.google.android.gms.asterism.service.START`, which previously resolved to
`DummyService`, so phone-number verification and consent never complete.
Provisioning also touches DroidGuard (VM cache/account reporting) and
`InstanceID.requestToken()`, both of which were incomplete.

## Changes

### New modules

- **`play-services-constellation`** — public AIDL + parcelable API.
- **`play-services-constellation/core`**
  - `ConstellationApiService` implementing `IConstellationApiService`.
  - Wire-protocol protos and request builders (`constellation.proto`).
  - Phone-number verification: TS.43 EAP-AKA entitlement, carrier-ID, MO/MT and
    registered SMS verifiers, challenge processor, auth manager, state store.
  - Phone-number-verification preferences UI.
- **`play-services-asterism`** — public AIDL + parcelable API.
- **`play-services-asterism/core`**
  - `AsterismApiService` implementing `IAsterismApiService`.
  - On-demand consent get/set handlers.

### Service registration and dispatch

- `settings.gradle`, `play-services-core/build.gradle`: include the new modules
  and depend on their `core` projects.
- `AndroidManifest.xml`: removed the Asterism/Constellation actions from
  `DummyService`; each service now registers its own intent filter:
  - `com.google.android.gms.constellation.service.START`
  - `com.google.android.gms.asterism.service.START`
- `GmsService` already defined `CONSTELLATION (155)` and `ASTERISM (199)`, so no
  dispatcher change was needed.

### Settings and Phenotype

- `SettingsContract` / `SettingsProvider`: new `Constellation` namespace
  (verification enabled, last package, last-used timestamp, last successful).
- `PhenotypeService`: Messages MLS provisioning flags
  (`bugle_phenotype__enableDownloadCertificatesForMlsProvisioning`,
  `bugle_phenotype__enableUploadKeyPackagesForMlsProvisioning`) and the IMS flag
  `RcsProvisioning__min_gmscore_version_for_upi_without_acs_fallback_met`.
- `SettingsFragment`, `nav_settings.xml`, `preferences_start.xml`: "Phone number
  verification" settings entry and navigation destination.

### Permissions / self-check

- `SelfCheckFragment` now requests `READ_PHONE_NUMBERS`, `READ_SMS`, and
  `SEND_SMS` in addition to phone-state and receive-SMS.

### DroidGuard fixes

- New `DroidGuardVmCache`: stock-compatible `dg_cache` directory and uppercase
  VM cache keys (`HandleProxyFactory`, `NetworkHandleProxyFactory`).
- Report Google account presence via `AccountManager` (`GET_ACCOUNTS`).
- `DroidGuardInitReply` tolerates partially-null init replies.
- Added unit tests and the Kotlin test source set.

### Instance ID

- Implement `InstanceID.requestToken()` using the existing registration RPC
  instead of throwing `UnsupportedOperationException`.

No stubs: Binder methods unpack parcels, run real logic, and return proper
result codes.

## Verification

Built with JDK 17:

```bash
./gradlew :play-services-asterism:assembleDebug \
          :play-services-constellation:assembleDebug \
          :play-services-asterism-core:assembleDebug \
          :play-services-constellation-core:assembleDebug \
          :play-services-core:assembleVtmDefaultDebug

./gradlew :play-services-constellation-core:testDebugUnitTest \
          :play-services-droidguard:test
```

- All assemble tasks: **BUILD SUCCESSFUL**.
- Constellation unit tests: 14 tests, 0 failures
  (`SyncRequestPhoneNumberResolver` 5, `Fips186Prf` 3, `RcsAutoConsent` 2,
  `MtSmsVerifier` 4).
- DroidGuard tests: `DroidGuardVmCacheTest` (3) and `DroidGuardInitReplyTest`
  (3), 0 failures in debug and release.
- Merged `vtmDefaultDebug` manifest: 125 services, no duplicate entries; exactly
  one Asterism and one Constellation service with the expected intent filters;
  neither action remains on `DummyService`.

## Device testing (rootless)

1. Install the debug APK; requires native signature spoofing from the ROM or
   microG companion (no root/Magisk).
2. Grant `READ_PHONE_NUMBERS`, `READ_PHONE_STATE`, `READ_SMS`, `RECEIVE_SMS`,
   `SEND_SMS` in microG Self-Check.
3. Enable **microG Settings → Phone number verification** and allow
   `com.google.android.apps.messaging`.
4. Open **Messages → Settings → RCS chats → Enable/verify** while capturing:

   ```bash
   adb logcat -s C11NApiService AsterismApiService GetIidToken \
                    GetPnvCapabilities VerifyPhoneNumber Ts43Verifier \
                    EapAkaService MtSmsVerifier MoSmsVerifier DroidGuard
   ```

5. Expected: Constellation/Asterism Binders bind, capabilities and IID token
   return results, and RCS setup completes without hanging. Check with
   `dumpsys activity services com.google.android.gms`.

## Notes

- Based on the upstream work in #3784 (Constellation/Asterism + DroidGuard),
  rebased and adapted to current `master`. Only RCS-relevant manifest/build
  changes were applied.
- On non-carrier builds, SMS-based verification may depend on TS.43/EAP-AKA
  support; the added IMS Phenotype flag permits UPI verification without the
  unusable ACS fallback.
- No root, Magisk, or hardware-integrity spoofing is used.

## Checklist

- [x] Real service implementations (no stubs)
- [x] Intent filters registered for both services
- [x] Permission checks in service entry points
- [x] Unit tests added and passing
- [x] Debug build verified
- [x] Rootless
