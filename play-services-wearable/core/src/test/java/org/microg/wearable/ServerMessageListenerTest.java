/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.wearable;

import org.junit.Test;
import org.microg.wearable.proto.*;

import static org.junit.Assert.*;

public class ServerMessageListenerTest {
    private static final class TestListener extends ServerMessageListener {
        boolean receivedAsset;
        TestListener() {
            super(new Connect.Builder().id("phone").peerVersion(1).peerMinimumVersion(0).build());
        }
        @Override public void onSetAsset(SetAsset value) { receivedAsset = true; }
        @Override public void onAckAsset(AckAsset value) {}
        @Override public void onFetchAsset(FetchAsset value) {}
        @Override public void onSyncStart(SyncStart value) {}
        @Override public void onSetDataItem(SetDataItem value) {}
        @Override public void onRpcRequest(Request value) {}
        @Override public void onHeartbeat(Heartbeat value) {}
        @Override public void onFilePiece(FilePiece value) {}
        @Override public void onChannelRequest(Request value) {}
    }

    @Test public void acceptsOverlappingProtocolVersions() {
        TestListener listener = new TestListener();
        Connect peer = new Connect.Builder().id("watch").peerVersion(2).peerMinimumVersion(0).build();
        listener.onConnect(peer);
        assertEquals(peer, listener.getRemoteConnect());
    }

    @Test public void rejectsMissingOrLocalIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new TestListener().onConnect(new Connect.Builder().build()));
        assertThrows(IllegalArgumentException.class, () -> new TestListener().onConnect(new Connect.Builder().id("phone").build()));
        assertThrows(IllegalArgumentException.class, () -> new TestListener().onConnect(new Connect.Builder().id("").build()));
    }

    @Test public void rejectsIncompatibleOrInvalidProtocolVersions() {
        assertThrows(IllegalArgumentException.class, () -> new TestListener().onConnect(
                new Connect.Builder().id("watch").peerVersion(2).peerMinimumVersion(2).build()));
        assertThrows(IllegalArgumentException.class, () -> new TestListener().onConnect(
                new Connect.Builder().id("watch").peerVersion(0).peerMinimumVersion(1).build()));
        assertThrows(IllegalArgumentException.class, () -> new TestListener().onConnect(
                new Connect.Builder().id("watch").peerVersion(-1).build()));
    }

    @Test public void rejectsDataBeforeNegotiation() {
        assertThrows(IllegalArgumentException.class, () -> new TestListener().onMessage(null,
                new RootMessage.Builder().heartbeat(new Heartbeat()).build()));
    }

    @Test public void negotiatesBeforeAnyPiggybackedData() {
        TestListener listener = new TestListener();
        Connect peer = new Connect.Builder().id("watch").build();
        listener.onMessage(null, new RootMessage.Builder().connect(peer).setAsset(new SetAsset.Builder().build()).build());
        assertEquals(peer, listener.getRemoteConnect());
        assertFalse(listener.receivedAsset);
    }

    @Test public void rejectsRenegotiationButAcceptsNewSessionAfterDisconnect() {
        TestListener listener = new TestListener();
        Connect peer = new Connect.Builder().id("watch").build();
        listener.onConnect(peer);
        assertThrows(IllegalArgumentException.class, () -> listener.onConnect(peer));
        listener.onDisconnected();
        assertNull(listener.getRemoteConnect());
        listener.onConnect(peer);
        assertEquals(peer, listener.getRemoteConnect());
    }
}
