# BountyHub claim notes — microG GmsCore #2994 (RCS / Google Messages)

**Branch:** `rcs-2994-ayush8620`  
**Draft PR (leave draft):** https://github.com/microg/GmsCore/pull/3828  
**Compare:** https://github.com/microg/GmsCore/compare/master...ayush8620:GmsCore:rcs-2994-ayush8620  
**Issue:** https://github.com/microg/GmsCore/issues/2994  
**Updated:** 2026-09-21 11:08 IST  

**Honest verdict:** This branch is at source parity with the strongest open “working-lineage” PR (#3808 / opstic #3360 constellation+asterism hardening), plus AppCert Spatula fallback, Phenotype Messages/IMS flags, DroidGuard dg_cache/account probe, and additional client-compat ports (`APP_CERT`, `gcm_ver`, Phenotype `getStorageInfo`). **It is NOT claimable on BountyHub without Ayush capturing on-device E2E proof** (Messages shows RCS Connected / chat features on a locked-bootloader, no-root, no-Magisk device). JVM tests and debug APK assemble are green; that is necessary but not sufficient.

---

## What is implemented

### Core RCS stack (adapted from opstic / #3784 / #3808 lineage — preserve credit)
- `play-services-constellation` — phone-number verification (PNV), Sync RPC, Spatula header via AppCert bind, MO/MT SMS, TS.43 / EAP-AKA entitlement helpers, consent gate, client-signature invariant, UI preferences.
- `play-services-asterism` — Get/Set Asterism consent, RCS consent fast-path + RCS-only consent-version invariant, Samsung CompositeToken codec.
- Core wiring: settings, manifest services, Phenotype Messages MLS + IMS UPI flags, SelfCheck / nav.

### AppCert / Spatula
- Android-ID Spatula **fallback completed** (no longer `return null // TODO`) when DeviceKey unavailable.
- Manifest exposes both:
  - `com.google.android.gms.auth.be.appcert.AppCertService`
  - `com.google.android.gms.auth.APP_CERT` (ported from @paulcakeface #3815)
- Constellation `SpatulaHeaderProvider` tries classic action, then `APP_CERT`.

### GCM / IID-adjacent (ported from @paulcakeface #3818 / #3819)
- Register requests send `gcm_ver` (header + form) = `BuildConfig.VERSION_CODE`.
- Unregister path uses explicit `delete(true)` helper.

### Phenotype (ported from @paulcakeface #3817 + prior Messages flags)
- Advertises `get_storage_info_api`; `getStorageInfo` returns status **29514** (Messages-supported fallback).
- Messages package keeps all 4 penpal/MLS flags (no duplicate-key wipe).
- IMS library exposes `RcsProvisioning__min_gmscore_version_for_upi_without_acs_fallback_met`.

### DroidGuard
- Stock-like `dg_cache` dir name, uppercase VM cache key, Google account presence probe for handle requests.

---

## Gap analysis vs strongest working-claim PRs

| Area | opstic #3360 / Chess-Debug #3808 | This branch | Notes |
|------|----------------------------------|-------------|-------|
| Constellation + Asterism source | Full | **Identical to #3808** for those trees | Confirmed via `git diff HEAD upstream/pr-3808 -- play-services-constellation play-services-asterism` → empty |
| AppCert Spatula fallback | Present in #3808 | Present (+ docs) | |
| APP_CERT intent action | Not in #3808 | **Added** (#3815) | |
| gcm_ver / unregister delete | Not in #3808 | **Added** (#3818/#3819) | |
| Phenotype getStorageInfo | Not in #3808 | **Added** (#3817) | |
| Messages Phenotype flags | 4 flags | 4 + IMS UPI | We keep extra IMS flag |
| DroidGuard dg_cache / account | Present | Present | Minor import/blank-line drift only |
| On-device RCS E2E proof | Testers reported on related builds | **None on this fork** | Blocker for claim |
| Fake AST / AI junk PRs | Avoid | Avoided | Do not open junk PR fluff |

**Still open risks vs a “known good” opstic-derived build:**
1. No device E2E on *this* APK / signature / profile combo.
2. Carrier-specific TS.43 / MT-SMS / SIM entitlement may fail even when services bind.
3. Play Integrity / DroidGuard remote policy can still reject microG VMs.
4. Competing PRs may land first; bounty likely requires a PR that *solves* with evidence, not source parity alone.
5. Draft PR #3828 title still says only Spatula — parent must update title/body when Ayush approves ready (do **not** mark ready here).

---

## How to build the APK

On a machine with Android SDK (this box: `sdk.dir=/workspace/android-sdk`):

```bash
cd /workspace/GmsCore
git checkout rcs-2994-ayush8620
./gradlew :play-services-core:assembleMapboxDefaultDebug -x lint -x test
```

APK (debug, Mapbox default flavor):

`play-services-core/build/outputs/apk/mapboxDefault/debug/com.google.android.gms-252432033.apk`

Release / signed / privileged install paths depend on your ROM (Lineage with signature spoofing, Graphene, etc.). Prefer the same install method you use for stock microG.

### JVM tests (green on this box, 2026-09-21 IST)

```bash
./gradlew \
  :play-services-constellation-core:testDebugUnitTest \
  :play-services-asterism-core:testDebugUnitTest \
  :play-services-droidguard:testDebugUnitTest \
  :play-services-core:testMapboxDefaultDebugUnitTest
```

---

## Device verification (Ayush must run)

**Target acceptance:** Google Messages RCS works with microG, **no Magisk/root**, locked bootloader OK.

### Prep
1. Install this build as `com.google.android.gms` (privileged / system / spoofed as needed for your ROM).
2. Ensure microG: Device registration (checkin), Cloud Messaging, and Google account sign-in work.
3. Grant SMS / Phone / Notifications to microG and Google Messages.
4. In microG Settings → enable Phone number verification (Constellation) if shown.
5. Clear Google Messages storage once after installing this GmsCore (optional but reduces stale ACS state).
6. Use a real SIM with RCS-capable carrier; Wi‑Fi-only may get further on some accounts but cellular helps PNV.

### Steps
1. Open Google Messages → set as default SMS app.
2. Messages → Profile → **Message settings** → **RCS chats** / Chat features → turn on.
3. Wait for status: **Connected** / “RCS status: Connected” (wording varies by Messages version).
4. Send an RCS message to another RCS user (or yourself on another device): typing indicators / delivery receipts / higher-res media as applicable.
5. Confirm no endless “Setting up…” / “Try again later”.

### Logcat filters (capture full session from toggle → Connected)

```bash
adb logcat -v time \
  SpatulaHeaderProvider:D AppCertManager:D AppCertService:D \
  Constellation:D Asterism:D \
  GmsConstellation:D GmsAsterism:D \
  PhenotypeService:D DroidGuard:D \
  BugleRcs:D Bugle:D Jibe:D IMS:D \
  *:S
```

Also useful broader catch:

```bash
adb logcat -v time | tee rcs-session.log | rg -i 'spatula|constellation|asterism|appcert|rcs|jibe|acs|pnv|ts43|eap|phenotype|droidguard'
```

### Evidence Ayush must capture for BountyHub
1. Screenshots: Messages RCS **Connected**; successful RCS chat thread.
2. Device facts: model, Android version, locked bootloader (Developer options / `adb shell getprop ro.boot.flash.locked` / OEM), **no Magisk** (`which magisk` empty / Settings), microG version / APK versionCode.
3. `adb` logcat from enable → Connected (redact tokens/phone numbers if posting publicly; keep full private copy for claim).
4. Link to draft PR #3828 + this branch commit SHA after push.
5. Short note: build command + APK hash (`sha256sum` of APK).

---

## Attribution (do not erase)

Primary RCS implementation lineage: **@opstic** (#3359 / #3360), consolidated/hardened via **#3784** (@naormeit et al.) and **#3808** (@Chess-Debug), with ports from **@Anusha0501**, **@keeltrace**, MT-SMS contributors, Spatula/AppCert discussion (**@juliushill42** and thread). Client-compat slices ported from **@paulcakeface** (#3815 APP_CERT, #3817 Phenotype storage info, #3818 gcm_ver, #3819 unregister delete). This fork does not claim sole authorship of constellation/asterism.

---

## Explicit non-actions (this agent run)

- Did **not** create a new PR.
- Did **not** `gh pr ready`.
- Did **not** use CloudAgent.
- Left draft PR #3828 as draft; only pushed commits to `origin/rcs-2994-ayush8620`.
