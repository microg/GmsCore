/*
 * Copyright (C) 2013-2017 microG Project Team
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

import static org.microg.gms.wearable.WearableConnection.calculateDigest;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.MessageEventParcelable;

import org.microg.gms.common.Utils;
import org.microg.gms.profile.Build;
import org.microg.gms.settings.SettingsContract;
import org.microg.gms.wearable.proto.AckAsset;
import org.microg.gms.wearable.proto.AppKey;
import org.microg.gms.wearable.proto.AssetEntry;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.FetchAsset;
import org.microg.gms.wearable.proto.FilePiece;
import org.microg.gms.wearable.proto.Heartbeat;
import org.microg.gms.wearable.proto.Request;
import org.microg.gms.wearable.proto.RootMessage;
import org.microg.gms.wearable.proto.SetAsset;
import org.microg.gms.wearable.proto.SetDataItem;
import org.microg.gms.wearable.proto.SyncStart;
import org.microg.gms.wearable.proto.SyncTableEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okio.ByteString;

public class MessageHandler extends ServerMessageListener {
    private static final String TAG = "WearMessageHandler";
    private final WearableImpl wearable;
    private final String oldConfigNodeId;
    private String peerNodeId;

    public MessageHandler(Context context, WearableImpl wearable, ConnectionConfiguration config) {
        this(wearable, config, Build.MODEL, config.nodeId, SettingsContract.getSettings(context, SettingsContract.CheckIn.INSTANCE.getContentUri(context), new String[]{SettingsContract.CheckIn.ANDROID_ID}, cursor -> cursor.getLong(0)));
    }

    private MessageHandler(WearableImpl wearable, ConnectionConfiguration config, String name, String networkId, long androidId) {
        super(new Connect.Builder()
                .name(name)
                .id(wearable.getLocalNodeId())
                .networkId(networkId)
                .peerAndroidId(androidId)
                .unknown4(3)
                .peerVersion(2)
                .build());
        this.wearable = wearable;
        this.oldConfigNodeId = config.nodeId;
    }

    @Override
    public void onConnect(Connect connect) {
        super.onConnect(connect);
        peerNodeId = connect.id;
        wearable.onConnectReceived(getConnection(), oldConfigNodeId, connect);
        try {
            getConnection().writeMessage(new RootMessage.Builder().syncStart(new SyncStart.Builder()
                    .receivedSeqId(-1L)
                    .version(2)
                    .syncTable(Arrays.asList(
                            new SyncTableEntry.Builder().key("cloud").value(1L).build(),
                            new SyncTableEntry.Builder().key(wearable.getLocalNodeId()).value(wearable.getCurrentSeqId(wearable.getLocalNodeId())).build(), // TODO
                            new SyncTableEntry.Builder().key(peerNodeId).value(wearable.getCurrentSeqId(peerNodeId)).build() // TODO
                    )).build()).build());
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void onDisconnected() {
        Connect connect = getRemoteConnect();
        if (connect == null)
            connect = new Connect.Builder().id(oldConfigNodeId).name("Wear device").build();
        wearable.onDisconnectReceived(getConnection(), connect);
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
                wearable.syncToPeer(peerNodeId, entry.key, entry.value);
                if (wearable.getLocalNodeId().equals(entry.key)) hasLocalNode = true;
            }
        } else {
            Log.d(TAG, "No sync table given.");
        }
        if (!hasLocalNode) wearable.syncToPeer(peerNodeId, wearable.getLocalNodeId(), 0);
    }

    @Override
    public void onSetDataItem(SetDataItem setDataItem) {
        Log.d(TAG, "onSetDataItem: " + setDataItem);
        wearable.putDataItem(DataItemRecord.fromSetDataItem(setDataItem));
    }

    @Override
    public void onRpcRequest(Request rpcRequest) {
        Log.d(TAG, "onRpcRequest: " + rpcRequest);
        if (TextUtils.isEmpty(rpcRequest.targetNodeId) || rpcRequest.targetNodeId.equals(wearable.getLocalNodeId())) {
            int requestId = rpcRequest.requestId + 31 * (rpcRequest.generation + 527);
            String path = rpcRequest.path;
            byte[] data = rpcRequest.rawData != null ? rpcRequest.rawData.toByteArray() : null;
            String sourceNodeId = TextUtils.isEmpty(rpcRequest.sourceNodeId) ? peerNodeId : rpcRequest.sourceNodeId;

            MessageEventParcelable messageEvent = new MessageEventParcelable(requestId, path, data, sourceNodeId);

            sendMessageReceived(rpcRequest.packageName, messageEvent);
        } else if (rpcRequest.targetNodeId.equals(peerNodeId)) {
            // Drop it
        } else {
            // TODO: find next hop
        }
    }

    @Override
    public void onHeartbeat(Heartbeat heartbeat) {
        Log.d(TAG, "onHeartbeat: " + heartbeat);
    }

    @Override
    public void onFilePiece(FilePiece filePiece) {
        Log.d(TAG, "onFilePiece: " + filePiece);
        handleFilePiece(getConnection(), filePiece.fileName, filePiece.piece.toByteArray(), filePiece.finalPiece ? filePiece.digest : null);
    }

    @Override
    public void onChannelRequest(Request channelRequest) {
        Log.d(TAG, "onChannelRequest:" + channelRequest);
    }

    public void handleMessage(WearableConnection connection, String sourceNodeId, RootMessage message) {
        Log.d(TAG, "handleMessage from " + sourceNodeId);

        if (message.heartbeat != null) {
            Log.d(TAG, "Received heartbeat from " + sourceNodeId);
            return;
        }

        if (message.syncStart != null) {
            handleSyncStart(connection, sourceNodeId, message.syncStart);
        }

        if (message.channelRequest != null && wearable.getChannelManager() != null) {
            wearable.getChannelManager().onChannelRequestReceived(connection, sourceNodeId, message.channelRequest);
        }

        if (message.rpcRequest != null) {
            handleRpcRequest(connection, sourceNodeId, message.rpcRequest);
        }

        if (message.setDataItem != null) {
            handleSetDataItem(connection, sourceNodeId, message.setDataItem);
        }

        if (message.filePiece != null) {
            FilePiece piece = message.filePiece;
            handleFilePiece(connection, piece.fileName,
                    piece.piece != null ? piece.piece.toByteArray() : new byte[0], piece.finalPiece ? piece.digest : null);
        }

        if (message.ackAsset != null) {
            Log.d(TAG, "Asset acknowledged: " + message.ackAsset.digest);
        }

        if (message.fetchAsset != null) {
            handleFetchAsset(connection, sourceNodeId, message.fetchAsset);
        }

        if (message.setAsset != null) {
            handleSetAsset(connection, sourceNodeId, message.setAsset, message.hasAsset);
        }
    }

    private void handleSetAsset(WearableConnection connection, String sourceNodeId,
                                SetAsset setAsset, Boolean hasAsset) {
        Log.d(TAG, "handleSetAsset: digest=" + setAsset.digest + ", hasAsset=" + hasAsset);

        if (setAsset.appkeys != null && setAsset.appkeys.appKeys != null &&
                !setAsset.appkeys.appKeys.isEmpty()) {
            for (AppKey appKey : setAsset.appkeys.appKeys) {
                wearable.getNodeDatabase().allowAssetAccess(
                        setAsset.digest,
                        appKey.packageName,
                        appKey.signatureDigest
                );
            }
        }

        boolean assetExistsLocally = wearable.assetFileExists(setAsset.digest);

        if (assetExistsLocally) {
            wearable.getNodeDatabase().markAssetAsPresent(setAsset.digest);
            Log.d(TAG, "Asset already present locally: " + setAsset.digest);
        } else {
            if (setAsset.appkeys != null && setAsset.appkeys.appKeys != null &&
                    !setAsset.appkeys.appKeys.isEmpty()) {
                AppKey firstKey = setAsset.appkeys.appKeys.get(0);
                wearable.getNodeDatabase().markAssetAsMissing(
                        setAsset.digest,
                        firstKey.packageName,
                        firstKey.signatureDigest
                );
            }
        }
    }


    private void handleSyncStart(WearableConnection connection, String sourceNodeId,
                                 org.microg.gms.wearable.proto.SyncStart syncStart) {
        Log.d(TAG, "handleSyncStart from " + sourceNodeId +
                ": receivedSeqId=" + syncStart.receivedSeqId +
                ", version=" + syncStart.version);

        if (syncStart.syncTable != null) {
            for (org.microg.gms.wearable.proto.SyncTableEntry entry : syncStart.syncTable) {
                Log.d(TAG, "  Watch sync state: key=" + entry.key + ", seqId=" + entry.value);
            }
        }

        try {
            List<SyncTableEntry> syncTable = new ArrayList<>();
            syncTable.add(new org.microg.gms.wearable.proto.SyncTableEntry.Builder()
                    .key(wearable.getLocalNodeId())
                    .value(wearable.getClockworkNodePreferences().getNextSeqId() - 1)
                    .build());

            RootMessage response = new RootMessage.Builder()
                    .syncStart(new org.microg.gms.wearable.proto.SyncStart.Builder()
                            .receivedSeqId(syncStart.receivedSeqId)
                            .syncTable(syncTable)
                            .version(2)
                            .build())
                    .build();

            connection.writeMessage(response);
            Log.d(TAG, "Sent SyncStart response");

            if (syncStart.syncTable != null) {
                for (org.microg.gms.wearable.proto.SyncTableEntry entry : syncStart.syncTable) {
                    String nodeId = entry.key;
                    long theirSeqId = entry.value;
                    wearable.syncToPeer(sourceNodeId, nodeId, theirSeqId);
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to respond to syncStart", e);
        }
    }

    private void handleRpcRequest(WearableConnection connection, String sourceNodeId, Request request) {
        Log.d(TAG, "handleRpcRequest from " + sourceNodeId + ": path=" + request.path);

        if (request.rawData != null) {
            MessageEventParcelable messageEvent = new MessageEventParcelable(
                    request.requestId,
                    request.path,
                    request.rawData.toByteArray(),
                    sourceNodeId
            );
            sendMessageReceived(request.packageName, messageEvent);
        }
    }

    private void handleSetDataItem(WearableConnection connection, String sourceNodeId,
                                   SetDataItem setDataItem) {
        Log.d(TAG, "handleSetDataItem from " + sourceNodeId + ": " + setDataItem.uri);

        DataItemRecord record = DataItemRecord.fromSetDataItem(setDataItem);
        record.source = sourceNodeId;

        List<Asset> missingAssets = new ArrayList<>();
        if (setDataItem.assets != null) {
            for (AssetEntry assetEntry : setDataItem.assets) {
                if (assetEntry.value != null && assetEntry.value.digest != null) {
                    String digest = assetEntry.value.digest;
                    if (!wearable.assetFileExists(digest)) {
                        missingAssets.add(Asset.createFromRef(digest));
                    }
                }
            }
        }

        record.assetsAreReady = missingAssets.isEmpty();

        wearable.putDataItem(record);

        if (!missingAssets.isEmpty()) {
            fetchMissingAssets(connection, record, missingAssets);
        }
    }



    private void fetchMissingAssets(WearableConnection connection, DataItemRecord record,
                                    List<Asset> missingAssets) {
        for (Asset asset : missingAssets) {
            try {
                String digest = asset.getDigest();
                Log.d(TAG, "Fetching missing asset: " + digest);

                FetchAsset fetchAsset = new FetchAsset.Builder()
                        .assetName(digest)
                        .packageName(record.packageName)
                        .signatureDigest(record.signatureDigest)
                        .permission(false)
                        .build();

                connection.writeMessage(new RootMessage.Builder()
                        .fetchAsset(fetchAsset)
                        .build());

            } catch (IOException e) {
                Log.w(TAG, "Error fetching asset " + asset.getDigest(), e);
            }
        }
    }


    private void handleFetchAsset(WearableConnection connection, String sourceNodeId,
                                  FetchAsset fetchAsset) {
        Log.d(TAG, "handleFetchAsset: " + fetchAsset.assetName);

        File assetFile = wearable.createAssetFile(fetchAsset.assetName);
        if (assetFile.exists()) {
            try {
                RootMessage announceMessage = new RootMessage.Builder()
                        .setAsset(new SetAsset.Builder()
                                .digest(fetchAsset.assetName)
                                .build())
                        .hasAsset(true)
                        .build();
                connection.writeMessage(announceMessage);

                String fileName = calculateDigest(announceMessage.encode());
                FileInputStream fis = new FileInputStream(assetFile);
                byte[] arr = new byte[12215];
                ByteString lastPiece = null;
                int c;
                while ((c = fis.read(arr)) > 0) {
                    if (lastPiece != null) {
                        connection.writeMessage(new RootMessage.Builder()
                                .filePiece(new FilePiece(fileName, false, lastPiece, null))
                                .build());
                    }
                    lastPiece = ByteString.of(arr, 0, c);
                }
                fis.close();
                connection.writeMessage(new RootMessage.Builder()
                        .filePiece(new FilePiece(fileName, true, lastPiece, fetchAsset.assetName))
                        .build());
            } catch (IOException e) {
                Log.e(TAG, "Failed to send asset", e);
            }
        } else {
            Log.w(TAG, "Asset not found: " + fetchAsset.assetName);
        }
    }

    public void handleFilePiece(WearableConnection connection, String fileName, byte[] bytes, String finalPieceDigest) {
        File file = wearable.createAssetReceiveTempFile(fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(bytes);
            fos.close();
        } catch (IOException e) {
            Log.w(TAG, "Error writing file piece", e);
        }

        if (finalPieceDigest == null) {
            return;
        }

        // This is a final piece. If digest matches we're so happy!
        try {
            String digest = calculateDigest(Utils.readStreamToEnd(new FileInputStream(file)));

            if (!digest.equals(finalPieceDigest)) {
                Log.w(TAG, "Digest mismatch: expected=" + finalPieceDigest +
                        ", actual=" + digest + ". Deleting temp file.");
                file.delete();
                return;
            }

            File targetFile = wearable.createAssetFile(digest);
            if (!file.renameTo(targetFile)) {
                Log.w(TAG, "Failed to rename temp file to target. Deleting temp file.");
                file.delete();
                return;
            }

            Log.d(TAG, "Asset saved successfully: " + digest);

            try {
                connection.writeMessage(new RootMessage.Builder()
                        .ackAsset(new AckAsset(digest))
                        .build());
            } catch (IOException e) {
                Log.w(TAG, "Failed to send asset ACK", e);
            }

            synchronized (wearable.getNodeDatabase()) {
                wearable.getNodeDatabase().markAssetAsPresent(digest);

                Cursor cursor = wearable.getNodeDatabase().getDataItemsWaitingForAsset(digest);
                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            DataItemRecord record = DataItemRecord.fromCursor(cursor);

                            boolean allPresent = true;
                            for (Asset asset : record.dataItem.getAssets().values()) {
                                if (!wearable.assetFileExists(asset.getDigest())) {
                                    allPresent = false;
                                    break;
                                }
                            }

                            if (allPresent && !record.assetsAreReady) {
                                Log.d(TAG, "All assets now ready for: " + record.dataItem.uri);

                                record.assetsAreReady = true;
                                wearable.getNodeDatabase().updateAssetsReady(
                                        record.dataItem.uri.toString(), true);

                                Intent intent = new Intent("com.google.android.gms.wearable.DATA_CHANGED");
                                intent.setPackage(record.packageName);
                                intent.setData(record.dataItem.uri);
                                wearable.invokeListeners(intent,
                                        listener -> listener.onDataChanged(record.toEventDataHolder()));
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Error processing final file piece", e);
            file.delete();
        }
    }

    public void sendMessageReceived(String packageName, MessageEventParcelable messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent);
        Intent intent = new Intent("com.google.android.gms.wearable.MESSAGE_RECEIVED");
        intent.setPackage(packageName);
        intent.setData(Uri.parse("wear://" + wearable.getLocalNodeId() + "/" + messageEvent.getPath()));
        wearable.invokeListeners(intent, listener -> listener.onMessageReceived(messageEvent));
    }
}
