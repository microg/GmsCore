package org.microg.gms.wearable.channel;

import android.util.Log;

import org.microg.gms.wearable.proto.ChannelDataHeader;
import org.microg.gms.wearable.proto.ChannelDataRequest;

import java.io.IOException;

public class OnChannelDataTask extends ChannelTask {
    private static final String TAG = "OnChannelDataTask";

    private final String sourceNodeId;
    private final ChannelDataRequest dataRequest;

    public OnChannelDataTask(ChannelManager manager, String sourceNodeId,
                             ChannelDataRequest dataRequest) {
        super(manager);
        this.sourceNodeId = sourceNodeId;
        this.dataRequest = dataRequest;
    }

    @Override
    protected void execute() throws IOException, ChannelException {
        if (dataRequest.header == null) {
            Log.w(TAG, "Received data request with null header");
            return;
        }

        ChannelDataHeader header = dataRequest.header;
        Log.d(TAG, "onChannelDataReceived: channelId=" + header.channelId +
                ", size=" + (dataRequest.payload != null ? dataRequest.payload.size() : 0));

        ChannelStateMachine channel = channelManager.channelTable.get(
                sourceNodeId, header.channelId, !header.fromChannelOperator);

        if (channel == null) {
            Log.w(TAG, "Received data for unknown channel: " + header.channelId);
            return;
        }

        setChannel(channel);

        byte[] data = dataRequest.payload != null ?
                dataRequest.payload.toByteArray() : new byte[0];
        boolean isFinal = dataRequest.finalMessage;
        long requestId = header.requestId;

        channel.onDataReceived(data, isFinal, requestId);
    }
}