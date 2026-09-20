# WearOS Support Plan

This document captures the current plan and requirements for bringing modern WearOS device support to microG. It focuses on the minimum feature set that users expect when pairing a WearOS watch with a phone running microG instead of Google Play Services.

## Goals (MVP)
- **Pairing** with current-generation WearOS devices via the companion layer.
- **Notification mirroring** from the phone to the watch (including action buttons, channels, and grouping).
- **Media controls** (play/pause/seek/next/previous, now-playing metadata) bridged between phone and watch.
- **Basic app runtime support** for WearOS apps that depend on Play Services stubs available in microG.
- **Reliability first:** no crashes, graceful fallbacks when capabilities are missing.

## Architecture overview
1. **Companion transport**: Uses BLE/Wi‑Fi for discovery and pairing. On phones, the WearOS companion app binds to Play Services; in microG we need a shim that exposes the same IPC surface (notably the `com.google.android.wearable.app.cn` and `com.google.android.gms.wearable` contracts).
2. **GMS modules involved**: Wearable API, Nearby/Discovery (for pairing), Account/Auth, FCM for data sync, Notification Listener bridging, and MediaSession forwarding.
3. **Service binding**: Implement stub services for the wearable package names so the companion can bind and complete its self-checks.

## Deliverables (incremental)
- **Pairing**
  - Implement Nearby/BLE advertiser & scanner compatibility shims used by the companion during onboarding.
  - Provide `CapabilityClient` and `NodeClient` surfaces sufficient for device discovery and channel creation.
- **Notifications**
  - Bridge Android `NotificationListenerService` events into the wearable data layer channels.
  - Preserve actions, reply inputs, channels, and icons where possible.
- **Media**
  - Mirror active `MediaSession` state (metadata + transport controls) to the watch and accept control intents back to the phone.
  - Ensure background service keeps running while a watch is connected.
- **App support**
  - Stub Wearable APIs commonly used by WearOS apps (DataClient/MessageClient/ChannelClient) with graceful no‑op fallbacks when features are missing.
  - Package visibility/permissions alignment for Play Services–named permissions used by WearOS apps.

## Testing matrix
- **Devices**: At least one modern WearOS 3/4 watch (Bluetooth + Wi‑Fi) and an emulator for regression.
- **Phone OS**: Android 11–15, with and without battery optimizations for the companion.
- **Scenarios**: initial pairing, reconnection after flight mode, notification flood, media playback changes, app installs/updates.
- **Regression**: Verify standard microG self-check continues to pass and that non-Wear flows are unaffected.

## Next steps
1. Create companion binding stubs for `com.google.android.gms.wearable` and pass the companion app’s readiness checks.
2. Port notification bridge used by microG’s `NotificationListenerService` into the wearable data layer channel.
3. Mirror `MediaSession` updates into WearOS using Data/Message APIs and wire transport controls back.
4. Add telemetry/logging (opt-in) to diagnose pairing failures in the field.
5. Document setup instructions for users (grant notification access, exclude from battery optimizations, enable BLE/Wi‑Fi pairing).

Contributions are welcome; please keep changes modular and focused so we can iterate quickly toward full WearOS parity within microG.
