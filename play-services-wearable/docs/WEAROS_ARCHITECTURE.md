# Wear OS Support Architecture

## Overview

This PR implements Wear OS 3+ device pairing and companion management for microG.

## Components

### CompanionPairingManager
Manages the pairing lifecycle between a phone and Wear OS device:
- Bluetooth device discovery and pairing
- Companion device profile management
- Connection state tracking
- Reconnection logic

### WearableServiceImpl (enhanced)
Extended with:
- `openChannel` API support for bidirectional data streams
- Bluetooth connection thread management
- Notification bridging hooks
- Call state synchronization
- Media playback state forwarding

## Supported Features

| Feature | Status | API Level |
|---------|--------|-----------|
| Bluetooth pairing | ✅ | 18+ |
| Notification bridging | ✅ | 20+ |
| Call state sync | ✅ | 21+ |
| Media controls | ✅ | 21+ |
| Channel API (data streams) | ✅ | 18+ |
| File transfer | ✅ | 18+ |

## Bluetooth Connection Flow

```
Phone                    Wear OS Device
  |                            |
  |--- Bluetooth discovery --->|
  |<--- Device found ----------|
  |                            |
  |--- Pair request ---------->|
  |<--- Pair confirm ----------|
  |                            |
  |--- RFCOMM connect -------->|
  |<--- Channel established ---|
  |                            |
  |--- Capability exchange --->|
  |<--- Capability response ----|
  |                            |
  |--- Data channel ready -----|
```

## Notification Bridging

Notifications from the phone are forwarded to the watch via:
1. `NotificationListenerService` captures notification
2. `NotificationBridge` serializes to wearable format
3. `WearableImpl.sendMessage()` delivers to connected node

## Channel API

Bidirectional byte streams for:
- File transfers (APK, assets, fonts)
- Sensor data streaming
- Configuration sync
- Firmware updates

## Testing

```bash
# Unit tests
./gradlew :play-services-wearable:core:test

# Instrumented tests (requires device)
./gradlew :play-services-wearable:core:connectedAndroidTest
```

## References

- [Wear OS Companion Device Pairing](https://developer.android.com/training/wearables/companion-device-pairing)
- [Bluetooth RFCOMM](https://developer.android.com/reference/android/bluetooth/BluetoothSocket)
- [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
