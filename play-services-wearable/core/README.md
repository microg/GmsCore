# Wearable transport and channel implementation

This module exposes the existing Wearable Binder API over a paired, secure
Bluetooth RFCOMM connection. The legacy TCP transport remains available on IPv4
loopback for a local ADB emulator bridge; it does not accept LAN connections.
TCP requires a debuggable build running on an Android emulator. Several emulator
configurations may be stored, but only one can be enabled at a time. Selecting
another emulator through the companion closes the previous transport and keeps
both configurations' negotiated identities and data. Its configured address uses
`EmulatorAddr-` followed by the expected node ID, never a socket destination. Simultaneous TCP
connections to multiple emulators are not supported. Ambiguous legacy enabled
configurations are not automatically selected during startup.

## Protocol boundaries

- A peer must negotiate a nonempty node identity and an overlapping protocol
  version before application messages are accepted. A socket connection alone
  does not advertise a connected node.
- Bluetooth configurations are restored when the service starts. Retry delays
  increase from 3 to 30 seconds; disabling/deleting a configuration or stopping
  the service closes its socket and cancels retries.
- Message assembly is limited to 16 MiB, eight incomplete queues, and 64 KiB
  pieces. Encoded metadata is included in the buffer budget.
- Message and data callbacks are addressed to the requested package and checked
  against its installed signing certificate. Channel tokens are local opaque
  identifiers, scoped to the requesting package and to one transport session.

## Channel wire contract

The inherited protocol definition requires two compatibility details which
cannot be verified by a round trip between two copies of the same encoder:

1. Channel identifiers use `fixed64`, in control field 2 and data-header field 1.
   The sequence number in data-header field 3 remains `int64`.
2. Request fields 1–6 must be present for current peers. In particular, field 5
   is explicitly zero and a channel request's path is explicitly the empty
   string. Channel envelopes use the nonpersistent generation zero.

Control values are OPEN=1, OPEN_ACK=2 and CLOSE=3. `fromChannelOperator` identifies
the opener; it is inverted when identifying the local session for an incoming
control, data packet or acknowledgement. Channel protocol version is field 6;
origin is field 7. Only the Channel API origin is currently implemented.

Opening succeeds only after receiving OPEN_ACK. There is a 15-second opening
timeout. Each stream uses stop-and-wait flow control and at most one 64 KiB
pending packet; incoming data is acknowledged after writing it to the consumer's
pipe. Blocking pipe operations run separately from the Bluetooth reader and
the network dispatch handler. There are at most eight sessions and 16 stream
workers. Admission before dispatch permits 32 bounded requests, including data,
ACK and control bursts from bidirectional channels.

Disconnecting invalidates the session, releases its buffers and closes its file
descriptors. A reconnect does not inherit old channel tokens or queued frames.
Each direction emits its closure callback/event once. Logs contain lifecycle
events and failure locations, never message payloads, certificate contents or
exception messages from the peer.

## Verification

Run `:play-services-wearable-core:testDebugUnitTest` with the project's supported
JDK (Java 21 in the development environment). Tests cover framing, asset bounds,
negotiation, package routing, TCP closure, channel flow control/admission and
literal fixed64 wire bytes and required fields.

Instrumented tests (`:play-services-wearable-core:connectedDebugAndroidTest`)
cover SQLite storage, capability dispatch, RPC sequencing and cancellation,
configuration Binder calls and consent callbacks on a real Android runtime.

Unit results and APK assembly do not establish Wear OS pairing compatibility.
An end-to-end test must complete the ordinary companion/watch setup, show
notifications and their removal, execute media commands, exchange data with a
signed app on both devices, and recover after a cold start. Do not manually set
`user_setup_complete` or `device_provisioned` to make such a test pass. A generic
SDK watch also does not establish compatibility with a particular manufacturer's
watch.

### Manual end-to-end check

1. Pair a watch through the official companion app (Bluetooth, or the SDK
   emulator bridge on a debuggable emulator build) and accept the terms screen.
2. In the companion, open *Google → Accounts → Add account*, confirm the microG
   consent screen and the device screen lock, then complete Google's own
   verification page. The watch must list the account and its sync adapters must
   succeed.
3. Post, update and remove a notification on the phone; dismiss one on the
   watch and check that it disappears from the phone as well.
4. Control a phone media player (play, pause, next, previous) from the watch.
   The player's own queue decides whether next/previous are offered: a
   single-item queue advertises no `SKIP_TO_NEXT`, and many players restart the
   current track on the first *previous*.
5. Exchange messages, data items and assets with a Data Layer sample app.

## Account transfer to the watch

The companion copies a Google account by calling the source side of the
direct-transfer service (`org.microg.gms.smartdevice.directtransfer`, in the
`play-services-core` module). microG implements that source role:

- Only the official Pixel Watch companion, verified by package, signing
  certificate and Binder UID, may start a transfer.
- The phone shows its own consent screen naming the account, then requires the
  Android device credential. Devices without a secure screen lock are refused.
- The session is encrypted with UKEY2/D2D before any account data is exchanged.
  Bootstrap assertions are signed with per-account CryptAuth keys that are
  stored privately and excluded from backups.
- When Google asks for interactive verification, its page is shown in an
  isolated WebView profile in the `:persistent` process (Android 9+, WebView
  with `MULTI_PROFILE`). Navigation is limited to `accounts.google.com`; only
  the resulting checkpoint cookie is used and the profile is deleted afterwards.
- A transfer is reported as complete only after the watch returns success for
  the selected account and the final acknowledgement has been written.
  Cancellation, timeouts and malformed messages end it with a fixed error reason;
  logs never contain account names, tokens or payloads.

Other companion apps, enterprise/supervised accounts and alternative bootstrap
modes are not supported by this path.

## Capabilities and media permission

Declared `android_wear_capabilities` are reconciled when the owning app binds;
dynamic capabilities are persisted separately. Queries exclude the local node
and filter by the caller's package/certificate. A full installed-app startup
scan and package lifecycle reconciliation are not yet implemented.

`WearableMediaBridge` requires the Android notification-listener permission. It
publishes bounded metadata to the official watch media package and accepts only
bounded commands from the installed companion identity over the current paired
session. Revocation removes local state and prevents persisted media metadata
from being synchronized. It does not grant account authentication or transfer.

The local framing sources and initial schema originate from the Apache-2.0
licensed `microg/Wearable` project; their existing attribution is retained.
