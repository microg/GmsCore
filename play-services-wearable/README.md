<!--
SPDX-FileCopyrightText: 2026 microG Project Team
SPDX-License-Identifier: Apache-2.0
-->

# play-services-wearable

Client library and service implementation for the Wearable Data Layer APIs used by Wear OS
companion apps and phone↔watch app pairs.

## Support for Wear OS (#2843 / #2444)

This module provides:

1. **Wear OS TOS activity** — `TermsOfServiceActivity` shows an accept/decline dialog so
   companion apps (Galaxy Wearable, Wear OS) are not blocked by an immediate
   `RESULT_CANCELED` (see #2444).
2. **Client API facades** — `NodeApi`, `DataApi`, and `MessageApi` delegate to
   `WearableServiceImpl` (they previously threw `UnsupportedOperationException`).
3. **Bluetooth RFCOMM transport** — `BluetoothConnectionThread` speaks the existing
   length-prefixed protobuf wire format over Bluetooth Classic, using UUIDs documented in
   [teccheck/wearos-research](https://github.com/teccheck/wearos-research/blob/main/docs/btcomm.md):
   - `5e8945b0-9525-11e3-a5e2-0800200c9a66` — WearableBt (watch is server; phone connects)
   - `fafbdd20-83f0-4389-addf-917ac9dae5b2` — Flow (phone is server)
   - `6a1eafb1-61c0-42a0-8bb0-a336fb1c3f00` — Flow15 (phone is server)
4. **Notification mirroring** — `WearableNotificationListenerService` forwards posted/removed
   notifications to connected peers on `/wearable/notification/{posted,removed}`.
5. **Media controls** — `WearableMediaSessionBridge` pushes playback state on
   `/wearable/media/state` and handles watch commands under `/wearable/media/control/*`
   (play, pause, toggle, next, previous, seek, rate).

When a `ConnectionConfiguration` with a Bluetooth MAC (`address`) is enabled, microG starts a
WearableBt client connection to that device and ensures Flow/Flow15 listeners are running.
The legacy TCP server on port `5601` (config name `"server"`) is unchanged for emulator use.

Enable the **Wear OS notification bridge** notification listener in system settings so
notification mirroring and media-session access can work.

## Remaining device-specific work

OEM companion pairing handshakes (Galaxy Wearable / Wear OS by Google setup wizards) still
benefit from physical-device verification. Stock watch UI may expect additional OEM protocols
beyond the Data Layer paths above.

## Testing

```bash
./gradlew :play-services-wearable-core:testDebugUnitTest
```

Unit tests cover RFCOMM UUIDs (rejecting known-wrong values from earlier PRs), notification
payload encoding, and media control parsing.
