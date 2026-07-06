# Google Play Services Wearable

This module implements the Wearable API (`com.google.android.gms.wearable`), enabling microG to communicate with Wear OS devices.

## Features

### Bluetooth RFCOMM Transport (`BluetoothConnectionThread.java`)
- Implements the standard Wear OS Bluetooth RFCOMM UUID (`a3c87500-8ed3-4bdf-8a39-a01bebede295`)
- Wraps `BluetoothSocket` in a `java.net.Socket` proxy to reuse the existing `SocketWearableConnection` framing code
- Supports both server (listen) and client (connect) modes

### Phone Call Handling (`CallBridge.java`)
- Monitors phone call state (idle, ringing, off-hook)
- Sends call state changes to connected Wear OS devices
- Handles incoming call actions from the watch: answer, end, silence

### Media Controls (`MediaBridge.java`)
- Registers as a media session listener to receive playback state updates
- Forwards play/pause/skip events from the phone to connected Wear OS devices

### Notification Bridging (`WearableNotificationService.java`)
- Extends `NotificationListenerService` to intercept phone notifications
- Forwards clearable, non-ongoing notifications to connected Wear OS devices
- Supports notification removal events (same UID on removal)

### Wearable Channels API (`ChannelManager.java`)
- Implements the full `ChannelGetInputStream` / `ChannelGetOutputStream` AIDL interface
- Supports stream and file transfers between phone and watch

### Settings UI (`play-services-core`)
- `WearableFragment.kt`: Settings screen with auto-accept TOS toggle
- `TermsOfServiceActivity.kt`: Shows a dialog on first pairing; auto-accept if enabled
- `WearablePreferences.kt`: Persistent preference storage

## Permissions Added

- `BLUETOOTH`, `BLUETOOTH_ADMIN` (up to API 30)
- `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`
- `ANSWER_PHONE_CALLS`
- `MEDIA_CONTENT_CONTROL`

## Relates to

- Issue [#2843](https://github.com/microg/GmsCore/issues/2843) — WearOS Support bounty
- PR [#3473](https://github.com/microg/GmsCore/pull/3473)
