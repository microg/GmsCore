/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.microg.wearable.proto.Request;
import org.microg.wearable.proto.RootMessage;
import okio.ByteString;

/** Fields 1 through 6 are required by current peers, including the zero-valued routing field. */
public final class WearableRequests {
    private WearableRequests() { }

    public static Request.Builder envelope(String packageName, String signature, String target,
                                           String source, String path, int generation, int requestId) {
        if (packageName == null || signature == null || target == null || source == null || path == null) {
            throw new IllegalArgumentException("Incomplete wearable request");
        }
        return new Request.Builder().requestId(requestId).packageName(packageName).signatureDigest(signature)
                .targetNodeId(target).unknown5(0).path(path).sourceNodeId(source).generation(generation);
    }

    public static int rpcId(Request request) {
        if (request.generation == null || request.requestId == null) {
            throw new IllegalArgumentException("Missing RPC sequence");
        }
        return (request.generation + 527) * 31 + request.requestId;
    }

    public static RootMessage rpcRequest(Request.Builder envelope, byte[] data, int priority) {
        if (priority < 0 || priority > 1 || data == null || data.length > PendingRpcRequests.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Invalid RPC payload or priority");
        }
        return new RootMessage.Builder().rpcRequestWithResponse(envelope.requiresResponse(true)
                .priority(priority + 1).rawData(ByteString.of(data)).build()).build();
    }

    public static RootMessage rpcResponse(Request.Builder envelope, int requestId, byte[] data) {
        return rpcResponse(envelope, requestId, data, 0);
    }

    public static RootMessage rpcResponse(Request.Builder envelope, int requestId, byte[] data, int priority) {
        if (priority < 0 || priority > 1) throw new IllegalArgumentException("Invalid RPC priority");
        if (data != null && data.length > PendingRpcRequests.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("RPC response too large");
        }
        return new RootMessage.Builder().rpcRequestWithResponse(envelope.responseRequestId(requestId)
                .priority(priority + 1).rawData(data == null ? null : ByteString.of(data)).build()).build();
    }

    /** The peer treats absent, unspecified and unknown priority values as normal priority. */
    public static int rpcPriority(Request request) {
        return Integer.valueOf(2).equals(request.priority) ? 1 : 0;
    }

    public static boolean validRpcEnvelope(Request request, String localNode, String peerNode) {
        return localNode != null && peerNode != null && peerNode.equals(request.sourceNodeId)
                && localNode.equals(request.targetNodeId) && validRpcPath(request.path)
                && request.requestId != null && request.generation != null && request.request == null
                && request.packageName != null && !request.packageName.isEmpty() && request.packageName.length() <= 256
                && request.signatureDigest != null && !request.signatureDigest.isEmpty() && request.signatureDigest.length() <= 256
                && (request.rawData == null || request.rawData.size() <= PendingRpcRequests.MAX_PAYLOAD_BYTES)
                && !(request.responseRequestId != null && Boolean.TRUE.equals(request.requiresResponse));
    }

    public static boolean validRpcPath(String path) {
        return path != null && path.startsWith("/") && path.length() <= 4096;
    }
}
