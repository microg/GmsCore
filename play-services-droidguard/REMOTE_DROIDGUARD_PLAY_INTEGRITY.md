# Remote DroidGuard for Play Integrity (`pia_attest_e1` & `pia_express`)

This guide explains how to use a secondary Android phone (for example, a spare stock or `DEVICE`-integrity-compliant phone kept on home Wi-Fi / Tailscale) as a **Remote DroidGuard Server** for a daily-driver microG phone (#2851).

## Why Standard Single-Step Remote DroidGuard Previously Failed for Play Integrity

1. **Binary Bundle Parameters (`pia_express`)**: `IntegrityExtensions.regenerateToken` passes `piaExpressDeviceKey` as a `ByteArray` inside `DroidGuardResultsRequest.bundle`. Previously, `RemoteHandleImpl` only read `request.bundle.getString(key)`, which silently returned `null` for `ByteArray` entries and broke Express Play Integrity (`pia_express`) used by Firebase AppCheck apps such as Dott (`com.ridedott.rider`).
2. **Cross-Process `DroidGuardInitReply` Handshake (`pia_attest_e1`)**: Standard Play Integrity (`pia_attest_e1`) uses a two-step cross-process VM handshake (`DroidGuardHandleImpl.initWithRequest` -> `rb()` `Bundle` + `ParcelFileDescriptor` of `the.apk` -> `DroidGuardApiClient.handleDroidGuardData` -> `snapshot`). A standalone Python script outside Android cannot execute the cross-process `IBinder` handshake.
3. **One-Shot `guardWithRequest` Service Path**: `DroidGuardServiceImpl.guardWithRequest` previously threw `NotImplementedError`.

## Architecture of the Solution

- **Client Phone (`RemoteHandleImpl` + `DroidGuardServiceImpl`)**:
  - Serializes `String` (`x-request-<key>`), `ByteArray` (`x-request-b64-<key>`), and primitive (`x-request-meta-<key>`) bundle entries on `DroidGuardResultsRequest`.
  - Implements `DroidGuardServiceImpl.guardWithRequest` with full handle lifecycle cleanup and `FallbackCreator` error handling.
- **Donor Server Phone (`RemoteDroidGuardServer`)**:
  - Runs inside the Android process on the donor phone (default port `8789`).
  - Reconstructs `DroidGuardResultsRequest` (including `piaExpressDeviceKey` `ByteArray` and `thirdPartyCallerAppPackageName`) and invokes `DroidGuardClientImpl(context, sourcePackage).getResults(flow, snapshotData, resultsRequest)` on the donor phone so the full cross-process `DroidGuardInitReply` (`the.apk` + `rb()` `Bundle`) and `snapshot` execute natively on the donor device.

## Step-by-Step Setup Guide

### 1. Prepare the Donor Phone (Home Server Device)
1. Use any spare Android phone that passes `MEETS_DEVICE_INTEGRITY` (either stock Android or LineageOS/microG configured with a valid keybox).
2. Connect the donor phone to power and Wi-Fi (or Tailscale / WireGuard for access outside home).
3. Start `RemoteDroidGuardServer` on port `8789` and verify health:
   ```bash
   curl http://<DONOR_PHONE_IP>:8789/health
   # Expected output: OK
   ```

### 2. Configure the Client Phone (Daily Driver with microG)
1. Open **microG Settings** -> **SafetyNet / Play Integrity / DroidGuard**.
2. Switch DroidGuard mode to **Network (Remote Server)**.
3. Set the Network Server URL to:
   ```text
   http://<DONOR_PHONE_IP>:8789
   ```
4. Ensure **Play Integrity / Device Attestation** is enabled in microG Settings.
5. Launch your Play Integrity / Firebase AppCheck app (e.g., Dott `com.ridedott.rider`) — both `pia_attest_e1` and `pia_express` requests are transparently forwarded to the donor phone and return valid tokens.
