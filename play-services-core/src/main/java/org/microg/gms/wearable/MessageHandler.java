/*
 * Copyright 2013-2015 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.wearable;

import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.MessageEventParcelable;

import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.Build;
import org.microg.wearable.ServerMessageListener;
import org.microg.wearable.proto.AckAsset;
import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.FetchAsset;
import org.microg.wearable.proto.FilePiece;
import org.microg.wearable.proto.Heartbeat;
import org.microg.wearable.proto.Request;
import org.microg.wearable.proto.RootMessage;
import org.microg.wearable.proto.SetAsset;
import org.microg.wearable.proto.SetDataItem;
import org.microg.wearable.proto.SyncStart;
import org.microg.wearable.proto.SyncTableEntry;

import java.io.IOException;
import java.util.Arrays;

public class MessageHandler extends ServerMessageListener {
    private static final String TAG = "GmsWearMsgHandler";
    private final WearableImpl wearable;
    private final String thisNodeId;
    private String peerNodeId;

    public MessageHandler(WearableImpl wearable, ConnectionConfiguration config) {
        this(wearable, config, new Build().model, config.nodeId, LastCheckinInfo.read(wearable.getContext()).androidId);
    }

    private MessageHandler(WearableImpl wearable, ConnectionConfiguration config, String name, String networkId, long androidId) {
        super(new Connect.Builder()
                .name(name)
                .id(config.nodeId)
                .networkId(networkId)
                .peerAndroidId(androidId)
                .unknown4(3)
                .peerVersion(1)
                .build());
        this.wearable = wearable;
        this.thisNodeId = config.nodeId;
    }

    @Override
    public void onConnect(Connect connect) {
        super.onConnect(connect);
        peerNodeId = connect.id;
        wearable.onConnectReceived(getConnection(), thisNodeId, connect);
        try {
            getConnection().writeMessage(new RootMessage.Builder().syncStart(new SyncStart.Builder()
                    .receivedSeqId(-1L)
                    .version(2)
                    .syncTable(Arrays.asList(
                            new SyncTableEntry.Builder().key("cloud").value(1L).build(),
                            new SyncTableEntry.Builder().key(thisNodeId).value(wearable.getCurrentSeqId(thisNodeId)).build(), // TODO
                            new SyncTableEntry.Builder().key(peerNodeId).value(wearable.getCurrentSeqId(peerNodeId)).build() // TODO
                    )).build()).build());
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void onDisconnected() {
        wearable.onDisconnectReceived(getConnection(), thisNodeId, getRemoteConnect());
        super.onDisconnected();
    }

    @Override
    public void onSetAsset(SetAsset setAsset) {
        Log.d(TAG, "onSetAsset: " + setAsset);
        Asset asset;
        if (setAsset.data != null) {
            asset = Asset.createFromBytes(setAsset.data.toByteArray());
        } else {
            asset = Asset.createFromRef(setAsset.digest);
        }
        wearable.addAssetToDatabase(asset, setAsset.appkeys.appKeys);
    }

    @Override
    public void onAckAsset(AckAsset ackAsset) {
        Log.d(TAG, "onAckAsset: " + ackAsset);
    }

    @Override
    public void onFetchAsset(FetchAsset fetchAsset) {
        Log.d(TAG, "onFetchAsset: " + fetchAsset);
    }

    @Override
    public void onSyncStart(SyncStart syncStart) {
        Log.d(TAG, "onSyncStart: " + syncStart);
        if (syncStart.version < 2) {
            Log.d(TAG, "Sync uses version " + syncStart.version + " which is not supported (yet)");
        }
        boolean hasLocalNode = false;
        if (syncStart.syncTable != null) {
            for (SyncTableEntry entry : syncStart.syncTable) {
                wearable.syncToPeer(getConnection(), entry.key, entry.value);
                if (wearable.getLocalNodeId().equals(entry.key)) hasLocalNode = true;
            }
        } else {
            Log.d(TAG, "No sync table given.");
        }
        if (!hasLocalNode) wearable.syncToPeer(getConnection(), wearable.getLocalNodeId(), 0);
    }

    @Override
    public void onSetDataItem(SetDataItem setDataItem) {
        Log.d(TAG, "onSetDataItem: " + setDataItem);
        wearable.putDataItem(DataItemRecord.fromSetDataItem(setDataItem));
    }

    @Override
    public void onRpcRequest(Request rpcRequest) {
        Log.d(TAG, "onRpcRequest: " + rpcRequest);
        if (TextUtils.isEmpty(rpcRequest.targetNodeId) || rpcRequest.targetNodeId.equals(thisNodeId)) {
            MessageEventParcelable messageEvent = new MessageEventParcelable();
            messageEvent.data = rpcRequest.rawData != null ? rpcRequest.rawData.toByteArray() : null;
            messageEvent.path = rpcRequest.path;
            messageEvent.requestId = rpcRequest.requestId + 31 * (rpcRequest.generation + 527);
            messageEvent.sourceNodeId = TextUtils.isEmpty(rpcRequest.sourceNodeId) ? peerNodeId : rpcRequest.sourceNodeId;

            wearable.sendMessageReceived(rpcRequest.packageName, messageEvent);
        } else if (rpcRequest.targetNodeId.equals(peerNodeId)) {
            // Drop it, loop detection (yes we really need this in this protocol o.O)
        } else {
            // TODO: find next hop (yes, wtf hops in a network usually consisting of two devices)
        }
    }

    @Override
    public void onHeartbeat(Heartbeat heartbeat) {
        Log.d(TAG, "onHeartbeat: " + heartbeat);
    }

    @Override
    public void onFilePiece(FilePiece filePiece) {
        Log.d(TAG, "onFilePiece: " + filePiece);
        wearable.handleFilePiece(getConnection(), filePiece.fileName, filePiece.piece.toByteArray(), filePiece.finalPiece ? filePiece.digest : null);
    }

    @Override
    public void onChannelRequest(Request channelRequest) {
        Log.d(TAG, "onChannelRequest:" + channelRequest);
    }
}
