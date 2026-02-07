package org.microg.gms.wearable.channel;

import android.os.IBinder;
import android.util.Log;

import org.microg.gms.wearable.WearableConnection;
import org.microg.gms.wearable.proto.AppKey;
import org.microg.gms.wearable.proto.ChannelControlRequest;
import org.microg.gms.wearable.proto.Request;

import java.io.IOException;

public class OnChannelControlTask extends ChannelTask {
    private static final String TAG = "OnChannelControlTask";

    private final String sourceNodeId;
    private final WearableConnection connection;
    private final Request request;

    public OnChannelControlTask(ChannelManager manager, String sourceNodeId,
                                WearableConnection connection, Request request) {
        super(manager);
        this.sourceNodeId = sourceNodeId;
        this.connection = connection;
        this.request = request;
    }

    @Override
    protected void execute() throws IOException, ChannelException {
        ChannelControlRequest control = request.request.channelControlRequest;

        if (control == null) {
            Log.w(TAG, "Channel control request is null");
            return;
        }

        int type = control.type;
        Log.d(TAG, "onChannelControlReceived: type=" + type +
                ", channelId=" + control.channelId +
                ", from=" + sourceNodeId);

        switch (type) {
            case ChannelManager.CHANNEL_CONTROL_TYPE_OPEN:
                handleChannelOpen(control);
                break;
            case ChannelManager.CHANNEL_CONTROL_TYPE_OPEN_ACK:
                handleChannelOpenAck(control);
                break;
            case ChannelManager.CHANNEL_CONTROL_TYPE_CLOSE:
                handleChannelClose(control);
                break;
            default:
                Log.w(TAG, "Unknown channel control type: " + type);
        }
    }

    private void handleChannelOpen(ChannelControlRequest control) throws IOException, ChannelException {
        Log.d(TAG, "handleChannelOpen: channelId=" + control.channelId +
                ", path=" + control.path + ", from=" + sourceNodeId);

        if (connection == null) {
            Log.w(TAG, "Received channel open from null connection: " + sourceNodeId);
            throw new ChannelException(null, "Connection not active");
        }

        if (control.packageName == null || control.packageName.isEmpty()) {
            Log.w(TAG, "Channel open missing package name");
            throw new ChannelException(null, "Missing package name");
        }

        if (control.signatureDigest == null || control.signatureDigest.isEmpty()) {
            Log.w(TAG, "Channel open missing signature digest");
            throw new ChannelException(null, "Missing signature digest");
        }

        if (control.path == null || control.path.isEmpty()) {
            Log.w(TAG, "Channel open missing path");
            throw new ChannelException(null, "Missing path");
        }

        AppKey appKey = new AppKey(control.packageName, control.signatureDigest);

        boolean isReliable = control.isReliable != null ? control.isReliable : true;

        ChannelToken token = new ChannelToken(sourceNodeId, appKey, control.channelId, false, isReliable);

        ChannelStateMachine channel = channelManager.channelTable.get(token);

        if (channel != null) {
            handleDuplicateChannelOpen(channel, control);
            return;
        }

        if (!checkChannelLimits(sourceNodeId, appKey)) {
            Log.w(TAG, "Channel limit reached for " + sourceNodeId + "/" + appKey.packageName);
            sendOpenError(token, control.path, ChannelStatusCodes.CHANNEL_LIMIT_REACHED);
            return;
        }

        IBinder.DeathRecipient deathRecipient = () -> onChannelBinderDied(token);

        ChannelCallbacks callbacks = channelManager.getChannelCallbacks();

        channel = new ChannelStateMachine(
                token,
                channelManager,
                channelManager.getTransport(),  // FIX: Use shared transport
                callbacks,
                false,
                deathRecipient,
                channelManager.getHandler()  // FIX: Add getter for handler
        );

        channel.channelPath = control.path;
        channel.setConnectionState(ChannelStateMachine.CONNECTION_STATE_ESTABLISHED);

        channelManager.channelTable.put(token, channel);

        try {
            channel.sendOpenAck();
        } catch (IOException e) {
            Log.e(TAG, "Failed to send open ACK", e);
            channelManager.channelTable.remove(token);
            throw e;
        }

        scheduleHealthCheck(channel);

        if (callbacks != null) {
            try {
                callbacks.onChannelOpened(token, control.path);
            } catch (Exception e) {
                Log.e(TAG, "Error in channel opened callback", e);
            }
        }

        Log.d(TAG, "Channel opened successfully: " + channel);
    }

    private void handleDuplicateChannelOpen(ChannelStateMachine existingChannel,
                                            ChannelControlRequest control) throws IOException, ChannelException {
        Log.d(TAG, "Received duplicate OPEN for existing channel: " + existingChannel.token);

        setChannel(existingChannel);

        if (existingChannel.connectionState == ChannelStateMachine.CONNECTION_STATE_ESTABLISHED) {
            if (!existingChannel.channelPath.equals(control.path)) {
                Log.w(TAG, "Duplicate OPEN with different path. Expected: " +
                        existingChannel.channelPath + ", got: " + control.path);
                throw new ChannelException(existingChannel.token, "Path mismatch on duplicate OPEN");
            }

            Log.d(TAG, "Resending ACK for duplicate OPEN request");
            existingChannel.sendOpenAck();
        } else {
            Log.w(TAG, "Received OPEN for channel in state: " +
                    ChannelStateMachine.getConnectionStateString(existingChannel.connectionState));
            throw new ChannelException(existingChannel.token,
                    "Channel exists in unexpected state: " + existingChannel.connectionState);
        }
    }

    private boolean checkChannelLimits(String nodeId, AppKey appKey) {
        int channelsForNode = 0;
        int channelsForApp = 0;

        for (ChannelStateMachine channel : channelManager.channelTable.values()) {
            if (channel.token.nodeId.equals(nodeId)) {
                channelsForNode++;

                if (channel.token.appKey.equals(appKey)) {
                    channelsForApp++;
                }
            }
        }

        final int MAX_CHANNELS_PER_NODE = 20;
        final int MAX_CHANNELS_PER_APP = 10;

        if (channelsForNode >= MAX_CHANNELS_PER_NODE) {
            Log.w(TAG, "Node " + nodeId + " has reached channel limit: " + channelsForNode);
            return false;
        }

        if (channelsForApp >= MAX_CHANNELS_PER_APP) {
            Log.w(TAG, "App " + appKey.packageName + " has reached channel limit: " + channelsForApp);
            return false;
        }

        return true;
    }

    private void sendOpenError(ChannelToken token, String path, int errorCode) {
        try {
            ChannelStateMachine tempChannel = new ChannelStateMachine(
                    token,
                    channelManager,
                    channelManager.getTransport(),
                    null,
                    false,
                    null,
                    channelManager.getHandler()
            );

            tempChannel.channelPath = path;
            tempChannel.sendCloseRequest(errorCode);
        } catch (IOException e) {
            Log.w(TAG, "Failed to send open error", e);
        }
    }

    private void scheduleHealthCheck(ChannelStateMachine channel) {
        channelManager.getHandler().postDelayed(
                () -> performHealthCheck(channel),
                30000
        );
    }

    private void performHealthCheck(ChannelStateMachine channel) {
        if (!channelManager.isRunning()) {
            return;
        }

        ChannelStateMachine current = channelManager.channelTable.get(channel.token);
        if (current == null) {
            return;
        }

        if (current.connectionState == ChannelStateMachine.CONNECTION_STATE_ESTABLISHED) {
            long timeSinceCreation = System.currentTimeMillis() - channel.creationTime;

            if (timeSinceCreation > 60000) {
                if (current.sendingState == ChannelStateMachine.SENDING_STATE_NOT_STARTED &&
                        current.receivingState == ChannelStateMachine.RECEIVING_STATE_WAITING_FOR_DATA &&
                        !current.hasInputStream() && !current.hasOutputStream()) {

                    Log.w(TAG, "Channel appears unused after 1 minute: " + channel.token);
                }
            }

            channelManager.getHandler().postDelayed(
                    () -> performHealthCheck(channel),
                    60000
            );
        }
    }

    private void onChannelBinderDied(ChannelToken token) {
        Log.w(TAG, "Channel client died: " + token);

        channelManager.getHandler().post(() -> {
            try {
                ChannelStateMachine channel = channelManager.channelTable.get(token);
                if (channel != null) {
                    channel.forceClose();
                    channelManager.channelTable.remove(token);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling binder death", e);
            }
        });
    }


    private void handleChannelOpenAck(ChannelControlRequest control) throws ChannelException {
        Log.d(TAG, "handleChannelOpenAck: channelId=" + control.channelId);

        ChannelStateMachine channel = channelManager.channelTable.get(
                sourceNodeId, control.channelId, true);

        if (channel == null) {
            Log.w(TAG, "Received open ACK for unknown channel: " + control.channelId);
            return;
        }

        setChannel(channel);

        if (!sourceNodeId.equals(channel.token.nodeId)) {
            Log.w(TAG, String.format("Got OPEN_ACK from wrong node for channel %d. Expected %s got %s",
                    control.channelId, channel.token.nodeId, sourceNodeId));
            return;
        }

        if (channel.connectionState != ChannelStateMachine.CONNECTION_STATE_OPEN_SENT) {
            Log.w(TAG, "Received OPEN_ACK in wrong state: " +
                    ChannelStateMachine.getConnectionStateString(channel.connectionState));

            if (channel.connectionState == ChannelStateMachine.CONNECTION_STATE_ESTABLISHED) {
                Log.d(TAG, "Ignoring duplicate OPEN_ACK");
                return;
            }

            throw new ChannelException(channel.token, "Received OPEN_ACK in wrong state");
        }

        try {
            channel.onChannelOpenAckReceived();
            Log.d(TAG, "Channel established: " + channel);

            scheduleHealthCheck(channel);

        } catch (ChannelException e) {
            Log.e(TAG, "Error processing OPEN_ACK", e);
            channelManager.channelTable.remove(channel.token);
            throw e;
        }
    }

    private void handleChannelClose(ChannelControlRequest control) throws IOException, ChannelException {
        Log.d(TAG, "handleChannelClose: channelId=" + control.channelId);

        ChannelStateMachine channel = channelManager.channelTable.get(
                sourceNodeId, control.channelId, !control.fromChannelOperator);

        if (channel == null) {
            Log.w(TAG, "Received close for unknown channel: " + control.channelId);
            return;
        }

        setChannel(channel);

        try {
            int errorCode = control.closeErrorCode;
            channel.onRemoteCloseReceived(errorCode);
            Log.d(TAG, "Channel closed by remote: " + channel.token);
        } catch (ChannelException e) {
            Log.e(TAG, "Error handling remote close", e);
            throw e;
        } finally {
            channelManager.channelTable.remove(channel.token);
        }
    }
}