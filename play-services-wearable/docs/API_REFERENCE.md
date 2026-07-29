# Wear OS API Reference

## CompanionPairingManager

### Constructor
```java
public CompanionPairingManager(Context context, WearableImpl wearable, String packageName)
```

### Public Methods

| Method | Return | Description |
|--------|--------|-------------|
| `startScanning()` | void | Begin Bluetooth/BLE device discovery |
| `getPairedDevices()` | List<CompanionDevice> | Return all paired wearable devices |
| `isBluetoothAvailable()` | boolean | Check Bluetooth adapter availability |
| `isBleScanningSupported()` | boolean | Check BLE scanning hardware support |
| `addListener(PairingListener)` | void | Register for pairing events |
| `removeListener(PairingListener)` | void | Unregister pairing event listener |

### CompanionDevice
```java
public static class CompanionDevice {
    public final String macAddress;
    public final String deviceName;
    public final String nodeId;
    public final int deviceType;
    public final long pairedTimestamp;
    public boolean isConnected;
}
```

### PairingListener Interface
```java
public interface PairingListener {
    void onDeviceFound(CompanionDevice device);
    void onPairingCompleted(CompanionDevice device);
    void onPairingFailed(String macAddress, int errorCode);
    void onDeviceDisconnected(CompanionDevice device);
}
```

## WearableServiceImpl

### Channel API Methods
```java
public void openChannel(IWearableCallbacks callbacks, String targetNodeId, String path)
public void addListener(IWearableCallbacks callbacks, AddListenerRequest request)
```

## Constants

### Bluetooth Service UUIDs
| Constant | Value | Purpose |
|----------|-------|---------|
| `WEAR_OS_SERVICE_UUID` | `0000fca5-0000-1000-8000-00805f9b34fb` | Wear OS primary service |
| `DEVICE_INFORMATION_SERVICE_UUID` | `0000180a-0000-1000-8000-00805f9b34fb` | Device info |
| `BATTERY_SERVICE_UUID` | `0000180f-0000-1000-8000-00805f9b34fb` | Battery level |

### Wear OS Device Name Prefixes
| Prefix | Device Type |
|--------|-------------|
| `Galaxy Watch` | Samsung |
| `Pixel Watch` | Google |
| `TicWatch` | Mobvoi |
| `Fossil` | Fossil Group |
| `Skagen` | Fossil Group |
| `Misfit` | Fossil Group |
| `Michael Kors` | Fossil Group |
| `Montblanc` | Richemont |
| `TAG Heuer` | LVMH |
| `OPPO Watch` | OPPO |
| `Xiaomi Watch` | Xiaomi |
| `OnePlus Watch` | OnePlus |
| `Huawei Watch` | Huawei |

## Bluetooth Connection States

| State | Description |
|-------|-------------|
| `DISCONNECTED` | No active connection |
| `CONNECTING` | RFCOMM socket connection in progress |
| `CONNECTED` | RFCOMM channel established |
| `PAIRING` | Bluetooth bonding in progress |
| `PAIRED` | Device bonded, awaiting RFCOMM |
| `ERROR` | Connection failed with error |

## Channel API Data Flow

```
App on Phone                    Wear OS Device
     |                                |
     |-- openChannel(nodeId, path) -->|
     |                                |
     |<-- ChannelOpened(token) -------|
     |                                |
     |-- getOutputStream(token) ----->|
     |-- write(data) ---------------->|
     |-- write(data) ---------------->|
     |-- flush() -------------------->|
     |                                |
     |<-- onInputClosed(token) -------|
     |                                |
     |-- closeChannel(token) -------->|
```

## Error Codes

| Code | Name | Description |
|------|------|-------------|
| 0 | `SUCCESS` | Operation completed successfully |
| 1 | `BLUETOOTH_UNAVAILABLE` | Bluetooth adapter not present |
| 2 | `BLUETOOTH_DISABLED` | Bluetooth is turned off |
| 3 | `PERMISSION_DENIED` | Missing BLUETOOTH/CONNECT permission |
| 4 | `DEVICE_NOT_FOUND` | Target device not in range |
| 5 | `PAIRING_TIMEOUT` | Pairing operation exceeded time limit |
| 6 | `PAIRING_REJECTED` | User rejected pairing request |
| 7 | `CONNECTION_LOST` | RFCOMM channel unexpectedly closed |
| 8 | `INTERNAL_ERROR` | Unspecified internal failure |
| 9 | `CHANNEL_CLOSED` | Channel was closed by remote peer |
| 10 | `BUFFER_FULL` | Channel output buffer capacity exceeded |

## Service Lifecycle

### Initialization
```java
WearableServiceImpl service = new WearableServiceImpl(context);
// Service registers with Binder, initializes Bluetooth manager,
// and loads paired device state from SharedPreferences.
```

### Runtime Permissions
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) 
        != PackageManager.PERMISSION_GRANTED) {
        // Request permission with rationale
        requestPermissions(new String[]{
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        }, REQUEST_BLUETOOTH);
    }
}
```

### Service Shutdown
```java
service.onDestroy();
// Closes all open channels, disconnects Bluetooth sockets,
// removes BroadcastReceiver, and persists paired device state.
```

---
Last updated: 2026-07-29
