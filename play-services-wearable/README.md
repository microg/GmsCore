<!--
SPDX-FileCopyrightText: 2026 microG Project Team
SPDX-License-Identifier: Apache-2.0
-->

# play-services-wearable

Client library and service implementation for the Wearable Data Layer APIs used by Wear OS
companion apps and phone↔watch app pairs.

## Current support (foundation)

This module now provides:

1. **Client API facades** — `NodeApi`, `DataApi`, and `MessageApi` delegate to
   `WearableServiceImpl` (they previously threw `UnsupportedOperationException`).
2. **Wear OS TOS activity** — `TermsOfServiceActivity` shows an accept/decline dialog so
   companion apps (Galaxy Wearable, Wear OS) are not blocked by an immediate
   `RESULT_CANCELED` (see #2444 / #2843).
3. **Bluetooth RFCOMM transport** — `BluetoothConnectionThread` speaks the existing
   length-prefixed protobuf wire format over Bluetooth Classic, using UUIDs documented in
   [teccheck/wearos-research](https://github.com/teccheck/wearos-research/blob/main/docs/btcomm.md):
   - `5e8945b0-9525-11e3-a5e2-0800200c9a66` — WearableBt (watch is server; phone connects)
   - `fafbdd20-83f0-4389-addf-917ac9dae5b2` — Flow (phone is server)
   - `6a1eafb1-61c0-42a0-8bb0-a336fb1c3f00` — Flow15 (phone is server)

When a `ConnectionConfiguration` with a Bluetooth MAC (`address`) is enabled, microG starts a
WearableBt client connection to that device and ensures Flow/Flow15 listeners are running.
The legacy TCP server on port `5601` (config name `"server"`) is unchanged for emulator use.

## What is still missing for full Wear OS

Stock watches will not gain notification mirroring or media controls from custom message paths
alone. Those features depend on a successful companion pairing session and additional
protocol/OEM work beyond the Data Layer framing. Physical-device verification is required
before claiming end-to-end Wear OS support for #2843.

## Testing

```bash
./gradlew :play-services-wearable-core:testDebugUnitTest
```

Unit tests assert the RFCOMM UUIDs match the researched values (and reject known-wrong UUIDs
from earlier PRs).
