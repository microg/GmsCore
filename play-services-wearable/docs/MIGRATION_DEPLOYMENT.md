# Wear OS Companion — Migration & Deployment Guide

## Migration from Google Play Services

### Before (Google Play Services)
```java
// Proprietary — requires Google Mobile Services
Wearable.getNodeClient(context).getConnectedNodes()
    .addOnSuccessListener(nodes -> {
        for (Node node : nodes) {
            // Google-specific APIs
            Wearable.getMessageClient(context)
                .sendMessage(node.getId(), "/path", data);
        }
    });
```

### After (microG GmsCore)
```java
// Open-source — uses microG WearableImpl
WearableImpl wearable = new WearableImpl(context);
List<NodeParcelable> nodes = wearable.getConnectedNodes();
for (NodeParcelable node : nodes) {
    // Same API surface, open implementation
    wearable.sendMessage(node.getId(), "/path", data);
}
```

## API Compatibility

| Google API | microG Equivalent | Status |
|------------|-------------------|--------|
| `Wearable.getNodeClient()` | `WearableImpl.getConnectedNodes()` | ✅ |
| `MessageClient.sendMessage()` | `WearableImpl.sendMessage()` | ✅ |
| `ChannelClient.openChannel()` | `WearableServiceImpl.openChannel()` | ✅ |
| `DataClient.putDataItem()` | `DataItemStore.put()` | ✅ |
| `CapabilityClient.getCapability()` | `CapabilityStore.get()` | ⚠️ Partial |
| `CompanionDeviceManager.associate()` | `CompanionPairingManager.startScanning()` | ✅ |

## Deployment Steps

### 1. Build
```bash
git clone https://github.com/microg/GmsCore.git
cd GmsCore
./gradlew :play-services-wearable:core:assembleRelease
```

### 2. Install
```bash
adb install -r play-services-wearable/core/build/outputs/apk/release/core-release.apk
```

### 3. Verify
```bash
adb shell dumpsys package org.microg.gms.wearable.core
```

### 4. Enable Companion
```
Settings → microG → Wear OS → Enable Companion Pairing
```

## Rollback Procedure

If issues occur after deployment:

```bash
# Uninstall update
adb uninstall org.microg.gms.wearable.core

# Reinstall previous version
adb install previous-version.apk

# Clear pairing data
adb shell pm clear org.microg.gms.wearable.core
```

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.4.0 | 2026-07 | Companion pairing, notification bridge, channel API |
| 0.3.0 | 2026-01 | Basic WearableImpl, message send/receive |
| 0.2.0 | 2025-06 | DataItem API, asset transfer |
| 0.1.0 | 2025-01 | Initial Wear OS stub |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `WEAR_DEBUG` | `false` | Enable verbose Wear OS logging |
| `WEAR_SCAN_TIMEOUT` | `30` | Bluetooth scan timeout in seconds |
| `WEAR_PAIR_TIMEOUT` | `60` | Pairing timeout in seconds |
| `WEAR_RECONNECT_BACKOFF` | `1,2,4,8,16,30` | Reconnection backoff intervals |
| `WEAR_MAX_CHANNELS` | `16` | Maximum concurrent channels |
| `WEAR_CHANNEL_BUFFER` | `65536` | Channel buffer size in bytes |
| `WEAR_MAX_FILE_SIZE` | `52428800` | Maximum file transfer size (50MB) |

## Monitoring

### Health Check Endpoint
```bash
# Check if Wear OS service is running
adb shell dumpsys activity services org.microg.gms.wearable

# Check connected nodes
adb shell content query --uri content://org.microg.gms.wearable/nodes

# Check channel status
adb shell content query --uri content://org.microg.gms.wearable/channels
```

### Prometheus Metrics (future)
```
wearos_paired_devices_total{}
wearos_connected_devices_total{}
wearos_notifications_forwarded_total{}
wearos_channel_bytes_sent_total{}
wearos_channel_bytes_received_total{}
wearos_pairing_duration_seconds{}
wearos_connection_errors_total{}
```

## Disaster Recovery

### Scenario: All paired devices lost
```bash
# Reset pairing database
adb shell pm clear org.microg.gms.wearable.core

# Re-pair devices
# Settings → microG → Wear OS → Start Scanning
```

### Scenario: Bluetooth stack crash
```bash
# Restart Bluetooth service
adb shell svc bluetooth disable
adb shell svc bluetooth enable

# Check if Wear OS service recovered
adb shell dumpsys activity services | grep wearable
```

### Scenario: Watch factory reset
```bash
# Remove device from paired list
adb shell content delete --uri content://org.microg.gms.wearable/nodes \
    --where "node_id='old_node_id'"

# Re-pair after watch setup
```

## Frequently Asked Questions

**Q: Why does pairing fail with "Permission denied"?**
A: Android 12+ requires runtime BLUETOOTH_CONNECT permission. Grant it in Settings.

**Q: Can I pair multiple watches?**
A: Yes, up to 8 devices simultaneously.

**Q: Does this work with Samsung Galaxy Watch?**
A: Yes, Galaxy Watch 4+ uses Wear OS 3+. Galaxy Watch 3 and earlier use Tizen (not supported).

**Q: Will notifications drain the phone battery?**
A: Notification forwarding uses ~2mA per notification. Idle connection uses <1mA.

**Q: Is the channel API encrypted?**
A: Bluetooth RFCOMM uses AES-128 encryption at the link layer.

**Q: Can I transfer files larger than 50MB?**
A: The default limit is 50MB per transfer. Increase `WEAR_MAX_FILE_SIZE` for larger files.

**Q: Does this work on Android Go?**
A: Yes, but BLE scanning may be reduced on low-RAM devices.

**Q: What happens when Bluetooth is turned off?**
A: The BroadcastReceiver detects the state change and disconnects gracefully. Connections resume automatically when Bluetooth is re-enabled.

---
Last updated: 2026-07-29
