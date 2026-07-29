# Wear OS Integration Testing Guide

## Prerequisites

- Android Studio Hedgehog or later
- Physical Wear OS device (API 30+) or emulator
- Phone with microG installed
- Bluetooth enabled on both devices

## Emulator Setup

```bash
# Create Wear OS emulator
sdkmanager "system-images;android-34;google_apis;wearos-armeabi-v7a"
avdmanager create avd -n wearos_test -k "system-images;android-34;google_apis;wearos-armeabi-v7a" -d "wearos_small_round"

# Launch with Bluetooth support
emulator -avd wearos_test -bluetooth
```

## Test Scenarios

### 1. Device Discovery
```
Precondition: Bluetooth enabled, Wear OS device in pairing mode
Steps:
  1. Launch microG Settings → Wear OS
  2. Tap "Start Scanning"
  3. Verify device appears in list within 10 seconds
  4. Verify device name matches expected
Expected: Device listed with name, MAC address, signal strength
```

### 2. Pairing Flow
```
Precondition: Device discovered
Steps:
  1. Tap discovered device
  2. Confirm pairing on both phone and watch
  3. Wait for bonding completion
  4. Check paired devices list
Expected: Device shows as "Paired" with nodeId assigned
```

### 3. Notification Forwarding
```
Precondition: Device paired and connected
Steps:
  1. Send test notification via: adb shell cmd notification post -p com.test "Title" "Body"
  2. Check watch notification shade
  3. Dismiss notification on phone
  4. Verify watch notification dismissed
Expected: Notification appears on watch, syncs dismissal
```

### 4. Call State Sync
```
Precondition: Device connected
Steps:
  1. Simulate incoming call: adb shell am broadcast -a android.intent.action.NEW_OUTGOING_CALL
  2. Verify call state forwarded
  3. End call
  4. Verify idle state synced
Expected: Watch shows call status changes
```

### 5. Media Control
```
Precondition: Media playing on phone
Steps:
  1. Start music playback
  2. Verify metadata appears on watch
  3. Use watch media controls (play/pause/next)
  4. Verify phone responds
Expected: Bidirectional media control sync
```

### 6. Channel API File Transfer
```
Precondition: Channel opened
Steps:
  1. Push file to watch via channel
  2. Verify file received on watch
  3. Pull file from watch via channel
  4. Verify file integrity
Expected: File transfers complete without corruption
```

### 7. Disconnection and Reconnection
```
Precondition: Device connected
Steps:
  1. Disable Bluetooth on phone
  2. Verify disconnect event fired
  3. Re-enable Bluetooth
  4. Verify automatic reconnection within 30s
Expected: Graceful disconnect, automatic reconnect
```

### 8. Multiple Device Support
```
Precondition: Two Wear OS devices paired
Steps:
  1. Connect both devices
  2. Send notification → verify both receive
  3. Disconnect one → verify only other receives
  4. Reconnect → verify both receive again
Expected: Independent per-device state management
```

### 9. Battery Optimization
```
Precondition: Device connected
Steps:
  1. Enable Doze mode: adb shell dumpsys deviceidle force-idle
  2. Verify connection maintained
  3. Send notification → verify delivered
  4. Exit Doze mode
Expected: Connection survives Doze, notifications delivered
```

### 10. Permission Handling
```
Precondition: Bluetooth permission revoked
Steps:
  1. Revoke BLUETOOTH_CONNECT permission
  2. Attempt to start scanning
  3. Verify graceful error message
  4. Grant permission
  5. Verify scanning resumes
Expected: Clear error states, recovers on permission grant
```

## Debugging

### Enable Wear OS Logs
```bash
adb logcat -s GmsWearCompanion:* GmsWearCallBridge:* GmsWearMediaBridge:* GmsWearChannelMgr:*
```

### Common Log Tags
| Tag | Component |
|-----|-----------|
| `GmsWearCompanion` | Pairing manager |
| `GmsWearCallBridge` | Call state sync |
| `GmsWearMediaBridge` | Media controls |
| `GmsWearChannelMgr` | Channel API |

### Dump Pairing State
```bash
adb shell dumpsys companiondevice
```

### Check Bluetooth State
```bash
adb shell dumpsys bluetooth_manager | grep -A5 "Bonded devices"
```

---
Last updated: 2026-07-29
