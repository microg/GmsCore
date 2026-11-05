/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.test.AndroidTestCase;

import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.internal.*;
import com.google.android.gms.common.data.DataHolder;
import org.microg.gms.common.PackageUtils;

import org.microg.wearable.WearableConnection;
import org.microg.wearable.WearableRequests;
import org.microg.wearable.proto.ChannelControlRequest;
import org.microg.wearable.proto.ChannelRequest;
import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.MessagePiece;
import org.microg.wearable.proto.Request;
import org.microg.wearable.proto.RootMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real writes, protobuf decoding and preferences; no sockets or remote accounts. */
public class RpcSequencingTest extends AndroidTestCase {
    private NodeDatabaseHelper nodes;
    private ConfigurationDatabaseHelper configurations;
    private WearableImpl wearable;
    private SharedPreferences preferences;

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
                assertFalse(network.isAlive());
            }
        } finally {
            if (configurations != null) { configurations.close(); getContext().deleteDatabase("connectionconfig.db"); }
            if (nodes != null) { nodes.close(); getContext().deleteDatabase("node.db"); }
            if (preferences != null) assertTrue(preferences.edit().clear().commit());
            super.tearDown();
        }
    }

    public void testChannelWritesLeaveNoGapBeforeRpcResponse() throws Exception {
        String packageName = getContext().getPackageName();
        String peer = "sequence-peer";
        CapturingConnection connection = new CapturingConnection();
        ConnectionConfiguration config = configurations.putManagedConfiguration(
                new ConnectionConfiguration("sequence", "AA:BB:CC:DD:EE:01", 1, 1, true), wearable.getLocalNodeId());
        wearable.onConnectReceived(connection, config, new Connect.Builder().id(peer).build());

        wearable.sendMessage(packageName, peer, "/first", new byte[]{1});
        ChannelRequest channel = new ChannelRequest.Builder().version(1).origin(0)
                .channelControlRequest(new ChannelControlRequest.Builder().type(0).channelId(7L).build()).build();
        wearable.sendChannelRequest(connection, packageName, "synthetic-signature", peer, channel);
        wearable.sendChannelRequest(connection, packageName, "synthetic-signature", peer, channel);
        connection.writeMessage(WearableRequests.rpcResponse(wearable.newRpcEnvelope(
                packageName, "synthetic-signature", peer, "/echo"), 17000, new byte[]{2}));
        wearable.sendMessage(packageName, peer, "/last", new byte[]{3});

        assertEquals(5, connection.frames.size());
        Request first = connection.frames.get(0).rpcRequest;
        Request firstChannel = connection.frames.get(1).channelRequest;
        Request secondChannel = connection.frames.get(2).channelRequest;
        Request response = connection.frames.get(3).rpcRequestWithResponse;
        Request last = connection.frames.get(4).rpcRequest;
        assertNotNull(first); assertNotNull(firstChannel); assertNotNull(secondChannel);
        assertNotNull(response); assertNotNull(last);
        assertTrue(first.generation > 0);
        assertEquals(Integer.valueOf(0), firstChannel.generation);
        assertEquals(Integer.valueOf(0), secondChannel.generation);
        assertEquals(Integer.valueOf(1), firstChannel.requestId);
        assertEquals(Integer.valueOf(2), secondChannel.requestId);
        // The peer skips generation-zero frames and expects contiguous IDs in the ordered stream.
        assertEquals(first.generation, response.generation);
        assertEquals(first.generation, last.generation);
        assertEquals(Integer.valueOf(1), first.requestId);
        assertEquals(Integer.valueOf(2), response.requestId);
        assertEquals(Integer.valueOf(3), last.requestId);
        assertEquals(Integer.valueOf(17000), response.responseRequestId);
        assertEquals(1, response.rawData.size());
    }

    public void testUnorderedStateDoesNotPersistOrAdvanceOrderedGeneration() {
        RpcHelper helper = new RpcHelper(getContext());
        Map<String, ?> initial = new HashMap<>(preferences.getAll());
        RpcHelper.RpcConnectionState first = helper.useUnorderedConnectionState("app", "peer", "");
        RpcHelper.RpcConnectionState second = helper.useUnorderedConnectionState("app", "peer", "");
        assertEquals(0, first.generation);
        assertEquals(1, first.lastRequestId);
        assertEquals(2, second.lastRequestId);
        assertEquals(initial, preferences.getAll());
        RpcHelper.RpcConnectionState ordered = helper.useConnectionState("app", "peer", "/rpc");
        Map<String, ?> persisted = new HashMap<>(preferences.getAll());
        RpcHelper restarted = new RpcHelper(getContext());
        RpcHelper.RpcConnectionState restartedChannel = restarted.useUnorderedConnectionState("app", "peer", "");
        assertEquals(0, restartedChannel.generation);
        assertEquals(1, restartedChannel.lastRequestId);
        assertEquals(persisted, preferences.getAll());
        RpcHelper.RpcConnectionState next = restarted.useConnectionState("app", "peer", "/rpc");
        assertEquals(ordered.generation + 1, next.generation);
        assertEquals(1, next.lastRequestId);
    }

    public void testUnorderedSequencesAreIndependentAcrossPeersAndFromRpc() {
        RpcHelper helper = new RpcHelper(getContext());
        assertEquals(1, helper.useUnorderedConnectionState("app", "first", "").lastRequestId);
        assertEquals(1, helper.useUnorderedConnectionState("app", "second", "").lastRequestId);
        assertEquals(2, helper.useUnorderedConnectionState("other.app", "first", "").lastRequestId);
        assertEquals(1, helper.useConnectionState("app", "first", "/rpc").lastRequestId);
        assertEquals(1, helper.useConnectionState("app", "second", "/rpc").lastRequestId);
        assertEquals(3, helper.useUnorderedConnectionState("app", "first", "").lastRequestId);
        assertEquals(2, helper.useConnectionState("other.app", "first", "/rpc").lastRequestId);
    }

    public void testAlternatingPrioritiesHaveContiguousFramesAndUnambiguousReplies() throws Exception {
        String peer = "priority-peer";
        String app = getContext().getPackageName();
        CapturingConnection connection = connect(peer);
        Completion[] callbacks = {new Completion(), new Completion(), new Completion(), new Completion()};
        int[] priorities = {1, 0, 1, 0};
        for (int i = 0; i < priorities.length; i++) {
            wearable.requests.send(app, peer, "/same-path", new byte[]{(byte) i},
                    new MessageOptions(priorities[i]), callbacks[i]);
        }
        drainNetwork();
        assertEquals(4, connection.frames.size());
        Request[] requests = new Request[4];
        for (int i = 0; i < requests.length; i++) {
            requests[i] = connection.frames.get(i).rpcRequestWithResponse;
            assertNotNull(requests[i]);
            assertEquals(Integer.valueOf(priorities[i] + 1), requests[i].priority);
            assertEquals(Integer.valueOf(i / 2 + 1), requests[i].requestId);
            assertEquals(requests[i % 2].generation, requests[i].generation);
        }
        // Fresh hi and lo streams both start at generation 2; ID alone is deliberately ambiguous.
        assertEquals(WearableRequests.rpcId(requests[0]), WearableRequests.rpcId(requests[1]));
        String signature = PackageUtils.firstSignatureDigest(getContext(), app);
        for (int i = requests.length - 1; i >= 0; i--) {
            Request reply = WearableRequests.rpcResponse(WearableRequests.envelope(app, signature,
                    wearable.getLocalNodeId(), peer, "/same-path", 3, 5 - i),
                    WearableRequests.rpcId(requests[i]), new byte[]{(byte) (40 + i)}, priorities[i])
                    .rpcRequestWithResponse;
            wearable.requests.receive(connection, peer, reply);
            RpcResponse response = callbacks[i].responses.poll(3, TimeUnit.SECONDS);
            assertNotNull(response);
            assertEquals(0, response.statusCode);
            assertEquals(40 + i, response.data[0]);
            for (int earlier = 0; earlier < i; earlier++) assertTrue(callbacks[earlier].responses.isEmpty());
        }
    }

    public void testIncomingSameIdPrioritiesBothReplyInTheirOwnStream() throws Exception {
        String peer = "incoming-priority-peer";
        String app = getContext().getPackageName();
        String signature = PackageUtils.firstSignatureDigest(getContext(), app);
        CapturingConnection connection = connect(peer);
        BlockingQueue<IRpcResponseCallback> replies = new LinkedBlockingQueue<>();
        wearable.addListener(app, new RequestListener(replies), new IntentFilter[0]);
        Request high = WearableRequests.rpcRequest(WearableRequests.envelope(app, signature,
                wearable.getLocalNodeId(), peer, "/same-path", 4, 7), new byte[]{1}, 1).rpcRequestWithResponse;
        Request normal = WearableRequests.rpcRequest(WearableRequests.envelope(app, signature,
                wearable.getLocalNodeId(), peer, "/same-path", 4, 7), new byte[]{0}, 0).rpcRequestWithResponse;
        wearable.requests.receive(connection, peer, high);
        IRpcResponseCallback highReply = replies.poll(3, TimeUnit.SECONDS);
        assertNotNull(highReply);
        wearable.requests.receive(connection, peer, normal);
        IRpcResponseCallback normalReply = replies.poll(3, TimeUnit.SECONDS);
        assertNotNull(normalReply);
        highReply.onResponse(true, new byte[]{11});
        normalReply.onResponse(true, new byte[]{22});
        drainNetwork();
        assertEquals(2, connection.frames.size());
        Request first = connection.frames.get(0).rpcRequestWithResponse;
        Request second = connection.frames.get(1).rpcRequestWithResponse;
        assertEquals(Integer.valueOf(2), first.priority);
        assertEquals(Integer.valueOf(1), second.priority);
        assertEquals(Integer.valueOf(1), first.requestId);
        assertEquals(Integer.valueOf(1), second.requestId);
        assertEquals(Integer.valueOf(WearableRequests.rpcId(high)), first.responseRequestId);
        assertEquals(first.responseRequestId, second.responseRequestId);
        assertEquals(11, first.rawData.getByte(0));
        assertEquals(22, second.rawData.getByte(0));
    }

    public void testLegacyGenerationsAreAdvancedWhenSeparatingPriorityStreams() {
        assertTrue(preferences.edit().putInt("peer:lo", 8).putInt("peer:hi", 5).commit());
        RpcHelper helper = new RpcHelper(getContext());
        RpcHelper.RpcConnectionState high = helper.useConnectionState("app", "peer", "/rpc", 1);
        assertEquals(9, high.generation);
        assertEquals(1, high.lastRequestId);
        assertEquals(2, helper.useConnectionState("app", "peer", "/rpc", 1).lastRequestId);
        RpcHelper.RpcConnectionState voice = helper.useConnectionState("com.google.android.wearable.app",
                "peer", "/s3/request", 0);
        assertTrue(voice.generation > 5);
        assertEquals(1, voice.lastRequestId);
        assertEquals(1, helper.useConnectionState("app", "peer", "/rpc").lastRequestId);
    }

    public void testVoiceNormalAndHighPrioritySequencesRemainSeparate() {
        RpcHelper helper = new RpcHelper(getContext());
        assertEquals(1, helper.useConnectionState("app", "peer", "/rpc", 1).lastRequestId);
        assertEquals(1, helper.useConnectionState("com.google.android.wearable.app", "peer", "/s3/request").lastRequestId);
        assertEquals(1, helper.useConnectionState("app", "peer", "/rpc").lastRequestId);
        // Voice is selected by package/path regardless of the optional priority.
        assertEquals(2, helper.useConnectionState("com.google.android.wearable.app", "peer", "/s3/request", 1).lastRequestId);
        assertEquals(2, helper.useConnectionState("app", "peer", "/rpc", 1).lastRequestId);
        assertEquals(2, helper.useConnectionState("other.app", "peer", "/rpc").lastRequestId);
    }

    private CapturingConnection connect(String peer) throws Exception {
        CapturingConnection connection = new CapturingConnection();
        ConnectionConfiguration config = configurations.putManagedConfiguration(
                new ConnectionConfiguration("priority", "AA:BB:CC:DD:EE:02", 1, 1, true), wearable.getLocalNodeId());
        wearable.onConnectReceived(connection, config, new Connect.Builder().id(peer).build());
        drainNetwork();
        return connection;
    }

    private void drainNetwork() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        assertTrue(wearable.networkHandler.post(done::countDown));
        assertTrue(done.await(3, TimeUnit.SECONDS));
    }

    private static final class Completion extends BaseWearableCallbacks {
        final BlockingQueue<RpcResponse> responses = new LinkedBlockingQueue<>();
        @Override public void onRpcResponse(RpcResponse response) { responses.add(response); }
    }

    private static final class RequestListener extends IWearableListener.Stub {
        final BlockingQueue<IRpcResponseCallback> replies;
        RequestListener(BlockingQueue<IRpcResponseCallback> replies) { this.replies = replies; }
        @Override public void onRequestReceived(MessageEventParcelable event, IRpcResponseCallback callback) {
            replies.add(callback);
        }
        @Override public void onDataChanged(DataHolder data) { data.close(); }
        @Override public void onMessageReceived(MessageEventParcelable event) { }
        @Override public void onPeerConnected(NodeParcelable node) { }
        @Override public void onPeerDisconnected(NodeParcelable node) { }
        @Override public void onConnectedNodes(List<NodeParcelable> nodes) { }
        @Override public void onConnectedCapabilityChanged(CapabilityInfoParcelable info) { }
        @Override public void onNotificationReceived(AncsNotificationParcelable notification) { }
        @Override public void onChannelEvent(ChannelEventParcelable event) { }
        @Override public void onEntityUpdate(AmsEntityUpdateParcelable event) { }
    }

    private static final class CapturingConnection extends WearableConnection {
        final List<RootMessage> frames = new CopyOnWriteArrayList<>();
        CapturingConnection() { super(null); }
        @Override protected void writeMessagePiece(MessagePiece piece) throws IOException {
            if (piece.totalPieces != 1) throw new AssertionError("Unexpected fragmented test frame");
            frames.add(RootMessage.ADAPTER.decode(piece.data));
        }
        @Override protected MessagePiece readMessagePiece() { throw new AssertionError("No socket reads"); }
        @Override public void close() { }
    }
}
