# Wear OS Companion Pairing & Data Bridging

This module provides comprehensive Wear OS device pairing, data bridging, and
remote control capabilities for microG's `play-services-wearable` implementation.

## Features

### 1. Companion Device Pairing (`CompanionPairingManager`)
- BLE-based device discovery with Wear OS service UUID filtering
- Persistent paired device storage via SharedPreferences
- Pairing lifecycle callback interface

### 2. Bluetooth RFCOMM Transport (`BluetoothConnectionThread`)
- Server mode: listens for incoming watch connections over RFCOMM
- Client mode: connects to a remote Bluetooth device
- Automatic reconnection with exponential backoff (up to 5 attempts)
- Connection state tracking (DISCONNECTED → CONNECTING → CONNECTED → DISCONNECTING)
- Socket proxy layer for transparent integration with existing `SocketWearableConnection`

### 3. Notification Bridging (`NotificationBridge`)
- Maintains thread-safe map of active notifications (ANCS-compatible)
- Dispatches positive actions (notification action → content intent fallback)
- Dispatches negative actions (dismiss/cancel)
- Supports custom action dispatch by index
- Notification categorization: call, message, media, other
- Category-based filtering for wearable delivery

### 4. Call Bridge (`CallBridge`)
- Monitors phone call state via `PhoneStateListener`
- Forwards call state transitions to connected watches
- Handles watch-to-phone commands: accept, reject, mute, silence ringer
- Serializes call state updates in a compact binary format

### 5. Media Bridge (`MediaBridge`)
- Monitors active `MediaSession` instances
- Captures metadata: title, artist, album, position, duration
- Sends media state updates to connected watches
- Handles 11 media control commands: play/pause, next/previous,
  volume up/down, seek forward/backward, shuffle, repeat
- Auto-attaches to the most recently active media session

### 6. Channel API (`ChannelManager`)
- Bidirectional byte-stream channels for continuous data transfer
- File transfer with progress callbacks and automatic chunking (8 KB)
- Piped I/O streams for consumer/producer pattern
- Channel lifecycle management: open → transfer → close
- 100 MB maximum file size safety limit

## Architecture

```
WearableServiceImpl
├── CompanionPairingManager   (Wear OS 3+ BLE pairing)
├── BluetoothConnectionThread (RFCOMM transport)
├── NotificationBridge        (ANCS notification actions)
├── CallBridge                (phone call state & control)
├── MediaBridge               (media playback state & control)
├── ChannelManager            (Channel API data streams)
└── CapabilityManager         (node capabilities)
```

All components integrate through `WearableServiceImpl` which forwards
wearable peer messages to the appropriate handler.

## Tests

Unit tests cover constants, enums, serialization, and null-safety for all
bridge components. Run with:

```bash
./gradlew :play-services-wearable:core:test
```

## Requirements

- Android SDK 21+ (Lollipop) for MediaBridge
- Android SDK 19+ (KitKat) for notification actions
- Bluetooth permissions for RFCOMM transport
- `READ_PHONE_STATE` for CallBridge
- Notification Listener for media session access

## License

Apache 2.0 — see [LICENSE](../../../LICENSE)
