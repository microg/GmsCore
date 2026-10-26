package org.microg.gms.wearable;

import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.wearable.MessageOptions;

import org.microg.gms.wearable.proto.ChannelRequest;
import org.microg.gms.wearable.proto.Request;
import org.microg.gms.wearable.proto.RootMessage;

import java.util.UUID;

import okio.ByteString;

public class RpcMessageTransport {
    private static final String TAG = "RpcTransport";

    public static final int PROTO_PRIO_UNSPECIFIED = 0;
    public static final int PROTO_PRIO_NORMAL = 1;
    public static final int PROTO_PRIO_LOW = 2;

    public static int API_PRIO_LOW = 1;

    private static final String WEARABLE_APP_PACKAGE = "com.google.android.wearable.app";

    public interface Host {
        String getLocalNodeId();

        WearableWriter getWriter(String nodeId);

        void onWriteFailed(String nodeId);
    }

    private final RpcHelper rpcHelper;
    private Host host;

    public RpcMessageTransport(RpcHelper rpcHelper, Host host) {
        this.host = host;
        this.rpcHelper = rpcHelper;
    }

    public static int toProtoPriority(MessageOptions options) {
        return options != null && options.priority == API_PRIO_LOW
                ? PROTO_PRIO_LOW : PROTO_PRIO_NORMAL;
    }

    public static MessageOptions toMessageOptions(Request request) {
        int p = request.priority != null ? request.priority : PROTO_PRIO_UNSPECIFIED;
        return new MessageOptions(p == PROTO_PRIO_LOW ? API_PRIO_LOW : 0);
    }
    public static int writerQueuePriority(Request request) {
        if (isS3ProxyPath(request.packageName, request.path)) return 8;
        if (toMessageOptions(request).priority == API_PRIO_LOW) return 10;
        if (request.request != null) return 9;
        return 4;
    }

    public static boolean isS3ProxyPath(String packageName, String path) {
        return WEARABLE_APP_PACKAGE.equals(packageName) && path != null && path.startsWith("/s3");
    }

    public Request buildRequest(String packageName, String signatureDigest, String targetNodeId,
                                String path, byte[] data, ChannelRequest channel,
                                boolean requiresResponse, Integer senderRequestId,
                                MessageOptions options) {
        if (data != null && channel != null) {
            throw new IllegalArgumentException("can't set data and channel");
        }
        RpcHelper.RpcConnectionState state =
                rpcHelper.useConnectionState(packageName, targetNodeId, path);

        Request.Builder b = new Request.Builder()
                .requestId(state.lastRequestId)
                .generation(state.generation)
                .packageName(packageName)
                .signatureDigest(signatureDigest)
                .targetNodeId(targetNodeId)
                .sourceNodeId(host.getLocalNodeId())
                .path(path)
                .unknown5(0)
                .priority(toProtoPriority(options));

        if (requiresResponse) b.requiresResponse(true);
        if (senderRequestId != null) b.senderRequestId(senderRequestId);

        if (channel != null) {
            b.request(channel);
        } else {
            b.rawData(data != null ? ByteString.of(data) : ByteString.EMPTY);
        }
        return b.build();
    }

    public static RootMessage wrap(Request request) {
        RootMessage.Builder b = new RootMessage.Builder();
        if (Boolean.TRUE.equals(request.requiresResponse) || request.senderRequestId != null) {
            b.rpcServiceRequest(request);
        } else if (request.request != null) {
            b.channelRequest(request);
        } else {
            b.rpcRequest(request);
        }
        return b.build();
    }

    public int send(String nodeId, Request request) {
        if (TextUtils.isEmpty(nodeId)) {
            Log.w(TAG, "send: no target node for " + request.path);
            return -1;
        }
        if (nodeId.equals(host.getLocalNodeId())) {
            Log.w(TAG, "send: target is the local node, refusing " + request.path);
            return -1;
        }
        WearableWriter writer = host.getWriter(nodeId);
        if (writer == null || !writer.enqueue(wrap(request))) {
            Log.w(TAG, "send: no writer available for " + nodeId + " path=" + request.path);
            host.onWriteFailed(nodeId);
            return -1;
        }
        return idOf(request);
    }

    public int sendMessage(String packageName, String signatureDigest, String nodeId,
                           String path, byte[] data, MessageOptions options) {
        return send(nodeId, buildRequest(packageName, signatureDigest, nodeId, path, data,
                null, false, null, options));
    }

    public int sendRequest(String packageName, String signatureDigest, String nodeId,
                           String path, byte[] data, MessageOptions options) {
        return send(nodeId, buildRequest(packageName, signatureDigest, nodeId, path, data,
                null, true, null, options));
    }

    public int sendResponse(String packageName, String signatureDigest, String peerNodeId,
                            String path, byte[] data, int senderRequestId) {
        return send(peerNodeId, buildRequest(packageName, signatureDigest, peerNodeId, path,
                data, null, false, senderRequestId, null));
    }

    private static int idOf(Request r) {
        return RpcHelper.combineId(r.generation != null ? r.generation : 0,
                r.requestId != null ? r.requestId : 0);
    }

    public static Request extractRpc(RootMessage message) {
        if (message.rpcRequest != null) return message.rpcRequest;
        if (message.channelRequest != null) return message.channelRequest;
        if (message.rpcServiceRequest != null) return message.rpcServiceRequest;
        return null;
    }

    public Request normalizeInbound(String lastHopNodeId, Request request) {
        Request.Builder b = null;
        if (TextUtils.isEmpty(request.sourceNodeId)) {
            b = request.newBuilder().sourceNodeId(lastHopNodeId);
        }
        if (isNodeIdMadeUp(request.targetNodeId)) {
            if (b == null) b = request.newBuilder();
            b.targetNodeId(host.getLocalNodeId());
        }
        return b == null ? request : b.build();
    }

    public boolean isForLocalNode(Request request) {
        return TextUtils.isEmpty(request.targetNodeId)
                || request.targetNodeId.equals(host.getLocalNodeId());
    }

    public static boolean isNodeIdMadeUp(String nodeId) {
        if (TextUtils.isEmpty(nodeId)) return true;
        try {
            UUID.fromString(nodeId);
            return false;
        } catch (IllegalArgumentException ignored) {
            try {
                Long.parseLong(nodeId, 16);
                return false;
            } catch (NumberFormatException ignored2) {
                Log.v(TAG, "isNodeIdMadeUp: " + nodeId + " is neither uuid nor hex, assuming made up");
                return true;
            }
        }
    }
}
