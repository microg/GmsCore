/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.internal.RpcResponse;

import org.microg.gms.common.PackageUtils;
import org.microg.wearable.PendingRpcRequests;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.WearableRequests;
import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.MessagePiece;
import org.microg.wearable.proto.Request;
import org.microg.wearable.proto.RootMessage;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Real manager/Looper, protobuf writes and isolated databases; no sockets or accounts. */
public class RpcCancellationTest extends AndroidTestCase {
    private static final String PEER = "cancellation-peer";
    private NodeDatabaseHelper nodes;
    private ConfigurationDatabaseHelper configurations;
    private WearableImpl wearable;
    private SharedPreferences preferences;
    private final CapturingConnection connection = new CapturingConnection();
    private CountDownLatch releaseNetwork;

    @Override protected void setUp() throws Exception {
        super.setUp();
        assertEquals("org.microg.gms.wearable.core.test", getContext().getPackageName());
        preferences = getContext().getSharedPreferences("wearable.rpc_service.settings", 0);
        assertTrue(preferences.edit().clear().commit());
        getContext().deleteDatabase("node.db");
        getContext().deleteDatabase("connectionconfig.db");
        nodes = new NodeDatabaseHelper(getContext());
        configurations = new ConfigurationDatabaseHelper(getContext());
        wearable = new WearableImpl(getContext(), nodes, configurations);
        drainNetwork();
        ConnectionConfiguration config = configurations.putManagedConfiguration(
                new ConnectionConfiguration("cancellation", "AA:BB:CC:DD:EE:01", 1, 1, true),
                wearable.getLocalNodeId());
        wearable.onConnectReceived(connection, config, new Connect.Builder().id(PEER).build());
        drainNetwork();
    }

    @Override protected void tearDown() throws Exception {
        try {
            if (releaseNetwork != null) releaseNetwork.countDown();
            if (wearable != null) {
                Thread network = wearable.networkHandler.getLooper().getThread();
                wearable.stop();
                network.join(3000);
                assertFalse(network.isAlive());
            }
        } finally {
            if (configurations != null) { configurations.close(); getContext().deleteDatabase("connectionconfig.db"); }
            if (nodes != null) { nodes.close(); getContext().deleteDatabase("node.db"); }
            if (preferences != null) assertTrue(preferences.edit().clear().commit());
            super.tearDown();
        }
    }

    public void testDeadBinderAtLinkDoesNotConsumeAnOrderedId() throws Exception {
        Callback dead = new Callback();
        dead.die();
        send(dead);
        assertStatus(dead, 8, -1);
        assertEquals(0, connection.frames.size());
        assertNextRequestCompletes(1);
    }

    public void testDeathWhileQueuedDoesNotSendOrConsumeAnOrderedId() throws Exception {
        blockNetwork();
        // More than the per-app quota also proves that death releases each admission lease.
        for (int i = 0; i < PendingRpcRequests.MAX_PENDING_PER_PACKAGE + 1; i++) {
            Callback dead = new Callback();
            send(dead);
            assertEquals(1, dead.deaths.size());
            dead.die();
            assertEquals(0, dead.deaths.size());
            assertEquals(0, dead.responses.size());
        }
        unblockNetwork();
        assertEquals(0, connection.frames.size());
        assertNextRequestCompletes(1);
    }

    public void testFullTransportQueueRejectsWithoutConsumingAnOrderedId() throws Exception {
        blockNetwork();
        for (int i = 0; i < PendingRpcRequests.MAX_PENDING; i++) {
            Request incoming = WearableRequests.rpcRequest(WearableRequests.envelope(
                    "org.microg.lab.absent", "synthetic-signature", wearable.getLocalNodeId(), PEER,
                    "/ignored", 1, i + 1), new byte[0], 0).rpcRequestWithResponse;
            wearable.requests.receive(connection, PEER, incoming);
        }
        Callback rejected = new Callback();
        send(rejected);
        assertStatus(rejected, 16, -1);
        assertEquals(0, rejected.deaths.size());
        unblockNetwork();
        assertEquals(0, connection.frames.size());
        assertNextRequestCompletes(1);
        assertEquals(1, rejected.responses.size());
    }

    public void testDisconnectRemovesQueuedReservationAndCompletesOnce() throws Exception {
        blockNetwork();
        Callback disconnected = new Callback();
        send(disconnected);
        wearable.requests.disconnected(connection);
        assertStatus(disconnected, 4000, -1);
        disconnected.die();
        unblockNetwork();
        assertEquals(0, connection.frames.size());
        // Only the manager's transport cancellation was invoked; the fixture connection remains mapped.
        assertNextRequestCompletes(1);
        assertEquals(1, disconnected.responses.size());
    }

    public void testStopCompletesUnsentReservationWithoutWriting() throws Exception {
        blockNetwork();
        Callback stopped = new Callback();
        send(stopped);
        wearable.requests.stop();
        assertStatus(stopped, 16, -1);
        stopped.die();
        unblockNetwork();
        assertEquals(0, connection.frames.size());
        assertEquals(1, stopped.responses.size());
        assertEquals(0, stopped.deaths.size());
    }

    public void testDeathAfterCommitCannotSuppressTheFrame() throws Exception {
        Callback dead = new Callback();
        connection.beforeWrite = dead::die;
        send(dead);
        drainNetwork();
        assertEquals(1, connection.frames.size());
        assertEquals(Integer.valueOf(1), connection.frames.get(0).rpcRequestWithResponse.requestId);
        assertEquals(0, dead.responses.size());
        assertEquals(0, dead.deaths.size());
        connection.beforeWrite = null;
        assertNextRequestCompletes(2);
    }

    private void send(Callback callback) {
        wearable.requests.send(getContext().getPackageName(), PEER, "/echo", new byte[]{1, 2, 3},
                new MessageOptions(0), callback);
    }

    private void assertNextRequestCompletes(int expectedSequence) throws Exception {
        Callback callback = new Callback();
        send(callback);
        drainNetwork();
        Request sent = connection.frames.get(connection.frames.size() - 1).rpcRequestWithResponse;
        assertNotNull(sent);
        assertEquals(Integer.valueOf(expectedSequence), sent.requestId);
        Request reply = WearableRequests.rpcResponse(WearableRequests.envelope(getContext().getPackageName(),
                PackageUtils.firstSignatureDigest(getContext(), getContext().getPackageName()),
                wearable.getLocalNodeId(), PEER, "/echo", 1, 1), WearableRequests.rpcId(sent),
                new byte[]{4, 5}).rpcRequestWithResponse;
        wearable.requests.receive(connection, PEER, reply);
        assertStatus(callback, 0, WearableRequests.rpcId(sent));
        assertEquals(2, callback.responses.get(0).data.length);
        assertEquals(0, callback.deaths.size());
    }

    private void assertStatus(Callback callback, int status, int id) throws Exception {
        assertTrue(callback.finished.await(3, TimeUnit.SECONDS));
        assertEquals(1, callback.responses.size());
        assertEquals(status, callback.responses.get(0).statusCode);
        assertEquals(id, callback.responses.get(0).requestId);
    }

    private void drainNetwork() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        assertTrue(wearable.networkHandler.post(done::countDown));
        assertTrue(done.await(3, TimeUnit.SECONDS));
    }

    private void blockNetwork() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        releaseNetwork = new CountDownLatch(1);
        assertTrue(wearable.networkHandler.post(() -> {
            entered.countDown();
            try { releaseNetwork.await(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }));
        assertTrue(entered.await(3, TimeUnit.SECONDS));
    }

    private void unblockNetwork() throws Exception {
        releaseNetwork.countDown();
        drainNetwork();
    }

    /** Controlled remote-binder death semantics, without a production-only test bypass. */
    private static final class Callback extends BaseWearableCallbacks {
        final List<IBinder.DeathRecipient> deaths = new CopyOnWriteArrayList<>();
        final List<RpcResponse> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch finished = new CountDownLatch(1);
        private volatile boolean alive = true;
        private final Binder delegate = new Binder();
        private final IBinder binder = (IBinder) Proxy.newProxyInstance(IBinder.class.getClassLoader(),
                new Class<?>[]{IBinder.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "linkToDeath":
                            if (!alive) throw new RemoteException("Synthetic dead callback");
                            deaths.add((IBinder.DeathRecipient) args[0]);
                            return null;
                        case "unlinkToDeath": return deaths.remove(args[0]);
                        case "isBinderAlive": case "pingBinder": return alive;
                        default: return method.invoke(delegate, args);
                    }
                });
        @Override public IBinder asBinder() { return binder; }
        @Override public void onRpcResponse(RpcResponse response) {
            responses.add(response);
            finished.countDown();
        }
        void die() {
            alive = false;
            for (IBinder.DeathRecipient recipient : deaths) recipient.binderDied();
            deaths.clear();
        }
    }

    private static final class CapturingConnection extends WearableConnection {
        final List<RootMessage> frames = new CopyOnWriteArrayList<>();
        volatile Runnable beforeWrite;
        CapturingConnection() { super(null); }
        @Override protected void writeMessagePiece(MessagePiece piece) throws IOException {
            Runnable hook = beforeWrite;
            if (hook != null) hook.run();
            if (piece.totalPieces != 1) throw new AssertionError("Unexpected fragmented test frame");
            frames.add(RootMessage.ADAPTER.decode(piece.data));
        }
        @Override protected MessagePiece readMessagePiece() { throw new AssertionError("No socket reads"); }
        @Override public void close() { }
    }
}
