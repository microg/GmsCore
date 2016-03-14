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
import com.google.android.gms.wearable.internal.NodeParcelable;

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
    private final WearableServiceImpl service;
    private final ConnectionConfiguration config;

    public MessageHandler(WearableServiceImpl service, ConnectionConfiguration config) {
        this(service, config, new Build().model, config.nodeId, LastCheckinInfo.read(service.getContext()).androidId);
    }

    private MessageHandler(WearableServiceImpl service, ConnectionConfiguration config, String name, String networkId, long androidId) {
        super(new Connect.Builder()
                .name(name)
                .id(config.nodeId)
                .networkId(networkId)
                .peerAndroidId(androidId)
                .unknown4(3)
                .unknown5(1)
                .build());
        this.service = service;
        this.config = config;
    }

    @Override
    public void onConnect(Connect connect) {
        super.onConnect(connect);
        config.peerNodeId = connect.id;
        config.connected = true;
        service.onPeerConnected(new NodeParcelable(connect.id, connect.name));
        try {
            getConnection().writeMessage(new RootMessage.Builder().syncStart(new SyncStart.Builder()
                    .receivedSeqId(-1L)
                    .version(2)
                    .syncTable(Arrays.asList(
                            new SyncTableEntry.Builder().key("cloud").value(1L).build(),
                            new SyncTableEntry.Builder().key(config.nodeId).value(service.getCurrentSeqId(config.nodeId)).build(), // TODO
                            new SyncTableEntry.Builder().key(config.peerNodeId).value(service.getCurrentSeqId(config.peerNodeId)).build() // TODO
                    )).build()).build());
        } catch (IOException e) {
            Log.w(TAG, e);
        }
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
        service.addAssetToDatabase(asset, setAsset.appkeys.appKeys);
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
                service.syncToPeer(getConnection(), entry.key, entry.value);
                if (service.getLocalNodeId().equals(entry.key)) hasLocalNode = true;
            }
        } else {
            Log.d(TAG, "No sync table given.");
        }
        if (!hasLocalNode) service.syncToPeer(getConnection(), service.getLocalNodeId(), 0);
    }

    @Override
    public void onSetDataItem(SetDataItem setDataItem) {
        Log.d(TAG, "onSetDataItem: " + setDataItem);
        service.putDataItem(DataItemRecord.fromSetDataItem(setDataItem));
    }

    @Override
    public void onRcpRequest(Request rcpRequest) {
        Log.d(TAG, "onRcpRequest: " + rcpRequest);
        if (TextUtils.isEmpty(rcpRequest.targetNodeId)) {
            // TODO: That's probably not how it should go!
            MessageEventParcelable messageEvent = new MessageEventParcelable();
            messageEvent.data = rcpRequest.rawData != null ? rcpRequest.rawData.toByteArray() : null;
            messageEvent.path = rcpRequest.path;
            messageEvent.requestId = rcpRequest.requestId + 31 * (rcpRequest.generation + 527);
            messageEvent.sourceNodeId = TextUtils.isEmpty(rcpRequest.sourceNodeId) ? config.peerNodeId : rcpRequest.sourceNodeId;

            service.onMessageReceived(messageEvent);
        } else if (rcpRequest.targetNodeId.equals(config.peerNodeId)) {
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
        service.handleFilePiece(getConnection(), filePiece.fileName, filePiece.piece.toByteArray(), filePiece.finalPiece ? filePiece.digest : null);
    }

    @Override
    public void onChannelRequest(Request channelRequest) {
        Log.d(TAG, "onChannelRequest:" + channelRequest);
    }
}
