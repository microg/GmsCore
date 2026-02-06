package org.microg.gms.wearable.channel;

import android.util.Log;

import org.microg.gms.wearable.proto.ChannelDataAckRequest;
import org.microg.gms.wearable.proto.ChannelDataHeader;

public class OnChannelDataAckTask extends ChannelTask {
    private static final String TAG = "OnChannelDataAckTask";

    private final String sourceNodeId;
    private final ChannelDataAckRequest ackRequest;

    public OnChannelDataAckTask(ChannelManager manager, String sourceNodeId, ChannelDataAckRequest ackRequest) {
        super(manager);
        this.ackRequest = ackRequest;
        this.sourceNodeId = sourceNodeId;
    }

    @Override
    protected void execute() throws ChannelException {
        if (ackRequest.header == null) {
            Log.w(TAG, "Received data ACK with null header");
            return;
        }

        ChannelDataHeader header = ackRequest.header;
        Log.d(TAG, "onChannelDataAckReceived: channelId=" + header.channelId +
                ", from=" + sourceNodeId);

        ChannelStateMachine channel = channelManager.channelTable.get(
                sourceNodeId,
                header.channelId,
                header.fromChannelOperator
        );

        if (channel == null) {
            Log.w(TAG, "Received ACK for unknown channel: " + header.channelId +
                    " from node: " + sourceNodeId);
            return;
        }

        setChannel(channel);

        long requestId = header.requestId;
        boolean isFinal = ackRequest.finalMessage;

        channel.onDataAckReceived(requestId, isFinal);
    }
}