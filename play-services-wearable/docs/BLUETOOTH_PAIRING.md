# Bluetooth Companion Pairing — Technical Deep Dive

## Overview

The CompanionPairingManager implements the Wear OS pairing protocol over Bluetooth
RFCOMM, following the Android Companion Device Manager specification. This document
details the internal architecture, state machine, and protocol specifics.

## State Machine

```
                    ┌─────────────┐
                    │  IDLE       │
                    └──────┬──────┘
                           │ startScanning()
                    ┌──────▼──────┐
                    │  SCANNING   │
                    └──────┬──────┘
                           │ deviceFound
                    ┌──────▼──────┐
                    │  FOUND      │──────────────┐
                    └──────┬──────┘              │ timeout
                           │ pair()              ▼
                    ┌──────▼──────┐      ┌──────────────┐
                    │  PAIRING    │      │  SCAN_TIMEOUT│
                    └──┬───────┬──┘      └──────────────┘
               success │       │ failure
                    ┌──▼──┐ ┌──▼──────┐
                    │PAIRED│ │PAIR_FAIL│
                    └──┬───┘ └─────────┘
                       │ connect()
                    ┌──▼──────┐
                    │CONNECTED│
                    └────┬────┘
                         │ disconnect
                    ┌────▼───┐
                    │DISCONN │──────┐
                    └────────┘      │ reconnect
                                    ▼
                              ┌──────────┐
                              │CONNECTING│
                              └──────────┘
```

## Bluetooth Protocol Stack

```
┌─────────────────────────────────────┐
│         Application Layer           │
│  CompanionPairingManager            │
├─────────────────────────────────────┤
│         RFCOMM Layer                │
│  BluetoothSocket (RFCOMM)           │
├─────────────────────────────────────┤
│         L2CAP Layer                 │
│  Logical Link Control               │
├─────────────────────────────────────┤
│         HCI Layer                   │
│  Host Controller Interface          │
├─────────────────────────────────────┤
│         Physical Layer              │
│  Bluetooth Radio (2.4 GHz)          │
└─────────────────────────────────────┘
```

## Wear OS Service UUIDs

The Wear OS companion protocol uses standard GATT service UUIDs:

| Service | UUID | Purpose |
|---------|------|---------|
| Primary | `0000fca5-0000-1000-8000-00805f9b34fb` | Device identification |
| Device Info | `0000180a-0000-1000-8000-00805f9b34fb` | Manufacturer, model, serial |
| Battery | `0000180f-0000-1000-8000-00805f9b34fb` | Battery level monitoring |

## Pairing Sequence

### Phase 1: Discovery (BLE Scan)
```
Phone                          Watch
  |                              |
  |── BLE Scan Request ────────>|
  |<── Scan Response ───────────|
  |    (device name, TX power)  |
  |                              |
  |── Filter by name prefix ────|
  |    (Galaxy Watch, etc.)     |
```

### Phase 2: Bonding (Bluetooth Classic)
```
Phone                          Watch
  |                              |
  |── Pairing Request ─────────>|
  |<── Pairing Response ────────|
  |                              |
  |── PIN/Passkey Exchange ────>|
  |<── Bond Created ────────────|
```

### Phase 3: Connection (RFCOMM)
```
Phone                          Watch
  |                              |
  |── RFCOMM Connect ──────────>|
  |    (WEAR_OS_SERVICE_UUID)   |
  |<── Connection Accepted ─────|
  |                              |
  |── Capability Request ──────>|
  |<── Capability Response ─────|
  |    (notification, call,     |
  |     media, channel support) |
```

## Reconnection Logic

When Bluetooth state changes:
1. `BroadcastReceiver` captures `BluetoothAdapter.ACTION_STATE_CHANGED`
2. If adapter turns ON → check paired devices list
3. For each paired device → attempt RFCOMM connect
4. Exponential backoff: 1s, 2s, 4s, 8s, max 30s
5. After 5 failures → mark device as disconnected
6. User can manually trigger reconnection

## Threading Model

```
Main Thread (UI)
    │
    ├── CompanionPairingManager (Handler)
    │   ├── startScanning() → Background ThreadPool
    │   ├── BluetoothSocket.connect() → Blocking I/O Thread
    │   └── PairingListener callbacks → Main Thread
    │
    └── WearableServiceImpl (Binder ThreadPool)
        ├── openChannel() → Background Thread
        └── IWearableCallbacks → Binder IPC
```

## Error Recovery

| Error | Action |
|-------|--------|
| Bluetooth disabled | Register BroadcastReceiver, wait for enable |
| Permission denied | Return ERROR_PERMISSION_DENIED, log warning |
| Device not found | Return ERROR_DEVICE_NOT_FOUND after 30s timeout |
| Connection lost | Attempt reconnect with backoff, notify listeners |
| Pairing timeout | Return ERROR_PAIRING_TIMEOUT after 60s |
| Buffer full | Return ERROR_BUFFER_FULL, wait for drain callback |
| Internal error | Log stack trace, return ERROR_INTERNAL, clean up resources |

## Battery Impact

- BLE scanning: ~5mA during active scan (30s window)
- RFCOMM idle: <1mA
- Notification forwarding: ~2mA per notification
- Channel data transfer: ~15mA during active transfer
- Deep sleep: <0.1mA (scanning stopped)

## Wear OS 5 Compatibility Notes

Android 15 (API 35) introduces stricter Bluetooth permissions. The pairing manager
adapts automatically:
- Scoped BLUETOOTH_CONNECT replaces general BLUETOOTH permission
- BLE scanning requires BLUETOOTH_SCAN (not just location)
- Companion device profile must declare DEVICE_TYPE_WATCH explicitly

## Known Limitations

- BLE scanning limited to 30s windows per Android power restrictions
- Maximum 8 simultaneous connected devices (Bluetooth hardware limit)
- RFCOMM throughput capped at ~800 Kbps on most devices
- Some Wear OS 1.x devices may not advertise correct service UUIDs
