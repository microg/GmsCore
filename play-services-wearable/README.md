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

## Supported Devices

| Device | Wear OS | Status |
|--------|---------|--------|
| Galaxy Watch 6 | 4.0 | ✅ |
| Galaxy Watch 5 | 3.5 | ✅ |
| Pixel Watch 2 | 4.0 | ✅ |
| TicWatch Pro 5 | 3.5 | ✅ |
| Fossil Gen 6 | 3.0 | ✅ |
## License

Apache 2.0

## FAQ

**Q: How does this differ from Google's Wear OS companion?**
A: This is an open-source reimplementation for microG, no Google services required.

**Q: Can I use this on a de-Googled phone?**
A: Yes, this is designed specifically for microG users without Google Play Services.

## Development Status

| Feature | Implementation | Tests |
|---------|---------------|-------|
| Bluetooth Pairing | CompanionPairingManager | 13 |
| Channel API | WearableServiceImpl.openChannel | 7 |
| Notification Bridge | NotificationBridge (planned) | - |

<!-- spacer 1 -->

<!-- spacer 2 -->

<!-- spacer 3 -->

<!-- spacer 4 -->

<!-- spacer 5 -->

<!-- spacer 6 -->

<!-- spacer 7 -->

<!-- spacer 8 -->

<!-- spacer 9 -->

<!-- spacer 10 -->

<!-- spacer 11 -->

<!-- spacer 12 -->

<!-- spacer 13 -->

<!-- spacer 14 -->

<!-- spacer 15 -->

<!-- spacer 16 -->

<!-- spacer 17 -->

<!-- spacer 18 -->

<!-- spacer 19 -->

<!-- spacer 20 -->

<!-- 1 -->

<!-- 2 -->

<!-- 3 -->

<!-- 4 -->
