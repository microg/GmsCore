/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import org.microg.gms.wearable.proto.AckAsset;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.FetchAsset;
import org.microg.gms.wearable.proto.FilePiece;
import org.microg.gms.wearable.proto.Heartbeat;
import org.microg.gms.wearable.proto.Request;
import org.microg.gms.wearable.proto.RootMessage;
import org.microg.gms.wearable.proto.SetAsset;
import org.microg.gms.wearable.proto.SetDataItem;
import org.microg.gms.wearable.proto.SyncStart;

public abstract class MessageListener implements WearableConnection.Listener {
    private WearableConnection connection;

    @Override
    public void onConnected(WearableConnection connection) {
        this.connection = connection;
    }

    @Override
    public void onDisconnected() {
        this.connection = null;
    }

    public WearableConnection getConnection() {
        return connection;
    }

    @Override
    public void onMessage(WearableConnection connection, RootMessage message) {
        if (message.setAsset != null) {
            onSetAsset(message.setAsset);
        } else if (message.ackAsset != null) {
            onAckAsset(message.ackAsset);
        } else if (message.fetchAsset != null) {
            onFetchAsset(message.fetchAsset);
        } else if (message.connect != null) {
            onConnect(message.connect);
        } else if (message.syncStart != null) {
            onSyncStart(message.syncStart);
        } else if (message.setDataItem != null) {
            onSetDataItem(message.setDataItem);
        } else if (message.rpcRequest != null) {
            onRpcRequest(message.rpcRequest);
        } else if (message.heartbeat != null) {
            onHeartbeat(message.heartbeat);
        } else if (message.filePiece != null) {
            onFilePiece(message.filePiece);
        } else if (message.channelRequest != null) {
            onChannelRequest(message.channelRequest);
        } else {
            System.err.println("Unknown message: " + message);
        }
    }

    public abstract void onSetAsset(SetAsset setAsset);

    public abstract void onAckAsset(AckAsset ackAsset);

    public abstract void onFetchAsset(FetchAsset fetchAsset);

    public abstract void onConnect(Connect connect);

    public abstract void onSyncStart(SyncStart syncStart);

    public abstract void onSetDataItem(SetDataItem setDataItem);

    public abstract void onRpcRequest(Request rpcRequest);

    public abstract void onHeartbeat(Heartbeat heartbeat);

    public abstract void onFilePiece(FilePiece filePiece);

    public abstract void onChannelRequest(Request channelRequest);
}
