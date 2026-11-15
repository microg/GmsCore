# RCS phone-verification compatibility candidate

This candidate implements Google Messages verification dependencies in microG. Carrier activation and bidirectional RCS with a physical SIM remain unverified. It is an experimental debug build and is not a confirmed end-to-end RCS fix.

## Implementation and provenance

The base is upstream `f686900c68aa84d1605b52304f6615f3593c6f50`. Constellation/Asterism modules and protocol definitions are reused from [PR #3976](https://github.com/microg/GmsCore/pull/3976) at `d66138ba5714a5b1d87646c83cff6a11f6261d1e`. Original implementations are by opstic in #3359/#3360, with subsequent contributions in #3851, #3924 and #3976. Original source headers and attribution are retained. Unrelated DroidGuard changes and policy/MLS feature-flag overrides from that branch are excluded.

Source-confirmed setup gaps were:

- Constellation and Asterism resolved to `DummyService`, which returns `API_DISABLED`.
- `InstanceID.requestToken()` threw `UnsupportedOperationException`, preventing uncached IID registration through the integrated flow.
- The SMS Retriever permission activity requested `RECEIVE_SMS`/`READ_CONTACTS`; expanding its allowlist alone did not implement the verification services.

The integration adds real IID registration, backend consent/verification RPCs, per-subscription challenge processing, cancellation/callback handling and opt-in settings. Phone permission is requested when enabling verification. SMS permissions are requested through a separate opt-in, available while verification is enabled.

Additional safeguards reject empty credentials, inaccessible/request-mismatched SIMs, default-SIM fallback for an unmatched challenge, and reuse of cached credentials for a fresh certificate/nonce-bound request. Verification requests do not automatically opt users into server-side RCS consent. Only backend-provided verification records can produce verified results; an RPC success or public-key acknowledgement is not phone-number verification.

The existing `READ_SMS`/`SEND_SMS` declaration and permission-allowlist candidate is retained. ICC authentication declares supported ICC permissions and does not use `READ_PRIVILEGED_PHONE_STATE` as a replacement. Call-log permission is not added. Phenotype storage-info compatibility returns explicit unavailable status `29514`, without fabricating a successful protobuf or advertising commit-v2.

The declared GMS compatibility version is 25.45.00 for the TS.43 client-version gate reported in #3976. This global version change affects other GMS clients and needs broader device validation. It does not establish backend acceptance or bypass attestation.

## Installation prerequisites

Use an existing microG-compatible ROM with its bootloader already locked, preferably Android 12 or later for the supported ICC AppOp. This APK does not add ROM signature spoofing or replace privileged OS services. Google Play services must not coexist in the same profile. Use the Owner profile and a real active SIM with working ordinary SMS/data.

The requested artifact is a debug APK. Updating existing microG requires the same signing certificate as that installation, or an OS-supported compatible test installation. A normal debug certificate cannot update official production-signed microG. If installation reports UPDATE_INCOMPATIBLE, do not uninstall system packages, unlock the bootloader, or use root; use a compatible test setup or have the ROM/microG distributor sign this source with its update key. A stock phone with preinstalled Google Play services cannot replace that package with this differently signed APK. A locked bootloader alone does not provide microG compatibility.

1. Record the environment before testing:

```sh
adb shell getprop ro.product.model
adb shell getprop ro.build.fingerprint
adb shell getprop ro.build.version.sdk
adb shell getprop ro.boot.flash.locked
adb shell getprop ro.boot.vbmeta.device_state
adb shell getprop ro.boot.verifiedbootstate
adb shell dumpsys package com.google.android.gms > microg-package.txt
adb shell dumpsys package com.google.android.apps.messaging > messages-package.txt
adb shell cmd role get-role-holders android.app.role.SMS
```

Record the carrier/MCC-MNC separately, without sharing IMSI/ICCID/phone numbers. Keep the bootloader locked throughout. On supported phones flash.locked=1/device_state=locked supplies evidence; custom trust roots can affect verifiedbootstate, so do not equate a color alone with lock status.

2. Copy the APK to your ADB host, then install it using your compatible update/test signing setup:

```sh
adb install -r ./com.google.android.gms-254500037.apk
```

Do not use `-g`: permission requests are intentionally tested through the UI. Obtain current stable Google Messages from its official distribution (for example Aurora Store fetching the Play Store release on a microG phone), not a historical workaround version or a modified APK. Record versionName/versionCode from dumpsys above after installing/updating it. Set it as default SMS app. A current binary is not bundled with this source and has not been exercised here.

3. In microG settings, enable Google device registration and Cloud Messaging. Permit microG itself to register if confirm-new-apps is enabled (the Constellation default sender is 496232013492). Enable Phone Number Verification and grant the requested Phone permission. Enable Allow SMS verification and grant SMS access if testing SMS provisioning; carrier charges may apply. Denial should leave the SMS switch off. Read-only/IID/consent calls must not pop up SMS permission dialogs.

4. For a carrier/phone requiring ICC authentication or protected SIM identifiers, use the OS's explicit special permission control if it supports microG. AOSP's supported AppOp can alternatively be granted by an authorized ADB shell on Android versions exposing it:

```sh
adb shell cmd appops set com.google.android.gms USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER allow
adb shell cmd appops get com.google.android.gms USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER
```

This is an explicit special-access grant with access to device/SIM identifiers, not a runtime Phone grant. If the OS rejects or does not expose it and the app lacks carrier privileges/another supported ICC permission, that route is blocked; do not substitute READ_PRIVILEGED_PHONE_STATE or claim an APK can override OS enforcement. Revoke this test grant afterwards with `adb shell cmd appops set com.google.android.gms USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER default`. Some platforms instead offer USE_ICC_AUTH without device identifiers; identifier-dependent flows still need supported identifier access.

5. Check dispatch after installation:

```sh
adb shell cmd package query-services --brief -a com.google.android.gms.constellation.service.START -p com.google.android.gms
adb shell cmd package query-services --brief -a com.google.android.gms.asterism.service.START -p com.google.android.gms
```

Expect ConstellationApiService and AsterismApiService, not DummyService. The separate com.google.android.gms.rcs.START remains explicitly unimplemented; do not mistake that for a real RCS transport service. Messages supplies the RCS messaging transport; this candidate implements its verification dependencies. If current Messages actually requires the separate action, capture that as a remaining blocker.

6. Capture both a full log and a focused log before opening Messages > Settings > RCS chats and enabling RCS/accepting consent. Run in separate terminals, stopping each with Ctrl-C after the test:

```sh
adb logcat -v threadtime -T 1 > rcs-full.log
```

```sh
adb logcat -v threadtime -T 1 C11NApiService:V C11NRequest:V AsterismApiService:V GetAsterismConsent:V SetAsterismConsent:V GetIidToken:V GetPnvCapabilities:V VerifyPhoneNumber:V SyncRequestBuilder:V TelephonyInfoBuilder:V ChallengeProcessor:V MoSmsVerifier:V MtSmsVerifier:V CarrierIdVerifier:V Ts43Verifier:V EapAkaService:V GmsGcmRegister:V PhenotypeService:V GmsDummySvc:V AndroidRuntime:E '*:S' > rcs-microg.log
```

Useful checkpoints: Asterism consent calls; real IID registration; verifyPhoneNumber with API version/policy; Sending Sync request; server response count; server key acknowledgement; at most one key-ack retry; selected challenge and per-SIM final status. UNKNOWN/PENDING, an empty verification array, public-key acknowledgement, and RPC SUCCESS alone are not activation. Errors about SIM identifiers/ICC grants, RETRY_LATER, backend UNAUTHENTICATED/PERMISSION_DENIED, unavailable features, unsupported transactions, or Messages's Unable to retrieve MSISDN Token identify the next blocker. Respect carrier/backend retry delays; do not clear application data or repeatedly force provisioning jobs. Logs may include personal information from other components; redact before sharing.

7. With Messages reporting Connected, use a second known-working RCS endpoint. Send and receive a distinct text and an image in both directions, with SMS/MMS fallback disabled during the test (record both clients' RCS indicators). Confirm delivery/read behavior where enabled, then reboot the phone, confirm the bootloader is still locked, and repeat send/receive. Record exact Messages version, SIM/carrier, Android/ROM, APK SHA-256 and signing fingerprint, connection screenshots, timestamps and logs. Test another SIM/carrier separately; success on one path does not prove TS.43, dual-SIM, or all-carrier support.

## Validation completed

The [validation report](validation/emulator-api35.json) records artifact checksums, test counts and limits.

- `./gradlew :play-services-core:assembleVtmDefaultDebug`: successful with Gradle 8.13, JDK 17 and Android SDK/build tools 35. The Auth AIDL failure reported on #3976 did not reproduce.
- Unit tests: **153 passed**, zero failures/errors/skips: Constellation 134, Asterism 17, Phenotype 2. The SMS Retriever unit task had no test sources.
- Android 15/API 35 AOSP x86_64 emulator: the exact universal APK installed under a normal ADB shell (`uid=2000`) as a non-system app (`appId=10148`). Both service actions resolved uniquely to their implementations.
- Android instrumentation: **15 passed**: Constellation 13, Asterism 2. These cover module service binding, Binder/Parcel transport, credential failures and stored-key behavior. Injected credential/backend fixtures are not carrier provisioning tests.
- Full-APK permission UI checks passed: Phone denial keeps verification off and SMS disabled; granting Phone enables verification with SMS still off; SMS is requested only through its separate switch; SMS denial leaves all three SMS permissions denied. Verification was disabled again after testing.
- APK v1/v2 signatures, package metadata, packaged permissions and final SIM/credential safeguards were checked. No call-log or privileged-phone-state permission is declared.

Software-only emulator cold starts produced System UI and DEX-verification startup ANRs. Android's normal `cmd package compile -m verify -f com.google.android.gms` operation was run before the successful settings/permission checks. This does not establish cold-start performance on physical phones.

No current Google Messages binary was exercised, no real SIM/carrier authentication ran, and no physical locked-bootloader or bidirectional messaging result is claimed. The separate `com.google.android.gms.rcs.START` action remains unimplemented; a current Messages requirement for that action would be an additional blocker. Legacy Carrier-ID challenges prefixed with `[ts43]` remain unsupported. Flash-call verification has no newly requested call-log permission.

## Build and test commands

Configure the local Android SDK/JDK normally; SDK and signing paths belong in ignored local configuration.

```sh
./gradlew :play-services-core:assembleVtmDefaultDebug
./gradlew :play-services-constellation-core:testDebugUnitTest :play-services-asterism-core:testDebugUnitTest :play-services-auth-api-phone-core:testDebugUnitTest :play-services-core:testVtmDefaultDebugUnitTest
./gradlew :play-services-constellation-core:assembleDebugAndroidTest :play-services-asterism-core:assembleDebugAndroidTest
```

On an Android test device/emulator, install the two instrumentation APKs with `adb install -r -t`, then run:

```sh
adb shell am instrument -w org.microg.gms.constellation.core.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w org.microg.gms.asterism.core.test/androidx.test.runner.AndroidJUnitRunner
```

## Candidate artifacts

The tested universal APK is `com.google.android.gms-254500037.apk`, version `0.3.18.254500-dirty`, code `254500037`, 108406033 bytes. It was built from the working tree before the source commit; the `-dirty` version label is retained so the distributed file is the exact emulator-tested binary.

SHA-256: `bef5c7a730a5488ef61cbe4f385cd88d07f7329f73c46efac5c1c85eefbb36c5`.

The optional `com.google.android.gms-254500037-arm64.apk` is an architecture-filtered package derived from the universal APK. Its DEX, resources and ARM64 native libraries were checked byte-for-byte against the original; other native ABIs were removed and it was aligned/re-signed with the same local debug key. It has not run on physical ARM64 hardware. SHA-256: `aadf38b75287f643e5336c6ce592d1e5e3f4620146879c2a21f43bbf9b878e3b`.

Debug signing certificate SHA-256: `c807db3e94d62a59d07e372d69c735dd820bb81b3a5ea47f8063d2994c738614`. This is not the official microG release certificate; existing installations require a compatible signing key/update setup.

## References

- [RCS bounty #2994](https://github.com/microg/GmsCore/issues/2994)
- [Constellation/Asterism integration #3976](https://github.com/microg/GmsCore/pull/3976)
- [SMS permission candidate #4009](https://github.com/microg/GmsCore/pull/4009)
- [Android ICC authentication requirements](https://developer.android.com/reference/android/telephony/TelephonyManager#getIccAuthentication(int,int,java.lang.String))
- [GrapheneOS RCS guidance](https://grapheneos.org/usage#rcs), describing Google Play rather than proving microG compatibility
- [AOSP ICC permission declarations](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/res/AndroidManifest.xml)
- [AOSP ICC AppOp/identifier enforcement](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/telephony/common/com/android/internal/telephony/TelephonyPermissions.java)
