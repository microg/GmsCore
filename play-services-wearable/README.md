# Wear OS Support for microG

Companion device pairing and Wear OS 3+ integration for microG GmsCore.

## Features

- **Bluetooth Companion Pairing**: discovery, pairing, and reconnection for Wear OS devices
- **Notification Bridging**: forward phone notifications to connected wearables
- **Call State Sync**: incoming/outgoing/missed call status forwarding
- **Media Control Bridge**: playback state and metadata sync with media session
- **Channel API**: bidirectional byte streams for file transfer and sensor data

## Architecture

```
CompanionPairingManager  →  Bluetooth discovery & pairing
         ↕
WearableServiceImpl      →  openChannel, data routing
         ↕
NotificationBridge       →  phone → watch notifications
CallBridge               →  call state synchronization
MediaBridge              →  media playback control
ChannelManager           →  data stream lifecycle
```

## Building

```bash
./gradlew :play-services-wearable:core:assemble
```

## Testing

```bash
# Unit tests (Robolectric)
./gradlew :play-services-wearable:core:test

# Specific test class
./gradlew :play-services-wearable:core:test --tests "*.CompanionPairingManagerTest"
```

## Test Coverage

| Component | Tests | Status |
|-----------|-------|--------|
| CompanionPairingManager | 12 | ✅ |
| WearableServiceImpl Channel | 9 | ✅ |
| Notification bridging | Planned | ⏳ |
| Media bridge | Planned | ⏳ |

## Requirements

- Android API 18+ (Bluetooth)
- API 21+ (Media session, Call sync)
- microG GmsCore 0.3+
