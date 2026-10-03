/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.test.AndroidTestCase;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.wearable.SocketConnectionThread;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.RootMessage;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Isolated configuration state with real loopback connections on ephemeral ports, not pairing evidence. */
public class WearableEmulatorSwitchTest extends AndroidTestCase {
    private NodeDatabaseHelper nodes;
    private ConfigurationDatabaseHelper configurations;
    private WearableImpl wearable;
    private final List<SocketConnectionThread> servers = new ArrayList<>();
    private final List<Socket> clients = new ArrayList<>();

    @Override protected void setUp() throws Exception {
        super.setUp();
        assertEquals("org.microg.gms.wearable.core.test", getContext().getPackageName());
        getContext().deleteDatabase("node.db");
        getContext().deleteDatabase("connectionconfig.db");
        nodes = new NodeDatabaseHelper(getContext());
        configurations = new ConfigurationDatabaseHelper(getContext());
        start();
    }

    private void start() throws Exception {
        wearable = new WearableImpl(getContext(), nodes, configurations);
        CountDownLatch ready = new CountDownLatch(1);
        assertTrue(wearable.networkHandler.post(ready::countDown));
        assertTrue(ready.await(3, TimeUnit.SECONDS));
    }

    private void stop() throws Exception {
        if (wearable == null) return;
        Thread network = wearable.networkHandler.getLooper().getThread();
        wearable.stop();
        network.join(3000);
        assertFalse(network.isAlive());
        wearable = null;
    }

    @Override protected void tearDown() throws Exception {
        try {
            stop();
        } finally {
            for (SocketConnectionThread server : servers) server.close();
            for (Socket client : clients) client.close();
            for (SocketConnectionThread server : servers) {
                server.join(3000);
                assertFalse(server.isAlive());
            }
            if (configurations != null) configurations.close();
            if (nodes != null) nodes.close();
            getContext().deleteDatabase("connectionconfig.db");
            getContext().deleteDatabase("node.db");
            super.tearDown();
        }
    }

    private ConnectionConfiguration config(String name, String peer, boolean enabled) {
        return new ConnectionConfiguration(name, "EmulatorAddr-" + peer, 2, 2, enabled);
    }

    private Connect peer(String id) {
        return new Connect.Builder().id(id).name("Synthetic emulator").build();
    }

    private Object field(String name) throws Exception {
        Field field = WearableImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(wearable);
    }

    private void field(String name, Object value) throws Exception {
        Field field = WearableImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(wearable, value);
    }

    private Socket connect(int port) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        do {
            try {
                Socket client = new Socket("127.0.0.1", port);
                clients.add(client);
                return client;
            } catch (IOException e) { Thread.sleep(10); }
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Isolated listener did not start");
    }

    private SocketConnectionThread select(String name, String peerId) throws Exception {
        ConnectionConfiguration stored = configurations.putManagedConfiguration(config(name, peerId, true), wearable.getLocalNodeId());
        // Invalidate the runtime cache and retire the old transport through the actual update API.
        // The socket fixture uses an ephemeral port instead of the product's fixed ADB port.
        wearable.updateConnection(stored);
        assertNull(field("sct"));
        int port;
        try (ServerSocket reservation = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = reservation.getLocalPort();
        }
        CountDownLatch accepted = new CountDownLatch(1);
        WearableImpl owner = wearable;
        SocketConnectionThread server = SocketConnectionThread.serverListen(getContext(), port, new WearableConnection.Listener() {
            private WearableConnection connection;
            @Override public void onConnected(WearableConnection value) { connection = value; accepted.countDown(); }
            @Override public void onMessage(WearableConnection value, RootMessage message) { }
            @Override public void onDisconnected() { owner.onDisconnectReceived(connection, peer(peerId)); }
        });
        servers.add(server);
        field("sct", server);
        field("tcpConfigurationName", name);
        server.start();
        connect(port);
        assertTrue(accepted.await(3, TimeUnit.SECONDS));
        return server;
    }

    private void associate(SocketConnectionThread server, String name, String peerId) {
        wearable.onConnectReceived(server.getWearableConnection(), configurations.getConfiguration(name), peer(peerId));
    }

    public void testSwitchAndReturnKeepsIdentitiesAndRejectsLateOldTransport() throws Exception {
        SocketConnectionThread first = select("wear61", "peer61");
        ConnectionConfiguration originalSnapshot = configurations.getConfiguration("wear61");
        associate(first, "wear61", "peer61");
        SocketConnectionThread second = select("wear60", "peer60");
        associate(second, "wear60", "peer60");
        assertFalse(configurations.getConfiguration("wear61").enabled);
        assertEquals("peer61", configurations.getConfiguration("wear61").nodeId);
        assertNull(wearable.connectionForNode("peer61"));
        clients.get(0).setSoTimeout(1000);
        assertEquals(-1, clients.get(0).getInputStream().read());

        SocketConnectionThread returned = select("wear61", "peer61");
        associate(returned, "wear61", "peer61");
        assertFalse(configurations.getConfiguration("wear60").enabled);
        assertEquals("peer60", configurations.getConfiguration("wear60").nodeId);
        assertEquals(2, configurations.getAllConfigurations().length);
        wearable.onDisconnectReceived(first.getWearableConnection(), peer("peer61"));
        assertSame(returned.getWearableConnection(), wearable.connectionForNode("peer61"));
        try {
            wearable.onConnectReceived(first.getWearableConnection(), originalSnapshot, peer("peer61"));
            fail("An old listener must not revive after selecting the same emulator again");
        } catch (IllegalArgumentException expected) { }
        assertSame(returned.getWearableConnection(), wearable.connectionForNode("peer61"));
        assertEquals("peer61", configurations.getConfiguration("wear61").nodeId);
    }

    public void testUnexpectedPeerCannotClaimTheSelectedEmulator() throws Exception {
        SocketConnectionThread selected = select("selected", "expected-peer");
        try {
            associate(selected, "selected", "different-peer");
            fail("The address supplied by EmulatorActivity is the expected remote node ID");
        } catch (IllegalArgumentException expected) { }
        assertEquals(wearable.getLocalNodeId(), configurations.getConfiguration("selected").nodeId);
        assertNull(wearable.connectionForNode("different-peer"));
        associate(selected, "selected", "expected-peer");
        assertSame(selected.getWearableConnection(), wearable.connectionForNode("expected-peer"));
    }

    public void testDisabledAndFailedPutsKeepTheCurrentTransport() throws Exception {
        SocketConnectionThread first = select("first", "peer1");
        associate(first, "first", "peer1");
        wearable.createConnection(config("second", "peer2", false));
        assertSame(first, field("sct"));
        assertSame(first.getWearableConnection(), wearable.connectionForNode("peer1"));
        try {
            wearable.createConnection(config("first", "different-peer", true));
            fail("A name collision must fail before closing the active transport");
        } catch (IllegalArgumentException expected) { }
        assertSame(first, field("sct"));
        assertSame(first.getWearableConnection(), wearable.connectionForNode("peer1"));
        assertTrue(configurations.getConfiguration("first").enabled);
        assertFalse(configurations.getConfiguration("second").enabled);
    }

    public void testAmbiguousLegacyEnabledEmulatorsAreNotSelectedAtStartup() throws Exception {
        stop();
        configurations.putConfiguration(new ConnectionConfiguration("first", "EmulatorAddr-peer1", 2, 2, true, "peer1"));
        configurations.putConfiguration(new ConnectionConfiguration("second", "EmulatorAddr-peer2", 2, 2, true, "peer2"));
        start();
        assertNull(field("sct"));
        assertTrue(wearable.getConnectedNodesParcelableList().isEmpty());
        assertTrue(configurations.getConfiguration("first").enabled);
        assertTrue(configurations.getConfiguration("second").enabled);
    }

    public void testDisabledGenericAliasCannotCloseTheActiveSpecificTransport() throws Exception {
        configurations.putManagedConfiguration(new ConnectionConfiguration("server", null, 2, 2, true), wearable.getLocalNodeId());
        configurations.putConfiguration(new ConnectionConfiguration("server", null, 2, 2, true, "peer1"), wearable.getLocalNodeId());
        SocketConnectionThread specific = select("specific", "peer1");
        associate(specific, "specific", "peer1");
        assertFalse(configurations.getConfiguration("server").enabled);
        assertEquals("peer1", configurations.getConfiguration("server").nodeId);
        for (ConnectionConfiguration cached : wearable.getConfigurations()) {
            if (cached.name.equals("server")) cached.connected = true; // A residual flag from a retired listener.
        }
        wearable.updateConnection(config("specific", "peer1", true));
        assertSame(specific, field("sct"));
        assertSame(specific.getWearableConnection(), wearable.connectionForNode("peer1"));
        assertTrue(specific.isAlive());
        assertEquals(2, configurations.getAllConfigurations().length);
        for (ConnectionConfiguration cached : wearable.getConfigurations()) {
            if (cached.name.equals("server")) assertFalse(cached.connected);
            if (cached.name.equals("specific")) assertTrue(cached.connected);
        }
    }
}
