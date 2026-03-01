package org.microg.gms.wearable.channel;

import android.os.Build;
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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okio.ByteString;

public class ChannelManager {
    private static final String TAG = "ChannelManager";

    private static final int PROCESSING_LOOP_DELAY_MS = 10;
    private static final long OPEN_TIMEOUT_MS = 15000;

    public static final int CHANNEL_CONTROL_TYPE_OPEN = 1;
    public static final int CHANNEL_CONTROL_TYPE_OPEN_ACK = 2;
    public static final int CHANNEL_CONTROL_TYPE_CLOSE = 3;

    public static final int CHANNEL_ORIGIN_CHANNEL_API = 0;

    private final Handler handler;
    private final WearableImpl wearable;
    private final String localNodeId;
    private final Random random;
    private final ChannelTransport transport;

    public final ChannelTable channelTable = new ChannelTable();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final BlockingQueue<ChannelTask> taskQueue = new LinkedBlockingQueue<>();

    private final AtomicInteger requestIdCounter = new AtomicInteger(1);
    private final AtomicInteger generationCounter = new AtomicInteger(1);

    private ChannelCallbacks channelCallbacks;

    private volatile long cooldownUntil = 0;
    private final Object callbacksLock = new Object();
    private final EnumMap<ChannelAssetApiEnum, ChannelCallbacks> callbacksMap =
            new EnumMap<>(ChannelAssetApiEnum.class);
    public final TrustedPeersService trustedPeers;

    private final Runnable processingLoop = new Runnable() {
        @Override
        public void run() {
            if (!isRunning.get()) {
                return;
            }

            try {
                ChannelTask task;
                while ((task = taskQueue.poll()) != null) {
                    task.run();
                }

                for (ChannelStateMachine channel : channelTable.values()) {
                    try {
                        processChannelIO(channel);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing channel I/O", e);
                        try {
                            channel.forceClose();
                            channelTable.remove(channel.token);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error during cleanup", ex);
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error in processing loop", e);
            }

            if (isRunning.get()) {
                handler.postDelayed(this, PROCESSING_LOOP_DELAY_MS);
            }
        }
    };

    public ChannelManager(Handler handler, WearableImpl wearable, String localNodeId, TrustedPeersService trustedPeers) {
        this.handler = handler;
        this.wearable = wearable;
        this.localNodeId = localNodeId;
        this.random = new Random();
        this.transport = new ChannelTransport();
        this.trustedPeers = trustedPeers;
    }

    public void setCallbacks(ChannelAssetApiEnum origin, ChannelCallbacks callbacks) {
        synchronized (callbacksLock) {
            if (callbacks == null) {
                callbacksMap.remove(origin);
            } else {
                if (callbacksMap.containsKey(origin)) {
                    throw new IllegalStateException(
                            "setCallbacks called twice for the same origin: " + origin);
                }
                callbacksMap.put(origin, callbacks);
            }
        }
    }

    public ChannelCallbacks getCallbacks(ChannelAssetApiEnum origin) {
        synchronized (callbacksLock) {
            ChannelCallbacks cb = callbacksMap.get(origin);
            if (cb == null) {
                throw new IllegalStateException("No callbacks set for origin: " + origin);
            }
            return cb;
        }
    }

    public ChannelCallbacks getCallbacksOrNull(ChannelAssetApiEnum origin) {
        synchronized (callbacksLock) {
            return callbacksMap.get(origin);
        }
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

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            Log.d(TAG, "ChannelManager started, localNodeId=" + localNodeId);
            handler.post(processingLoop);
        }
    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            handler.removeCallbacks(processingLoop);

            for (ChannelStateMachine channel : channelTable.values()) {
                try {
                    if (channel.openResultDispatcher != null) {
                        channel.openResultDispatcher.onResult(
                                ChannelStatusCodes.INTERNAL_ERROR, null, channel.channelPath);
                    }
                    channel.forceClose();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing channel on stop", e);
                }
            }

            channelTable.clear();
            transport.clear();
            taskQueue.clear();

            Log.d(TAG, "ChannelManager stopped");
        }
    }

    public void setChannelCallbacks(ChannelCallbacks callbacks) {
        this.channelCallbacks = callbacks;
    }

    public ChannelStateMachine getChannel(ChannelToken token) {
        return channelTable.get(token);
    }

    public void removeChannel(ChannelToken token) {
        channelTable.remove(token);
    }

    private void processChannelIO(ChannelStateMachine channel) throws IOException {
        if (channel.sendingState == ChannelStateMachine.SENDING_STATE_WAITING_TO_READ) {
            channel.processOutgoingData();
        }

        if (channel.receivingState == ChannelStateMachine.RECEIVING_STATE_WAITING_TO_WRITE) {
            channel.processIncomingBuffer();
        }
    }

    public void openChannel(AppKey appKey, String nodeId, String path,
                            boolean isReliable, OpenChannelCallback callback) {
        openChannel(ChannelAssetApiEnum.ORIGIN_CHANNEL_API, appKey, nodeId, path, isReliable, callback);
    }

    public void openChannel(ChannelAssetApiEnum origin, AppKey appKey, String nodeId,
                            String path, boolean isReliable, OpenChannelCallback callback) {
        Log.d(TAG, String.format("openChannel(%s, %s, %s)", appKey.packageName, nodeId, path));

        if (!isRunning.get()) {
            Log.w(TAG, "openChannel called while not running, deferring 500ms");
            handler.postDelayed(() -> {
                if (!isRunning.get()) {
                    Log.e(TAG, "openChannel: still not running after delay, failing");
                    callback.onResult(ChannelStatusCodes.INTERNAL_ERROR, null, path);
                } else {
                    openChannel(origin, appKey, nodeId, path, isReliable, callback);
                }
            }, 500);
            return;
        }

        if (isInCooldown()) {
            long delay = cooldownUntil - System.currentTimeMillis() + 100;
            Log.d(TAG, "Deferring channel open by " + delay + "ms due to cooldown");
            handler.postDelayed(
                    () -> openChannel(origin, appKey, nodeId, path, isReliable, callback), delay);
            return;
        }

        taskQueue.offer(new ChannelTask(this) {
            @Override
            protected void execute() throws IOException, ChannelException {
                doOpenChannel(origin, appKey, nodeId, path, isReliable, callback);
            }
        });
    }


    private void doOpenChannel(ChannelAssetApiEnum origin, AppKey appKey, String nodeId,
                               String path, boolean isReliable, OpenChannelCallback callback)
            throws IOException, ChannelException {

        WearableConnection connection = wearable.getActiveConnections().get(nodeId);
        if (connection == null) {
            Log.w(TAG, "Target node not connected: " + nodeId);
            callback.onResult(ChannelStatusCodes.CHANNEL_NOT_CONNECTED, null, path);
            return;
        }

        long channelId = generateChannelId(nodeId);
        ChannelToken token = new ChannelToken(nodeId, appKey, channelId, true);

        IBinder.DeathRecipient deathRecipient = () -> onBinderDied(token);

        ChannelCallbacks callbacks = getCallbacksOrNull(origin);

        ChannelStateMachine channel = new ChannelStateMachine(
                token, this, transport, callbacks, origin, isReliable, false, deathRecipient, handler);
        channel.channelPath = path;
        channel.openResultDispatcher = callback;

        channelTable.put(token, channel);

        channel.openTimeoutOp = new PendingOperation(handler,
                () -> onOpenTimeout(token), OPEN_TIMEOUT_MS, "Open channel");

        channel.sendOpenRequest();
    }


    private void onOpenTimeout(ChannelToken token) {
        taskQueue.offer(new ChannelTask(this) {
            @Override
            protected void execute() throws IOException, ChannelException {
                ChannelStateMachine ch = channelTable.get(token);
                if (ch == null) return;
                setChannel(ch);

                if (ch.connectionState == ChannelStateMachine.CONNECTION_STATE_ESTABLISHED) return;

                ch.openTimeoutOp = null;
                if (ch.openResultDispatcher == null) {
                    throw new ChannelException(token, "No callback on timeout");
                }
                ch.openResultDispatcher.onResult(
                        ChannelStatusCodes.CLOSE_REASON_REMOTE_CLOSE, null, ch.channelPath);
                ch.openResultDispatcher = null;
            }
        });
    }

    private long generateChannelId(String nodeId) {
        for (int i = 0; i < 5; i++) {
            long id = random.nextLong() & Long.MAX_VALUE;
            if (channelTable.get(nodeId, id, true) == null) return id;
        }
        throw new IllegalStateException(
                "Failed to generate a free channel ID. Table size: " + channelTable.size());
    }

    public void closeChannel(ChannelToken token, int errorCode) {
        ChannelStateMachine channel = getChannel(token);
        if (channel == null) {
            Log.w(TAG, "closeChannel: channel not found");
            return;
        }

        taskQueue.offer(new ChannelTask(this) {
            @Override
            protected void execute() throws IOException {
                setChannel(channel);
                doCloseChannel(channel, errorCode);
            }
        });
    }

    private void doCloseChannel(ChannelStateMachine channel, int errorCode) throws IOException {
        try {
            channel.sendCloseRequest(errorCode);
            channel.forceClose();
        } finally {
            channelTable.remove(channel.token);
        }
    }

    public void onChannelRequestReceived(WearableConnection connection, String sourceNodeId, Request request) {
        Log.d(TAG, String.format("onChannelRequestReceived (%s): %s", sourceNodeId, request.toString()));
        if (request.request == null) {
            Log.w(TAG, "Received channel request with null ChannelRequest");
            return;
        }

        ChannelRequest channelRequest = request.request;

        if (channelRequest.channelControlRequest != null) {
            taskQueue.offer(new OnChannelControlTask(this, sourceNodeId, connection, request));
        } else if (channelRequest.channelDataRequest != null) {
            taskQueue.offer(new OnChannelDataTask(this, sourceNodeId, channelRequest.channelDataRequest));
        } else if (channelRequest.channelDataAckRequest != null) {
            taskQueue.offer(new OnChannelDataAckTask(this, sourceNodeId, channelRequest.channelDataAckRequest));
        }
    }

    public void sendOpenRequest(ChannelStateMachine channel) throws IOException {
        WearableConnection conn = requireConnection(channel.token.nodeId);

        ChannelControlRequest ctrl = new ChannelControlRequest.Builder()
                .type(CHANNEL_CONTROL_TYPE_OPEN)
                .channelId(channel.token.channelId)
                .fromChannelOperator(channel.token.thisNodeWasOpener)
                .packageName(channel.token.appKey.packageName)
                .signatureDigest(channel.token.appKey.signatureDigest)
                .path(channel.channelPath)
                .isReliable(channel.isReliable)
                .build();

        conn.writeMessage(buildRootMessage(channel, ctrl));
        Log.d(TAG, "Sent open request for " + channel.token);
    }


    public void sendOpenAck(ChannelStateMachine channel) throws IOException {
        WearableConnection conn = requireConnection(channel.token.nodeId);

        ChannelControlRequest ctrl = new ChannelControlRequest.Builder()
                .type(CHANNEL_CONTROL_TYPE_OPEN_ACK)
                .channelId(channel.token.channelId)
                .fromChannelOperator(channel.token.thisNodeWasOpener)
                .packageName(channel.token.appKey.packageName)
                .signatureDigest(channel.token.appKey.signatureDigest)
                .path(channel.channelPath)
                .build();

        conn.writeMessage(buildRootMessage(channel, ctrl));
        Log.d(TAG, "Sent open ACK for " + channel.token);
    }

    public void sendCloseRequest(ChannelStateMachine channel, int errorCode) throws IOException {
        WearableConnection conn = wearable.getActiveConnections().get(channel.token.nodeId);
        if (conn == null) {
            Log.w(TAG, "Cannot send close — connection not found for " + channel.token.nodeId);
            return;
        }

        ChannelControlRequest ctrl = new ChannelControlRequest.Builder()
                .type(CHANNEL_CONTROL_TYPE_CLOSE)
                .channelId(channel.token.channelId)
                .fromChannelOperator(channel.token.thisNodeWasOpener)
                .packageName(channel.token.appKey.packageName)
                .signatureDigest(channel.token.appKey.signatureDigest)
                .closeErrorCode(errorCode)
                .build();

        conn.writeMessage(buildRootMessage(channel, ctrl));
        Log.d(TAG, "Sent close request for " + channel.token);
    }

    public boolean sendData(ChannelStateMachine channel, byte[] data, boolean isFinal, long requestId) {
        WearableConnection conn = wearable.getActiveConnections().get(channel.token.nodeId);
        if (conn == null) {
            Log.w(TAG, "Cannot send data — connection not found");
            return false;
        }

        try {
            ChannelDataHeader hdr = new ChannelDataHeader.Builder()
                    .channelId(channel.token.channelId)
                    .fromChannelOperator(channel.token.thisNodeWasOpener)
                    .requestId(0L)
                    .build();

            ChannelDataRequest dataReq = new ChannelDataRequest.Builder()
                    .header(hdr)
                    .payload(ByteString.of(data))
                    .finalMessage(isFinal)
                    .build();

            conn.writeMessage(buildDataRootMessage(channel, dataReq));
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to send channel data", e);
            return false;
        }
    }

    public void sendDataAck(ChannelStateMachine channel, long offset, boolean isFinal) {
        WearableConnection conn = wearable.getActiveConnections().get(channel.token.nodeId);
        if (conn == null) {
            Log.w(TAG, "Cannot send ack — connection not found");
            return;
        }

        try {
            ChannelDataHeader hdr = new ChannelDataHeader.Builder()
                    .channelId(channel.token.channelId)
                    .fromChannelOperator(channel.token.thisNodeWasOpener)
                    .requestId(0L)
                    .build();

            ChannelDataAckRequest ack = new ChannelDataAckRequest.Builder()
                    .header(hdr)
                    .finalMessage(isFinal)
                    .build();

            conn.writeMessage(buildAckRootMessage(channel, ack));
        } catch (IOException e) {
            Log.e(TAG, "Failed to send data ack", e);
        }
    }

    private WearableConnection requireConnection(String nodeId) throws IOException {
        WearableConnection conn = wearable.getActiveConnections().get(nodeId);
        if (conn == null) throw new IOException("No connection to " + nodeId);
        return conn;
    }


    private RootMessage buildRootMessage(ChannelStateMachine ch, ChannelControlRequest ctrl) {
        ChannelRequest cr = new ChannelRequest.Builder()
                .channelControlRequest(ctrl)
                .version(0)
                .origin(0)
                .build();

        Request req = new Request.Builder()
                .requestId(requestIdCounter.getAndIncrement())
                .targetNodeId(ch.token.nodeId)
                .sourceNodeId(localNodeId)
                .packageName(ch.token.appKey.packageName)
                .signatureDigest(ch.token.appKey.signatureDigest)
                .path(ch.channelPath)
                .request(cr)
                .unknown5(0)
                .generation(generationCounter.get())
                .build();

        return new RootMessage.Builder().channelRequest(req).build();
    }

    private RootMessage buildDataRootMessage(ChannelStateMachine ch, ChannelDataRequest dataReq) {
        ChannelRequest cr = new ChannelRequest.Builder()
                .channelDataRequest(dataReq).version(1).origin(0).build();
        Request req = new Request.Builder()
                .requestId(requestIdCounter.getAndIncrement())
                .targetNodeId(ch.token.nodeId)
                .sourceNodeId(localNodeId)
                .packageName(ch.token.appKey.packageName)
                .signatureDigest(ch.token.appKey.signatureDigest)
                .request(cr)
                .generation(generationCounter.get())
                .build();
        return new RootMessage.Builder().channelRequest(req).build();
    }

    private RootMessage buildAckRootMessage(ChannelStateMachine ch, ChannelDataAckRequest ack) {
        ChannelRequest cr = new ChannelRequest.Builder()
                .channelDataAckRequest(ack).version(1).origin(0).build();
        Request req = new Request.Builder()
                .requestId(requestIdCounter.getAndIncrement())
                .targetNodeId(ch.token.nodeId)
                .sourceNodeId(localNodeId)
                .packageName(ch.token.appKey.packageName)
                .signatureDigest(ch.token.appKey.signatureDigest)
                .request(cr)
                .generation(generationCounter.get())
                .build();
        return new RootMessage.Builder().channelRequest(req).build();
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

    public ChannelCallbacks getChannelCallbacks() {
        return channelCallbacks;
    }

    public ChannelTransport getTransport() {
        return transport;
    }

    public Handler getHandler() {
        return handler;
    }
}