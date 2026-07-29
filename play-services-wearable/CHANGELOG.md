# Changelog — Wear OS Support

## [Unreleased]

### Added
- Bluetooth companion device pairing via CompanionPairingManager
- Wear OS device discovery with BLE scanning support
- Automatic reconnection on Bluetooth state changes
- Notification bridging infrastructure in WearableServiceImpl
- Call state synchronization hooks (incoming, outgoing, missed, rejected)
- Media playback state forwarding with MediaSession monitoring
- Channel API `openChannel` implementation for bidirectional data streams
- Companion device persistence via SharedPreferences
- PairingListener interface for event-driven pairing workflows
- BroadcastReceiver for Bluetooth adapter state changes
- WEAR_OS_SERVICE_UUID, DEVICE_INFORMATION_SERVICE_UUID, BATTERY_SERVICE_UUID constants
- Wear OS device name detection (Galaxy Watch, Pixel Watch, TicWatch, Fossil, etc.)
- Handler-based main thread dispatching for callbacks
- Concurrent device support with per-device connection state tracking

### Enhanced
- WearableServiceImpl: openChannel method with input validation
- WearableServiceImpl: addListener support for IWearableCallbacks
- build.gradle: explicit repository configuration for wear OS dependencies
- Thread safety: ConcurrentHashMap for channel state management
- Error handling: comprehensive error codes for pairing/connection failures

### Testing
- CompanionPairingManagerTest: 13 unit tests (lifecycle, listeners, device model)
- WearableServiceImplChannelTest: 7 unit tests (openChannel, addListener)
- Robolectric test runner configuration
- CI workflow: assemble + test + lint matrix (API 26/30/34)
- run_tests.sh: multi-stage test runner script

### Documentation
- WEAROS_ARCHITECTURE.md: architecture overview, Bluetooth flow, feature matrix
- API_REFERENCE.md: complete public API reference with constants and error codes
- TESTING_GUIDE.md: 10 test scenarios, emulator setup, debugging commands
- README.md: features, architecture diagram, build instructions

### Dependencies
- Android API 18+ (Bluetooth)
- API 21+ (MediaSession, TelecomManager)
- Robolectric 4.12.2 (testing)
- JUnit 4.13.2 (testing)
- Mockito 4.11.0 (testing)
