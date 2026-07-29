# Notification & Call Bridging Architecture

## Notification Bridge

### Flow
```
Android NotificationManager
         │
         ▼
NotificationListenerService (system)
         │
         ▼
NotificationBridge.onNotificationPosted()
         │
         ├── Extract: title, text, icon, package, timestamp
         ├── Serialize to Wearable protocol buffer
         └── WearableImpl.sendMessage(MEDIA_PATH, payload)
                  │
                  ▼
         Wear OS Device Notification Shade
```

### Notification Filter Rules
| Rule | Condition | Action |
|------|-----------|--------|
| Package blacklist | `packageName in blacklist` | Drop |
| Silent notifications | `notification.isSilent()` | Drop |
| Ongoing | `notification.isOngoing()` | Forward with lower priority |
| Media | `notification.isMedia()` | Forward via MediaBridge instead |
| Messaging | `MessagingStyle` detected | Include conversation metadata |
| Group | `notification.getGroup()` | Include group key for bundling |

### Serialized Payload Format
```json
{
  "id": 12345,
  "package": "com.whatsapp",
  "title": "Alice",
  "text": "See you at 5pm",
  "icon": "<base64>",
  "timestamp": 1690000000000,
  "category": "msg",
  "actions": [
    {"label": "Reply", "action": "reply"},
    {"label": "Mark read", "action": "mark_read"}
  ],
  "group": "conversation_42",
  "priority": 1
}
```

## Call Bridge

### State Machine
```
                    ┌──────────┐
                    │  IDLE    │
                    └────┬─────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
    ┌──────────┐  ┌────────────┐  ┌───────────┐
    │ RINGING  │  │ DIALING    │  │ CONNECTED │
    └────┬─────┘  └─────┬──────┘  └─────┬─────┘
         │              │               │
         ▼              ▼               ▼
    ┌──────────────────────────────────────────┐
    │              ENDED / MISSED               │
    └──────────────────────────────────────────┘
```

### Call State Payload
```json
{
  "state": "RINGING",
  "number": "+33612345678",
  "name": "Laurent",
  "timestamp": 1690000000000,
  "duration": 0,
  "type": "incoming"
}
```

### TelecomManager Integration
```java
// Register phone state listener
TelecomManager telecom = context.getSystemService(TelecomManager.class);
PhoneStateListener listener = new PhoneStateListener() {
    @Override
    public void onCallStateChanged(int state, String number) {
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                callBridge.onIncomingCall(number);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                callBridge.onCallAnswered(number);
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                callBridge.onCallEnded(number);
                break;
        }
    }
};
```

## Media Bridge

### MediaSession Monitoring
```
MediaSessionManager.getActiveSessions()
         │
         ▼
MediaController (active session)
         │
         ├── PlaybackState callback
         │   ├── STATE_PLAYING → forward
         │   ├── STATE_PAUSED → forward
         │   ├── STATE_STOPPED → forward
         │   └── position/duration → forward
         │
         └── MediaMetadata callback
             ├── METADATA_KEY_TITLE → "title"
             ├── METADATA_KEY_ARTIST → "artist"
             ├── METADATA_KEY_ALBUM → "album"
             ├── METADATA_KEY_DURATION → duration
             └── METADATA_KEY_ALBUM_ART → base64 thumbnail
```

### Media Command Protocol
```
Watch → Phone commands received via MEDIA_COMMAND_PATH:

| Command Byte | Action |
|-------------|--------|
| 0x01 | Play |
| 0x02 | Pause |
| 0x03 | Toggle play/pause |
| 0x04 | Next track |
| 0x05 | Previous track |
| 0x10 | Volume up |
| 0x11 | Volume down |
| 0x20 | Seek forward (+10s) |
| 0x21 | Seek backward (-10s) |
| 0x30 | Toggle shuffle |
| 0x31 | Toggle repeat |
```

### Album Art Transfer
```
Phone                                    Watch
  │                                        │
  │── /wearable/asset/album_art ──────────>│
  │   (DataItem API, max 100KB)            │
  │                                        │
  │<── /wearable/asset/album_art/ack ──────│
```

## Channel API Data Streams

### Channel Lifecycle
```
openChannel(nodeId, path)
        │
        ▼
   ChannelOpened(token)
        │
   ┌────┴────┐
   │         │
   ▼         ▼
getInputStream   getOutputStream
   │              │
   │ read()       │ write(data)
   │              │ flush()
   │              │
   ▼              ▼
closeChannel(token)
        │
        ▼
   ChannelClosed(token)
```

### Use Cases
| Use Case | Path Pattern | Direction | Max Size |
|----------|-------------|-----------|----------|
| File transfer | `/wearable/file/{name}` | Bidirectional | 50 MB |
| Sensor data | `/wearable/sensor/{type}` | Watch → Phone | 1 KB/s |
| Config sync | `/wearable/config` | Phone → Watch | 10 KB |
| Firmware update | `/wearable/firmware` | Phone → Watch | 100 MB |
| Log stream | `/wearable/logs` | Watch → Phone | Unlimited |
