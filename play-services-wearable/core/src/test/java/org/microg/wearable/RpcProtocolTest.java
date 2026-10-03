/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.junit.Test;
import org.microg.wearable.proto.*;
import okio.ByteString;
import static org.junit.Assert.*;

public class RpcProtocolTest {
    private Request.Builder envelope() {
        return WearableRequests.envelope("app", "signature", "watch", "phone", "/rpc", 2, 7);
    }

    @Test public void requestsUseRoot18AndDistinctPriorityWireValues() throws Exception {
        RootMessage root = WearableRequests.rpcRequest(envelope(), new byte[]{42}, 1);
        byte[] encoded = RootMessage.ADAPTER.encode(root);
        assertEquals(0x92, encoded[0] & 255);
        assertEquals(0x01, encoded[1] & 255);
        RootMessage decoded = RootMessage.ADAPTER.decode(encoded);
        assertNull(decoded.rpcRequest);
        assertTrue(decoded.rpcRequestWithResponse.requiresResponse);
        assertEquals(Integer.valueOf(2), decoded.rpcRequestWithResponse.priority);
        assertEquals(Integer.valueOf(1), WearableRequests.rpcRequest(envelope(), new byte[0], 0).rpcRequestWithResponse.priority);
    }

    @Test public void wireFlagsAndCorrelationHaveIndependentFieldNumbers() throws Exception {
        // Fields 11 (request), 12 (response correlation) and 13 (priority), from the peer wire schema.
        Request request = Request.ADAPTER.decode(ByteString.decodeHex("58016802"));
        assertTrue(request.requiresResponse);
        assertEquals(Integer.valueOf(2), request.priority);
        assertNull(request.responseRequestId);
        Request reply = Request.ADAPTER.decode(ByteString.decodeHex("60908001"));
        assertEquals(Integer.valueOf(16400), reply.responseRequestId);
        assertNull(reply.requiresResponse);
    }

    @Test public void replyCorrelatesCombinedOriginalIdNotItsNewEnvelopeSequence() {
        Request original = envelope().build();
        int id = WearableRequests.rpcId(original);
        assertEquals(16406, id);
        RootMessage reply = WearableRequests.rpcResponse(WearableRequests.envelope(
                "app", "signature", "phone", "watch", "/rpc", 8, 100), id, new byte[]{9});
        assertEquals(Integer.valueOf(16406), reply.rpcRequestWithResponse.responseRequestId);
        assertEquals(Integer.valueOf(100), reply.rpcRequestWithResponse.requestId);
        assertNotEquals(id, WearableRequests.rpcId(reply.rpcRequestWithResponse));
        assertNull(reply.rpcRequestWithResponse.requiresResponse);
    }

    @Test public void emptySuccessfulResponseDiffersFromMissingFailedResponse() throws Exception {
        RootMessage empty = WearableRequests.rpcResponse(envelope(), 1, new byte[0]);
        RootMessage failure = WearableRequests.rpcResponse(envelope(), 1, null);
        assertEquals(ByteString.EMPTY, RootMessage.ADAPTER.decode(RootMessage.ADAPTER.encode(empty)).rpcRequestWithResponse.rawData);
        assertNull(RootMessage.ADAPTER.decode(RootMessage.ADAPTER.encode(failure)).rpcRequestWithResponse.rawData);
    }

    @Test public void rejectsForgedPeersInvalidSequencesAndAmbiguousReplies() {
        Request request = envelope().requiresResponse(true).build();
        assertTrue(WearableRequests.validRpcEnvelope(request, "watch", "phone"));
        assertFalse(WearableRequests.validRpcEnvelope(request, "watch", "other"));
        assertFalse(WearableRequests.validRpcEnvelope(request, "other", "phone"));
        assertFalse(WearableRequests.validRpcEnvelope(request.newBuilder().generation(null).build(), "watch", "phone"));
        assertFalse(WearableRequests.validRpcEnvelope(request.newBuilder().responseRequestId(3).build(), "watch", "phone"));
        assertFalse(WearableRequests.validRpcEnvelope(request.newBuilder().signatureDigest("").build(), "watch", "phone"));
        assertFalse(WearableRequests.validRpcEnvelope(request.newBuilder().path("relative").build(), "watch", "phone"));
        assertFalse(WearableRequests.validRpcEnvelope(request.newBuilder().request(new ChannelRequest.Builder().build()).build(), "watch", "phone"));
    }

    @Test public void boundsRequestsResponsesAndPriority() {
        byte[] tooLarge = new byte[PendingRpcRequests.MAX_PAYLOAD_BYTES + 1];
        assertThrows(IllegalArgumentException.class, () -> WearableRequests.rpcRequest(envelope(), tooLarge, 0));
        assertThrows(IllegalArgumentException.class, () -> WearableRequests.rpcResponse(envelope(), 1, tooLarge));
        assertThrows(IllegalArgumentException.class, () -> WearableRequests.rpcRequest(envelope(), new byte[0], 2));
        assertThrows(IllegalArgumentException.class, () -> WearableRequests.rpcId(new Request.Builder().requestId(0).build()));
        assertFalse(WearableRequests.validRpcEnvelope(envelope().rawData(ByteString.of(tooLarge)).build(), "watch", "phone"));
    }

    @Test public void dispatchesRepliesOnlyToRpcHandler() {
        final int[] counts = new int[2];
        MessageListener listener = new MessageListener() {
            @Override public void onRpcRequest(Request request) { counts[0]++; }
            @Override public void onRpcRequestWithResponse(Request request) { counts[1]++; }
            @Override public void onSetAsset(SetAsset value) { }
            @Override public void onAckAsset(AckAsset value) { }
            @Override public void onFetchAsset(FetchAsset value) { }
            @Override public void onConnect(Connect value) { }
            @Override public void onSyncStart(SyncStart value) { }
            @Override public void onSetDataItem(SetDataItem value) { }
            @Override public void onHeartbeat(Heartbeat value) { }
            @Override public void onFilePiece(FilePiece value) { }
            @Override public void onChannelRequest(Request value) { }
        };
        listener.onMessage(null, WearableRequests.rpcResponse(envelope(), 6, new byte[]{1}));
        assertArrayEquals(new int[]{0, 1}, counts);
        listener.onMessage(null, new RootMessage.Builder().rpcRequest(envelope().build()).build());
        assertArrayEquals(new int[]{1, 1}, counts);
    }

    @Test public void admissionRemainsOccupiedUntilCompletionReturns() {
        RpcRequestBudget budget = new RpcRequestBudget();
        RpcRequestBudget.Lease[] leases = new RpcRequestBudget.Lease[8];
        for (int i = 0; i < leases.length; i++) leases[i] = budget.acquire("app");
        assertNull(budget.acquire("app"));
        assertNotNull(budget.acquire("other"));
        leases[0].release();
        leases[0].release();
        assertNotNull(budget.acquire("app"));
        assertNull(budget.acquire("app"));
    }

    @Test public void admissionAlsoBoundsDifferentApplications() {
        RpcRequestBudget budget = new RpcRequestBudget();
        for (int i = 0; i < 32; i++) assertNotNull(budget.acquire("app" + i));
        assertNull(budget.acquire("app32"));
    }
    @Test public void responsePreservesHighPriorityOnIndependentWireField() throws Exception {
        Request reply = RootMessage.ADAPTER.decode(RootMessage.ADAPTER.encode(
                WearableRequests.rpcResponse(envelope(), 16406, new byte[]{9}, 1))).rpcRequestWithResponse;
        // Independently decode the protobuf priority field: tag 13, enum value 2 means high.
        Request priorityField = Request.ADAPTER.decode(ByteString.decodeHex("6802"));
        assertEquals(priorityField.priority, reply.priority);
        assertEquals(Integer.valueOf(16406), reply.responseRequestId);
        assertEquals(1, WearableRequests.rpcPriority(reply));
        assertThrows(IllegalArgumentException.class,
                () -> WearableRequests.rpcResponse(envelope(), 1, new byte[0], 2));
    }

    @Test public void unspecifiedAndUnknownWirePrioritiesUseNormalStream() {
        for (Integer priority : new Integer[]{null, 0, 1, 3, -1}) {
            assertEquals(0, WearableRequests.rpcPriority(envelope().priority(priority).build()));
        }
        assertEquals(1, WearableRequests.rpcPriority(envelope().priority(2).build()));
        assertEquals(Integer.valueOf(1), WearableRequests.rpcResponse(envelope(), 1, new byte[0])
                .rpcRequestWithResponse.priority);
    }

}
