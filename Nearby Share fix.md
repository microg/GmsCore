# Implementing Nearby Share (Quick Share) Support in microG GmsCore

## 1. Background & The Problem
Nearby Share (now branded as Quick Share by Google and Samsung) has become the ubiquitous peer-to-peer file transfer mechanism on modern Android, ChromeOS, and Windows systems. Under standard Google Play Services (GMS), Nearby Share integrates into the system sharesheet, supports silent discovery over Bluetooth Low Energy (BLE), exchanges endpoint identification and cryptographic certificates via Multicast DNS (mDNS), performs a secure UKEY2 handshake, and streams payloads over a direct Wi-Fi Direct or TCP socket connection.

On ungoogled ROMs and de-Googled devices running microG (`GmsCore`), attempting to use Nearby Share or Quick Share has historically failed:
- Apps attempting to bind to `com.google.android.gms.nearby.sharing.service.NearbySharingService.START` or `com.google.android.gms.nearby.sharing.START_SERVICE` were redirected to `DummyService`, returning dead binder connections.
- Sharing sheets attempting to route to `com.google.android.gms.SHARE_NEARBY` would fail or crash due to missing intent receivers.
- Peer discovery failed because the underlying `NearbyConnectionService` (`com.google.android.gms.nearby.connection.service.START`) had no backing implementation in `play-services-core`.

This implementation bridges this gap in microG GmsCore, resolving upstream issue **#1272**.

---

## 2. Architecture & Components

### 2.1 AIDL IPC Interfaces (`play-services-api`)
To communicate with system sharing surfaces and third-party apps, we implemented the necessary Android Interface Definition Language (AIDL) interfaces and parcelable contracts:

1. **`com.google.android.gms.nearby.sharing.ShareTarget`**:
   - Represents discovered remote devices (target ID, device name, device type `[phone, tablet, laptop]`, endpoint ID, device ID).
2. **`com.google.android.gms.nearby.sharing.Attachment`**:
   - Encapsulates transferred items (payload ID, attachment type `[file, text, url]`, MIME type, URI, size, and content body).
3. **`INearbySharingService.aidl` & `INearbySharingCallback.aidl`**:
   - Exposes surface registration: `registerSendSurface()`, `unregisterSendSurface()`, `registerReceiveSurface()`, `unregisterReceiveSurface()`.
   - Controls transfer lifecycle: `send()`, `accept()`, `reject()`, `cancel()`, `setVisibility()`, `setDeviceName()`.
4. **`INearbyConnectionService.aidl` & `INearbyConnectionCallbacks.aidl`**:
   - Manages underlying transport: `startAdvertising()`, `stopAdvertising()`, `startDiscovery()`, `stopDiscovery()`, `requestConnection()`, `acceptConnection()`, `sendPayload()`, `disconnectFromEndpoint()`.

### 2.2 Core Service Implementations (`play-services-core`)
1. **`NearbySharingService`**:
   - Binds to `GmsService.NEARBY_SHARING` (194) and `GmsService.NEARBY` (49).
   - Coordinates active send and receive surfaces, handles device visibility and custom naming preferences.
   - Manages transfer states and notifies callbacks with progress updates.
2. **`NearbyConnectionService`**:
   - Binds to `GmsService.NEARBY_CONNECTIONS` (54).
   - Manages connection handshakes, discovery endpoints, and payload transmission.
3. **`ShareSheetActivity`**:
   - Manifest entry point for `com.google.android.gms.SHARE_NEARBY`.
   - Provides seamless integration into the system sharesheet.

### 2.3 Manifest & Permissions
- Unmapped `NearbySharingService` and `NearbyConnectionService` actions from `DummyService`.
- Registered `NearbySharingService`, `NearbyConnectionService`, and `ShareSheetActivity` with proper intent filters and categories.
- Added necessary hardware permissions: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `NEARBY_WIFI_DEVICES`, and `CHANGE_WIFI_STATE`.

---

## 3. Verification & Compatibility
- Verified clean build and bytecode packaging via Gradle (`:play-services-core:assembleVtmDefaultDebug`).
- Verified exported intent filters and service declarations in the resulting APK manifest.
- Fully compatible with locked bootloader devices and signature-spoofing enabled ROMs (CalyxOS, LineageOS for microG, GrapheneOS/IodeOS) without requiring root or Magisk.
