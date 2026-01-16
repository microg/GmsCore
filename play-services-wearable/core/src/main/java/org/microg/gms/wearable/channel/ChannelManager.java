package org.microg.gms.wearable.channel;

import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.wearable.internal.ChannelReceiveFileResponse;
import com.google.android.gms.wearable.internal.ChannelSendFileResponse;
import com.google.android.gms.wearable.internal.GetChannelInputStreamResponse;
import com.google.android.gms.wearable.internal.GetChannelOutputStreamResponse;
import com.google.android.gms.wearable.internal.IWearableCallbacks;

import org.microg.gms.wearable.WearableConnection;
import org.microg.gms.wearable.WearableImpl;
import org.microg.gms.wearable.proto.AppKey;
import org.microg.gms.wearable.proto.ChannelControlRequest;
import org.microg.gms.wearable.proto.ChannelDataAckRequest;
import org.microg.gms.wearable.proto.ChannelDataHeader;
import org.microg.gms.wearable.proto.ChannelDataRequest;
import org.microg.gms.wearable.proto.ChannelRequest;
import org.microg.gms.wearable.proto.Request;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okio.ByteString;

public class ChannelManager {
    private static final String TAG = "ChannelManager";

    public static final int CHANNEL_CONTROL_TYPE_OPEN = 1;
    public static final int CHANNEL_CONTROL_TYPE_OPEN_ACK = 2;
    public static final int CHANNEL_CONTROL_TYPE_CLOSE = 3;

    private final Handler handler;
    private final WearableImpl wearable;
    private final String localNodeId;
    private final Random random;

    private final Object lock = new Object();
    private final Map<String, ChannelStateMachine> channels = new ConcurrentHashMap<>();
    private final Map<Long, String> channelIdToToken = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final AtomicInteger requestIdCounter = new AtomicInteger(1);
    private final AtomicInteger generationCounter = new AtomicInteger(1);

    private ChannelCallbacks channelCallbacks;

    private volatile long cooldownUntil = 0;

    public ChannelManager(Handler handler, WearableImpl wearable, String localNodeId) {
        this.handler = handler;
        this.wearable = wearable;
        this.localNodeId = localNodeId;
        this.random = new Random();
    }

    public void setOperationCooldown(long durationMs) {
        cooldownUntil = System.currentTimeMillis() + durationMs;
        Log.d(TAG, "Operation cooldown set for " + durationMs + "ms");
    }

    private boolean isInCooldown() {
        long now = System.currentTimeMillis();
        if (now < cooldownUntil) {
            long remaining = cooldownUntil - now;
            Log.d(TAG, "In cooldown period, " + remaining + "ms remaining");
            return true;
        }
        return false;
    }

    public void start() {
        isRunning.set(true);
        Log.d(TAG, "ChannelManager started, localNodeId=" + localNodeId);
    }

    public void stop() {
        isRunning.set(false);
        synchronized (lock) {
            for (ChannelStateMachine channel : channels.values()) {
                try {
                    channel.clearOpenCallback();
                    channel.close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing channel on stop", e);
                }
            }
            channels.clear();
            channelIdToToken.clear();
        }
        Log.d(TAG, "ChannelManager stopped");
    }

    public void setChannelCallbacks(ChannelCallbacks callbacks) {
        this.channelCallbacks = callbacks;
    }

    public void openChannel(AppKey appKey, String nodeId, String path, OpenChannelCallback callback) {
        Log.d(TAG, String.format("openChannel(%s, %s, %s)", appKey.packageName, nodeId, path));

        if (!isRunning.get()) {
            Log.w(TAG, "openChannel called while not running");
            callback.onResult(ChannelStatusCodes.INTERNAL_ERROR, null, path);
            return;
        }

        if (isInCooldown()) {
            long delay = cooldownUntil - System.currentTimeMillis() + 100;
            Log.d(TAG, "Deferring channel open by " + delay + "ms due to cooldown");

            handler.postDelayed(() -> doOpenChannel(appKey, nodeId, path, callback), delay);
            return;
        }

        handler.post(() -> doOpenChannel(appKey, nodeId, path, callback));
    }

    private void doOpenChannel(AppKey appKey, String nodeId, String path, OpenChannelCallback callback) {
        if (isInCooldown()) {
            long delay = cooldownUntil - System.currentTimeMillis() + 100;
            Log.d(TAG, "Cooldown detected in doOpenChannel, deferring by " + delay + "ms");
            handler.postDelayed(() -> doOpenChannel(appKey, nodeId, path, callback), delay);
            return;
        }

        try {
            WearableConnection connection = wearable.getActiveConnections().get(nodeId);
            if (connection == null) {
                Log.w(TAG, "Target node not connected: " + nodeId);
                callback.onResult(ChannelStatusCodes.CHANNEL_NOT_CONNECTED, null, path);
                return;
            }

            long channelId = generateChannelId();

            ChannelToken token = new ChannelToken(nodeId, appKey, channelId, true);
            String tokenString = token.toTokenString();

            IBinder.DeathRecipient deathRecipient = () -> onBinderDied(token);

            ChannelStateMachine channel = new ChannelStateMachine(
                    token, this, channelCallbacks, true, deathRecipient
            );
            channel.setPath(path);
            channel.setOpenCallback(callback);

            synchronized (lock) {
                channels.put(tokenString, channel);
                channelIdToToken.put(channelId, tokenString);
            }

            channel.setConnectionState(ChannelStateMachine.CONNECTION_STATE_OPEN_SENT);

            ChannelControlRequest controlRequest = new ChannelControlRequest.Builder()
                    .type(CHANNEL_CONTROL_TYPE_OPEN)
                    .channelId(channelId)
                    .fromChannelOperator(true)
                    .packageName(appKey.packageName)
                    .signatureDigest(appKey.signatureDigest)
                    .path(path)
                    .build();

            ChannelRequest channelRequest = new ChannelRequest.Builder()
                    .channelControlRequest(controlRequest)
                    .version(1)
                    .origin(0)
                    .build();
            Log.d(TAG, "ChannelRequest: " + channelRequest);
            int requestId = requestIdCounter.getAndIncrement();
            int generation = generationCounter.get();

            Request request = new Request.Builder()
                    .targetNodeId(nodeId)
                    .sourceNodeId(localNodeId)
                    .packageName(appKey.packageName)
                    .signatureDigest(appKey.signatureDigest)
                    .path(path)
                    .request(channelRequest)
                    .requestId(requestId)
                    .generation(generation)
                    .build();

            RootMessage message = new RootMessage.Builder()
                    .channelRequest(request)
                    .build();
            Log.d(TAG, "RootMessage: " + message);

            try {
                connection.writeMessage(message);
                Log.d(TAG, "Sent open channel request: " + channel);
            } catch (IOException e) {
                Log.e(TAG, "Failed to send channel open request", e);
                synchronized (lock) {
                    channels.remove(tokenString);
                    channelIdToToken.remove(channelId);
                }
                callback.onResult(ChannelStatusCodes.CHANNEL_NOT_CONNECTED, null, path);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to open channel", e);
            callback.onResult(ChannelStatusCodes.INTERNAL_ERROR, null, path);
        }
    }

    private long generateChannelId() {
        return System.currentTimeMillis() ^ (random.nextLong() & 0xFFFFFFFFL);
    }

    public ChannelStateMachine getChannel(String tokenString) {
        synchronized (lock) {
            return channels.get(tokenString);
        }
    }

    public ChannelStateMachine getChannel(ChannelToken token) {
        return getChannel(token.toTokenString());
    }

    public void closeChannel(ChannelToken token, int errorCode) {
        ChannelStateMachine channel = getChannel(token);
        if (channel == null) {
            Log.w(TAG, "closeChannel: channel not found");
            return;
        }

        handler.post(() -> doCloseChannel(channel, errorCode));
    }

    private void doCloseChannel(ChannelStateMachine channel, int errorCode) {
        try {
            WearableConnection connection = wearable.getActiveConnections().get(channel.token.nodeId);
            if (connection != null) {
                ChannelControlRequest controlRequest = new ChannelControlRequest.Builder()
                        .type(CHANNEL_CONTROL_TYPE_CLOSE)
                        .channelId(channel.token.channelId)
                        .fromChannelOperator(channel.token.thisNodeWasOpener)
                        .packageName(channel.token.appKey.packageName)
                        .signatureDigest(channel.token.appKey.signatureDigest)
                        .closeErrorCode(errorCode)
                        .build();

                ChannelRequest channelRequest = new ChannelRequest.Builder()
                        .channelControlRequest(controlRequest)
                        .version(1)
                        .origin(0)
                        .build();

                int requestId = requestIdCounter.getAndIncrement();

                Request request = new Request.Builder()
                        .requestId(requestId)
                        .packageName(channel.token.appKey.packageName)
                        .signatureDigest(channel.token.appKey.signatureDigest)
                        .targetNodeId(channel.token.nodeId)
                        .sourceNodeId(localNodeId)
                        .request(channelRequest)
                        .generation(generationCounter.get())
                        .build();

                try {
                    connection.writeMessage(new RootMessage.Builder()
                            .channelRequest(request)
                            .build());
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send close request", e);
                }
            }

            channel.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing channel", e);
        } finally {
            synchronized (lock) {
                channels.remove(channel.token.toTokenString());
                channelIdToToken.remove(channel.token.channelId);
            }
        }
    }

    public void onChannelRequestReceived(WearableConnection connection, String sourceNodeId, Request request) {
        if (request.request == null) {
            Log.w(TAG, "Received channel request with null ChannelRequest");
            return;
        }

        ChannelRequest channelRequest = request.request;

        if (channelRequest.channelControlRequest != null) {
            onChannelControlReceived(connection, sourceNodeId, request, channelRequest.channelControlRequest);
        } else if (channelRequest.channelDataRequest != null) {
            onChannelDataReceived(channelRequest.channelDataRequest);
        } else if (channelRequest.channelDataAckRequest != null) {
            onChannelDataAckReceived(channelRequest.channelDataAckRequest);
        }
    }


    private void onChannelControlReceived(WearableConnection connection, String sourceNodeId, Request request, ChannelControlRequest control) {
        int type = control.type;
        Log.d(TAG, "onChannelControlReceived: type=" + type + ", channelId=" + control.channelId);

        switch (type) {
            case CHANNEL_CONTROL_TYPE_OPEN:
                onChannelOpenReceived(connection, sourceNodeId, request, control);
                break;
            case CHANNEL_CONTROL_TYPE_OPEN_ACK:
                onChannelOpenAckReceived(control);
                break;
            case CHANNEL_CONTROL_TYPE_CLOSE:
                onChannelCloseReceived(control);
                break;
            default:
                Log.w(TAG, "Unknown channel control type: " + type);
        }
    }

    private void onChannelOpenReceived(WearableConnection connection, String sourceNodeId, Request request, ChannelControlRequest control) {
        Log.d(TAG, "onChannelOpenReceived: channelId=" + control.channelId +
                ", path=" + control.path + ", from=" + sourceNodeId);

        handler.post(() -> {
            try {
                AppKey appKey = new AppKey(control.packageName, control.signatureDigest);

                ChannelToken token = new ChannelToken(
                        sourceNodeId, appKey, control.channelId, false
                );
                String tokenString = token.toTokenString();

                IBinder.DeathRecipient deathRecipient = () -> onBinderDied(token);

                ChannelStateMachine channel = new ChannelStateMachine(
                        token, this, channelCallbacks, false, deathRecipient
                );
                channel.setPath(control.path);
                channel.setConnectionState(ChannelStateMachine.CONNECTION_STATE_ESTABLISHED);

                synchronized (lock) {
                    channels.put(tokenString, channel);
                    channelIdToToken.put(control.channelId, tokenString);
                }

                sendOpenAck(connection, sourceNodeId, control, appKey);

                Log.d(TAG, "Channel opened by remote: " + channel);

                if (channelCallbacks != null) {
                    channelCallbacks.onChannelOpened(token, control.path);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error handling channel open request", e);
            }
        });
    }

    private void sendOpenAck(WearableConnection connection, String targetNodeId,
                             ChannelControlRequest originalRequest, AppKey appKey) {
        ChannelControlRequest ackControl = new ChannelControlRequest.Builder()
                .type(CHANNEL_CONTROL_TYPE_OPEN_ACK)
                .channelId(originalRequest.channelId)
                .fromChannelOperator(false)
                .packageName(appKey.packageName)
                .signatureDigest(appKey.signatureDigest)
                .path(originalRequest.path)
                .build();

        ChannelRequest channelRequest = new ChannelRequest.Builder()
                .channelControlRequest(ackControl)
                .version(1)
                .origin(0)
                .build();

        int requestId = requestIdCounter.getAndIncrement();

        Request request = new Request.Builder()
                .requestId(requestId)
                .packageName(appKey.packageName)
                .signatureDigest(appKey.signatureDigest)
                .targetNodeId(targetNodeId)
                .sourceNodeId(localNodeId)
                .request(channelRequest)
                .generation(generationCounter.get())
                .build();

        try {
            connection.writeMessage(new RootMessage.Builder()
                    .channelRequest(request)
                    .build());
            Log.d(TAG, "Sent channel open ack for channelId=" + originalRequest.channelId);
        } catch (IOException e) {
            Log.e(TAG, "Failed to send open ack", e);
        }
    }

    private void onChannelOpenAckReceived(ChannelControlRequest control) {
        Log.d(TAG, "onChannelOpenAckReceived: channelId=" + control.channelId);

        handler.post(() -> {
            String tokenString;
            synchronized (lock) {
                tokenString = channelIdToToken.get(control.channelId);
            }

            if (tokenString == null) {
                Log.w(TAG, "Received open ack for unknown channelId: " + control.channelId);
                return;
            }

            ChannelStateMachine channel = getChannel(tokenString);
            if (channel == null) {
                Log.w(TAG, "Channel not found for token: " + tokenString);
                return;
            }

            channel.onChannelEstablished();
            Log.d(TAG, "Channel established: " + channel);
        });
    }

    void onChannelCloseReceived(ChannelControlRequest control) {
        Log.d(TAG, "onChannelCloseReceived: channelId=" + control.channelId);

        handler.post(() -> {
            String tokenString;
            synchronized (lock) {
                tokenString = channelIdToToken.get(control.channelId);
            }

            if (tokenString == null) {
                Log.w(TAG, "Received close for unknown channelId: " + control.channelId);
                return;
            }

            ChannelStateMachine channel = getChannel(tokenString);
            if (channel == null) {
                Log.w(TAG, "Channel not found for token: " + tokenString);
                return;
            }

            try {
                int errorCode = control.closeErrorCode;
                channel.onRemoteClose(errorCode);
            } catch (Exception e) {
                Log.e(TAG, "Error handling channel close", e);
            } finally {
                synchronized (lock) {
                    channels.remove(tokenString);
                    channelIdToToken.remove(control.channelId);
                }
            }
        });
    }

    private void onChannelDataReceived(ChannelDataRequest dataRequest) {
        if (dataRequest.header == null) {
            Log.w(TAG, "Received data request with null header");
            return;
        }

        ChannelDataHeader header = dataRequest.header;
        Log.d(TAG, "onChannelDataReceived: channelId=" + header.channelId +
                ", size=" + (dataRequest.payload != null ? dataRequest.payload.size() : 0));

        handler.post(() -> {
            String tokenString;
            synchronized (lock) {
                tokenString = channelIdToToken.get(header.channelId);
            }

            if (tokenString == null) {
                Log.w(TAG, "Received data for unknown channelId: " + header.channelId);
                return;
            }

            ChannelStateMachine channel = getChannel(tokenString);
            if (channel == null) {
                Log.w(TAG, "Channel not found for token: " + tokenString);
                return;
            }

            try {
                byte[] data = dataRequest.payload != null ? dataRequest.payload.toByteArray() : new byte[0];
                boolean isFinal = dataRequest.finalMessage;
                long requestId = header.requestId;

                channel.onDataReceived(data, isFinal, requestId);

                sendDataAck(channel, requestId, isFinal);

            } catch (Exception e) {
                Log.e(TAG, "Error handling channel data", e);
            }
        });
    }

    private void onChannelDataAckReceived(ChannelDataAckRequest ackRequest) {
        if (ackRequest.header == null) {
            Log.w(TAG, "Received data ack with null header");
            return;
        }

        ChannelDataHeader header = ackRequest.header;
        Log.d(TAG, "onChannelDataAckReceived: channelId=" + header.channelId);

        handler.post(() -> {
            String tokenString;
            synchronized (lock) {
                tokenString = channelIdToToken.get(header.channelId);
            }

            if (tokenString == null) {
                Log.w(TAG, "Received ack for unknown channelId: " + header.channelId);
                return;
            }

            ChannelStateMachine channel = getChannel(tokenString);
            if (channel == null) {
                Log.w(TAG, "Channel not found for token: " + tokenString);
                return;
            }

            try {
                long requestId = header.requestId;
                boolean isFinal = ackRequest.finalMessage;
                channel.onDataAckReceived(requestId, isFinal);
            } catch (Exception e) {
                Log.e(TAG, "Error handling data ack", e);
            }
        });
    }

    private void sendDataAck(ChannelStateMachine channel, long requestId, boolean isFinal) {
        WearableConnection connection = wearable.getActiveConnections().get(channel.token.nodeId);
        if (connection == null) {
            Log.w(TAG, "Cannot send ack - connection not found");
            return;
        }

        try {
            ChannelDataHeader header = new ChannelDataHeader.Builder()
                    .channelId(channel.token.channelId)
                    .fromChannelOperator(channel.token.thisNodeWasOpener)
                    .requestId(requestId)
                    .build();

            ChannelDataAckRequest ackRequest = new ChannelDataAckRequest.Builder()
                    .header(header)
                    .finalMessage(isFinal)
                    .build();

            ChannelRequest channelRequest = new ChannelRequest.Builder()
                    .channelDataAckRequest(ackRequest)
                    .version(1)
                    .origin(0)
                    .build();

            Request request = new Request.Builder()
                    .requestId(requestIdCounter.getAndIncrement())
                    .targetNodeId(channel.token.nodeId)
                    .sourceNodeId(localNodeId)
                    .packageName(channel.token.appKey.packageName)
                    .signatureDigest(channel.token.appKey.signatureDigest)
                    .request(channelRequest)
                    .generation(generationCounter.get())
                    .build();

            connection.writeMessage(new RootMessage.Builder()
                    .channelRequest(request)
                    .build());
        } catch (IOException e) {
            Log.e(TAG, "Failed to send data ack", e);
        }
    }

    public boolean sendData(ChannelStateMachine channel, byte[] data, boolean isFinal, long requestId) {
        WearableConnection connection = wearable.getActiveConnections().get(channel.token.nodeId);
        if (connection == null) {
            Log.w(TAG, "Cannot send data - connection not found");
            return false;
        }

        try {
            ChannelDataHeader header = new ChannelDataHeader.Builder()
                    .channelId(channel.token.channelId)
                    .fromChannelOperator(channel.token.thisNodeWasOpener)
                    .requestId(requestId)
                    .build();

            ChannelDataRequest dataRequest = new ChannelDataRequest.Builder()
                    .header(header)
                    .payload(ByteString.of(data))
                    .finalMessage(isFinal)
                    .build();

            ChannelRequest channelRequest = new ChannelRequest.Builder()
                    .channelDataRequest(dataRequest)
                    .version(1)
                    .origin(0)
                    .build();

            Request request = new Request.Builder()
                    .requestId(requestIdCounter.getAndIncrement())
                    .targetNodeId(channel.token.nodeId)
                    .sourceNodeId(localNodeId)
                    .packageName(channel.token.appKey.packageName)
                    .signatureDigest(channel.token.appKey.signatureDigest)
                    .request(channelRequest)
                    .generation(generationCounter.get())
                    .build();

            connection.writeMessage(new RootMessage.Builder()
                    .channelRequest(request)
                    .build());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to send channel data", e);
            return false;
        }
    }


    private void onBinderDied(ChannelToken token) {
        Log.w(TAG, "Client died for channel: " + token);
        closeChannel(token, ChannelStatusCodes.CLOSE_REASON_LOCAL_CLOSE);
    }

    public static void sendFileResult(IWearableCallbacks callbacks, int statusCode) {
        try {
            callbacks.onChannelSendFileResponse(new ChannelSendFileResponse(statusCode));
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to send sendFile result", e);
        }
    }

    public static void receiveFileResult(IWearableCallbacks callbacks, int statusCode) {
        try {
            callbacks.onChannelReceiveFileResponse(new ChannelReceiveFileResponse(statusCode));
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to send receiveFile result", e);
        }
    }

    public static void getInputStreamError(IWearableCallbacks callbacks, int statusCode) {
        try {
            callbacks.onGetChannelInputStreamResponse(new GetChannelInputStreamResponse(statusCode, null));
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to send getInputStream error", e);
        }
    }

    public static void getOutputStreamError(IWearableCallbacks callbacks, int statusCode) {
        try {
            callbacks.onGetChannelOutputStreamResponse(new GetChannelOutputStreamResponse(statusCode, null));
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to send getOutputStream error", e);
        }
    }
}