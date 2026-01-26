# Proposal for Issue #2843: WearOS Support Implementation

Hi @mar-v-in and MicroG team,

I am interested in working on this bounty. I have performed an initial analysis of the `play-services-wearable` module and gathered the following findings. I believe a systematic approach to implementing the missing IPC methods and capability exchange is required to get a stable connection with WearOS devices.

## Technical Analysis

The current `WearableServiceImpl` contains the skeleton for `IWearableService`, but several critical paths needed for the initial handshake and app negotiation are unimplemented:

1.  **Capability Exchange**:
    *   Methods `getLocalNode`, `getConnectedCapability`, `addLocalCapability`, and `removeLocalCapability` are largely stubs or rely on a `CapabilityManager` that needs verification.
    *   Without correct capability exchange (`CAPABILITY_CHANGED` events), the companion app on the watch cannot verify that the host GmsCore supports the required features, causing the connection to hang or drop.

2.  **Channel API**:
    *   `openChannel`, `closeChannel`, and `getChannelInputStream`/`OutputStream` are stubbed (`unimplemented Method`).
    *   Modern WearOS apps rely heavily on `ChannelClient` for high-bandwidth data transfer (voice, images) rather than just `DataMap` syncing.

3.  **Connection Handshake**:
    *   `WearableImpl` uses `SocketConnectionThread` on port 5601. I suspect the handshake protocol needs to be robustly matched against the official GMS protocol to properly acknowledge `Connect` and `RootMessage` packets during the initial pairing flow.

## Proposed Implementation Plan

I propose to tackle this in three phases:

*   **Phase 1: Capability & Node Sync (The Foundation)**
    *   Implement `addLocalCapability` / `removeLocalCapability` persistence.
    *   Ensure `getConnectedNodes` returns the correct peer status immediately after the socket handshake.
    *   Goal: `WearableListenerService` on the watch should correctly fire `onPeerConnected`.

*   **Phase 2: Message Path & Reliability**
    *   Verify `sendMessage` routing in `WearableImpl` to ensure reliable delivery to the target node ID.
    *   Implement `sendRemoteCommand` if required for specific system events.

*   **Phase 3: Channel API (The "Heavy/WearOS" part)**
    *   Implement the missing `IChannelStreamCallbacks` to handle byte streams over the existing socket connection.

## Timeline & Commitment

I can start immediately by reproducing the connection failure with a standard WearOS emulator and `adb` port forwarding. I will avoid "AI-generated slop" and focus on clean, reverse-engineered implementations consistent with the existing codebase style.

Please let me know if there are specific "gotchas" with the `SocketConnectionThread` I should be aware of before diving in.

Best regards,
[Your Name]
