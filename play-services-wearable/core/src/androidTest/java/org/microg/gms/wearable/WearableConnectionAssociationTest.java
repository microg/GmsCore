/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.test.AndroidTestCase;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.MessagePiece;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** A synthetic connection never opens a socket; the real configuration database is isolated. */
public class WearableConnectionAssociationTest extends AndroidTestCase {
    private NodeDatabaseHelper nodes;
    private ConfigurationDatabaseHelper configurations;
    private WearableImpl wearable;

    @Override protected void setUp() throws Exception {
        super.setUp();
        assertEquals("Tests must only use the dedicated test application's storage",
                "org.microg.gms.wearable.core.test", getContext().getPackageName());
        getContext().deleteDatabase("node.db");
        getContext().deleteDatabase("connectionconfig.db");
        nodes = new NodeDatabaseHelper(getContext());
        configurations = new ConfigurationDatabaseHelper(getContext());
        wearable = new WearableImpl(getContext(), nodes, configurations);
        // Finish startup with an empty database before adding enabled fixtures. No real transport starts.
        CountDownLatch started = new CountDownLatch(1);
        assertTrue(wearable.networkHandler.post(started::countDown));
        assertTrue(started.await(3, TimeUnit.SECONDS));
    }

    @Override protected void tearDown() throws Exception {
        try {
            if (wearable != null) {
                Thread network = wearable.networkHandler.getLooper().getThread();
                wearable.stop();
                network.join(3000);
                assertFalse("The test must release its network thread", network.isAlive());
            }
        } finally {
            if (configurations != null) configurations.close();
            if (nodes != null) nodes.close();
            if (configurations != null) getContext().deleteDatabase("connectionconfig.db");
            if (nodes != null) getContext().deleteDatabase("node.db");
            super.tearDown();
        }
    }

    private static final class SyntheticConnection extends WearableConnection {
        boolean closed;
        SyntheticConnection() { super(null); }
        @Override protected void writeMessagePiece(MessagePiece piece) { }
        @Override protected MessagePiece readMessagePiece() { throw new AssertionError("No socket reads in this test"); }
        @Override public void close() { closed = true; }
    }

    private ConnectionConfiguration add(String name, String address) {
        ConnectionConfiguration config = new ConnectionConfiguration(name, address, 1, 1, true);
        return configurations.putManagedConfiguration(config, wearable.getLocalNodeId());
    }

    private ConnectionConfiguration runtime(String name) {
        for (ConnectionConfiguration config : wearable.getConfigurations()) {
            if (name.equals(config.name)) return config;
        }
        throw new AssertionError("Missing fixture " + name);
    }

    private Connect peer(String id) {
        return new Connect.Builder().id(id).name("Synthetic watch").build();
    }

    private void rejected(SyntheticConnection connection, ConnectionConfiguration expected, String peerId) {
        try {
            wearable.onConnectReceived(connection, expected, peer(peerId));
            fail("A stale or unrelated configuration must not gain a connection");
        } catch (IllegalArgumentException expectedFailure) {
            assertNull(wearable.connectionForNode(peerId));
        }
    }

    public void testTwoNewConfigurationsNegotiateOnlyTheirOwnPeer() {
        ConnectionConfiguration first = add("first", "AA:BB:CC:DD:EE:01");
        ConnectionConfiguration second = add("second", "AA:BB:CC:DD:EE:02");
        assertEquals(first.nodeId, second.nodeId);
        SyntheticConnection firstConnection = new SyntheticConnection();
        wearable.onConnectReceived(firstConnection, first, peer("peer-first"));
        assertEquals("peer-first", configurations.getConfiguration("first").nodeId);
        assertEquals(wearable.getLocalNodeId(), configurations.getConfiguration("second").nodeId);
        assertTrue(runtime("first").connected);
        assertFalse(runtime("second").connected);
        assertNull(runtime("second").peerNodeId);

        SyntheticConnection secondConnection = new SyntheticConnection();
        wearable.onConnectReceived(secondConnection, second, peer("peer-second"));
        assertEquals("peer-first", configurations.getConfiguration("first").nodeId);
        assertEquals("peer-second", configurations.getConfiguration("second").nodeId);
        assertSame(firstConnection, wearable.connectionForNode("peer-first"));
        assertSame(secondConnection, wearable.connectionForNode("peer-second"));
        assertEquals("peer-first", runtime("first").peerNodeId);
        assertEquals("peer-second", runtime("second").peerNodeId);
    }

    public void testReconnectWithOriginalSnapshotPreservesTheNegotiatedPeer() {
        ConnectionConfiguration expected = add("watch", "AA:BB:CC:DD:EE:01");
        SyntheticConnection original = new SyntheticConnection();
        wearable.onConnectReceived(original, expected, peer("known-peer"));
        SyntheticConnection replacement = new SyntheticConnection();
        wearable.onConnectReceived(replacement, expected, peer("known-peer"));
        assertTrue(original.closed);
        assertSame(replacement, wearable.connectionForNode("known-peer"));
        assertEquals("known-peer", configurations.getConfiguration("watch").nodeId);
    }

    public void testEstablishedIdentityCannotBeReassignedByAnotherHandshake() {
        ConnectionConfiguration original = add("watch", "AA:BB:CC:DD:EE:01");
        SyntheticConnection accepted = new SyntheticConnection();
        wearable.onConnectReceived(accepted, original, peer("known-peer"));
        rejected(new SyntheticConnection(), original, "other-peer");
        rejected(new SyntheticConnection(), configurations.getConfiguration("watch"), "other-peer");
        assertSame(accepted, wearable.connectionForNode("known-peer"));
        assertFalse(accepted.closed);
        assertEquals("known-peer", configurations.getConfiguration("watch").nodeId);
    }

    public void testMissingDisabledAndDifferentTransportCannotBeAssociated() {
        ConnectionConfiguration expected = add("watch", "AA:BB:CC:DD:EE:01");
        ConnectionConfiguration wrongName = new ConnectionConfiguration("missing", expected.address, 1, 1, true, expected.nodeId);
        rejected(new SyntheticConnection(), wrongName, "unknown-peer");
        ConnectionConfiguration wrongAddress = new ConnectionConfiguration(expected.name, "AA:BB:CC:DD:EE:02", 1, 1, true, expected.nodeId);
        rejected(new SyntheticConnection(), wrongAddress, "unknown-peer");
        wearable.disableConnection(expected.name);
        rejected(new SyntheticConnection(), expected, "unknown-peer");
        assertEquals(wearable.getLocalNodeId(), configurations.getConfiguration("watch").nodeId);
        assertFalse(runtime("watch").connected);
    }

    public void testDeleteThenPutSameNameAcceptsNewPeerWithoutOldCachedIdentity() {
        ConnectionConfiguration old = add("replaceable", "AA:BB:CC:DD:EE:01");
        ConnectionConfiguration other = add("retained", "AA:BB:CC:DD:EE:02");
        SyntheticConnection oldConnection = new SyntheticConnection();
        SyntheticConnection otherConnection = new SyntheticConnection();
        wearable.onConnectReceived(oldConnection, old, peer("old-peer"));
        wearable.onConnectReceived(otherConnection, other, peer("retained-peer"));

        wearable.deleteConnection("replaceable");
        // No getConfigurations call between delete and put. Creating disabled avoids real transport I/O.
        wearable.createConnection(new ConnectionConfiguration("replaceable", "AA:BB:CC:DD:EE:03", 1, 1, false));
        wearable.updateConnection(new ConnectionConfiguration("replaceable", "AA:BB:CC:DD:EE:03", 1, 1, true));
        ConnectionConfiguration replacement = configurations.getConfiguration("replaceable");
        assertEquals(wearable.getLocalNodeId(), replacement.nodeId);
        assertEquals(wearable.getLocalNodeId(), runtime("replaceable").nodeId);
        assertFalse(runtime("replaceable").connected);
        assertNull(runtime("replaceable").peerNodeId);
        assertTrue(runtime("retained").connected);
        assertEquals("retained-peer", runtime("retained").peerNodeId);

        SyntheticConnection replacementConnection = new SyntheticConnection();
        wearable.onConnectReceived(replacementConnection, replacement, peer("new-peer"));
        // A delayed disconnect from the deleted transport must not erase the replacement's state.
        wearable.onDisconnectReceived(oldConnection, peer("old-peer"));
        assertEquals("new-peer", configurations.getConfiguration("replaceable").nodeId);
        assertEquals("new-peer", runtime("replaceable").peerNodeId);
        assertTrue(runtime("replaceable").connected);
        assertSame(replacementConnection, wearable.connectionForNode("new-peer"));
        assertNull(wearable.connectionForNode("old-peer"));
        assertSame(otherConnection, wearable.connectionForNode("retained-peer"));
        assertTrue(runtime("retained").connected);
        assertEquals("retained-peer", configurations.getConfiguration("retained").nodeId);
    }
}
