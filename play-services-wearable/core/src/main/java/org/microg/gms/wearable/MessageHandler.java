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
import static org.microg.gms.wearable.WearableImpl.ROLE_CLIENT;
import static org.microg.gms.wearable.WearableImpl.ROLE_SERVER;

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
import org.microg.gms.wearable.proto.ControlMessage;
import org.microg.gms.wearable.proto.EncryptionHandshake;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okio.ByteString;

public class MessageHandler extends ServerMessageListener {
    private static final String TAG = "WearMessageHandler";
    private final WearableImpl wearable;
    private final String oldConfigNodeId;
    private String peerNodeId;
    private final ConnectionConfiguration config;

    private final AccountMatching accountMatching;

    public MessageHandler(Context ctx, WearableImpl wearable, ConnectionConfiguration config) {
        super(buildConnect(ctx, wearable, config));
        this.wearable = wearable;
        this.config = config;
        this.oldConfigNodeId = config.nodeId;
        this.peerNodeId = config.peerNodeId;
        this.accountMatching = new AccountMatching(wearable);
    }

    @Override
    public void onConnect(Connect connect) {
        super.onConnect(connect);
        peerNodeId = connect.id;
        Log.d(TAG, "onConnect: " + connect);

        if (config.migrating) {
            Log.d(TAG, "config.migrating...");
            if (!Boolean.TRUE.equals(connect.migrating)) {
                Log.e(TAG, "Migration state mismatch: local=true, peer=false for node "
                        + peerNodeId + ". Aborting.");
                try { getConnection().close(); } catch (IOException ignored) {}
                return;
            }

            if (config.role == ROLE_CLIENT) {
                String migratingFrom = connect.migratingFromNodeId;
                if (TextUtils.isEmpty(migratingFrom)) {
                    Log.e(TAG, "Attempting to migrate but Connect is missing migratingFromNodeId");
                    try { getConnection().close(); } catch (IOException ignored) {}
                    return;
                }
                Log.i(TAG, "Starting migration: node=" + peerNodeId
                        + " migratingFrom=" + migratingFrom);
                wearable.startNodeMigration(peerNodeId, migratingFrom);
            }
        } else if (Boolean.TRUE.equals(connect.migrating)) {
            Log.e(TAG, "Migration state mismatch: local=false, peer=true for node "
                    + peerNodeId + ". Aborting.");
            try { getConnection().close(); } catch (IOException ignored) {}
            return;
        }

        if (config.role == ROLE_SERVER) {
            String storedPeerId = wearable.getClockworkNodePreferences().getPeerNodeId();
            if (storedPeerId == null) {
                Log.i(TAG, "onConnect: first pairing, storing peerNodeId=" + peerNodeId);
                wearable.getClockworkNodePreferences().setPeerNodeId(peerNodeId);
            } else if (!storedPeerId.equals(peerNodeId) && !config.migrating) {
                Log.w(TAG, "onConnect: mismatched peerNodeId: stored=" + storedPeerId +
                        " incoming=" + peerNodeId + " — rejecting");
                try {
                    getConnection().close();
                } catch (IOException ignored) {}
                return;
            }
        }

        if (!wearable.getActiveConnections().containsKey(connect.id)) {
            wearable.onConnectReceived(getConnection(), oldConfigNodeId, connect);
        } else {
            Log.d(TAG, "onConnect: connection already registered for " + connect.id + ", skipping onConnectReceived");
        }
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected");
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
        Log.d(TAG, "onSyncStart from " + peerNodeId + ": version=" + syncStart.version);
        DataTransport dt = wearable.getDataTransport(peerNodeId);
        if (dt != null) {
            dt.respondToSyncStart(syncStart);
        } else {
            Log.w(TAG, "onSyncStart: no DataTransport for " + peerNodeId);
//            if (syncStart.syncTable != null) {
//                for (SyncTableEntry e : syncStart.syncTable) {
//                    wearable.syncToPeer(peerNodeId, e.key, e.value);
//                }
//            }
        }
    }

    @Override
    public void onSetDataItem(SetDataItem setDataItem) {
        Log.d(TAG, "onSetDataItem: " + setDataItem);
        wearable.putDataItem(DataItemRecord.fromSetDataItem(setDataItem));
    }

    @Override
    public void onRpcRequest(Request rpcRequest) {
        Log.d(TAG, "onRpcRequest: " + rpcRequest);

        if (rpcRequest.request != null) {
            if (wearable.getChannelManager() != null) {
                wearable.getChannelManager().onChannelRequestReceived(getConnection(), peerNodeId, rpcRequest);
            }
            return;
        }

        if (TextUtils.isEmpty(rpcRequest.targetNodeId) || rpcRequest.targetNodeId.equals(wearable.getLocalNodeId())) {
            int requestId = rpcRequest.requestId != null ? rpcRequest.requestId : 0;
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
    public void onRpcWithResponseId(Request rpcWithResponseId) {
        Log.d(TAG, "onRpcWithResponseId: " + rpcWithResponseId);
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
        if (wearable.getChannelManager() != null) {
            wearable.getChannelManager().onChannelRequestReceived(getConnection(), peerNodeId, channelRequest);
        }
    }

    @Override
    public void onEncryptionHandshake(EncryptionHandshake encryptionHandshake) {
        Log.d(TAG, "onChannelRequest:" + encryptionHandshake);
    }

    @Override
    public void onControlMessage(ControlMessage controlMessage) {
        dispatchControlMessage(getConnection(), peerNodeId, controlMessage);
    }

    public void handleMessage(WearableConnection connection, String sourceNodeId, RootMessage message) {
        Log.d(TAG, "handleMessage from " + sourceNodeId);

        if (message.heartbeat != null) {
            Log.d(TAG, "Received heartbeat from " + sourceNodeId);
            return;
        }

        if (message.controlMessage != null) {
            Log.d(TAG, "handleMessage: controlMessage from " + sourceNodeId);
            dispatchControlMessage(getConnection(), peerNodeId, message.controlMessage);
            return;
        }

        if (message.syncStart != null) {
            Log.d(TAG, "message.syncStart...");
            handleSyncStart(connection, sourceNodeId, message.syncStart);
        }

        if (message.channelRequest != null && wearable.getChannelManager() != null) {
            Log.d(TAG, "message.channelRequest...");
            wearable.getChannelManager().onChannelRequestReceived(connection, sourceNodeId, message.channelRequest);
        }

        if (message.rpcRequest != null) {
            Log.d(TAG, "message.rpcRequest...");
            handleRpcRequest(connection, sourceNodeId, message.rpcRequest);
        }

        if (message.setDataItem != null) {
            Log.d(TAG, "message.setDataItem...");
            handleSetDataItem(connection, sourceNodeId, message.setDataItem);
        }

        if (message.filePiece != null) {
            Log.d(TAG, "message.filePiece...");
            FilePiece piece = message.filePiece;
            handleFilePiece(connection, piece.fileName,
                    piece.piece != null ? piece.piece.toByteArray() : new byte[0], piece.finalPiece ? piece.digest : null);
        }

        if (message.ackAsset != null) {
            wearable.getAssetManager().onAckAsset(message.ackAsset.digest);
        }

        if (message.fetchAsset != null) {
            wearable.getAssetManager().handleFetchAsset(connection, sourceNodeId, message.fetchAsset);
        }

        if (message.setAsset != null) {
            wearable.getAssetManager().onAssetReceived(message.setAsset.digest);
        }
    }

    private void dispatchControlMessage(WearableConnection connection,
                                        String sourceNodeId, ControlMessage ctrl) {
        if (ctrl == null || ctrl.type == null) return;

        Log.d(TAG, "dispatchControlMessage: type=" + ctrl.type + " from=" + sourceNodeId);

        switch (ctrl.type) {
            case NodeMigrationController.CTRL_TERMINATE_ASSOCIATION:
                Log.i(TAG, "dispatchControlMessage: TERMINATE_ASSICUATION from " + sourceNodeId);
                wearable.terminateAssociation(sourceNodeId, false, "peer requested");
                break;

            case NodeMigrationController.CTRL_SUSPEND_SYNC:
                Log.i(TAG, "dispatchControlMessage: SUSPENDED_SYNC from " + sourceNodeId);
                wearable.getMigrationController().suspendNode(sourceNodeId);
                break;

            case NodeMigrationController.CTRL_RESUME_SYNC:
                Log.i(TAG, "dispatchControlMessage: RESUME_SYNC from " + sourceNodeId);
                wearable.getMigrationController().resumeNode(sourceNodeId);
                wearable.triggerResync(sourceNodeId);
                break;

            case NodeMigrationController.CTRL_MIGRATION_FAILED:
                Log.w(TAG, "dispatchControlMessage: MIGRATION_FAILED from " + sourceNodeId);
                wearable.onMigrationFailed(sourceNodeId, false);
                break;

            case NodeMigrationController.CTRL_ACCOUNT_MATCHING:
                accountMatching.handleControlMessage(connection, sourceNodeId, ctrl);
                break;

            case NodeMigrationController.CTRL_MIGRATION_CANCELLED:
                Log.w(TAG, "dispatchControlMessage: MIGRATION_CANCELLED from " + sourceNodeId);
                wearable.onMigrationFailed(sourceNodeId, false);
                break;

            default:
                Log.w(TAG, "dispatchControlMessage: Unknown control message type=" + ctrl.type
                                + " from=" + sourceNodeId);
                break;
        }
    }

    private void handleSetAsset(WearableConnection connection, String sourceNodeId,
                                SetAsset setAsset, Boolean hasAsset) {
        Log.d(TAG, "handleSetAsset: digest=" + setAsset.digest +
                ", hasAsset=" + hasAsset);


        boolean hasAppKeys = setAsset.appkeys != null &&
                setAsset.appkeys.appKeys != null &&
                !setAsset.appkeys.appKeys.isEmpty();

        if (!hasAppKeys) {
            Log.w(TAG, "SetAsset missing AppKeys for digest: " + setAsset.digest);
        }

        if (hasAppKeys) {
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
            wearable.getAssetFetcher().onAssetReceived(setAsset.digest);
            Log.d(TAG, "Asset already present locally: " + setAsset.digest);
        } else {
            if (hasAppKeys) {
                AppKey firstKey = setAsset.appkeys.appKeys.get(0);
                wearable.getNodeDatabase().markAssetAsMissing(
                        setAsset.digest,
                        firstKey.packageName,
                        firstKey.signatureDigest
                );
            } else {
                wearable.getNodeDatabase().markAssetAsMissing(
                        setAsset.digest,
                        "*",
                        "*"
                );
            }
        }
    }


    private void handleSyncStart(WearableConnection connection, String sourceNodeId,
                                 org.microg.gms.wearable.proto.SyncStart syncStart) {
        Log.d(TAG, "handleSyncStart from " + sourceNodeId +
                ": receivedSeqId=" + syncStart.receivedSeqId +
                ", version=" + syncStart.version);

        DataTransport dt = wearable.getDataTransport(sourceNodeId);
        if(dt != null) {
            dt.respondToSyncStart(syncStart);
        } else {
            Log.w(TAG, "onSyncStart: no DataTransport for " + sourceNodeId);
//            if(syncStart.syncTable != null) {
//                for (SyncTableEntry e : syncStart.syncTable) {
//                    wearable.syncToPeer(sourceNodeId, e.key, e.value);
//                }
//            }
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
        wearable.getAssetFetcher().fetchMissingAssetsForRecord(connection, record, missingAssets);
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
                wearable.getAssetFetcher().onAssetReceived(digest);

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

    private static Connect buildConnect(Context ctx, WearableImpl wearable,
                                        ConnectionConfiguration config) {
        long androidId = SettingsContract.getSettings(ctx,
                SettingsContract.CheckIn.INSTANCE.getContentUri(ctx),
                new String[]{SettingsContract.CheckIn.ANDROID_ID},
                c -> c.getLong(0));

        Connect.Builder b = new Connect.Builder()
                .name(Build.MODEL) // TODO: Should be hostname, but seems to be irrelevant
                .id(wearable.getLocalNodeId())
                .networkId(config.nodeId)
                .peerAndroidId(androidId)
                .unknown4(3)
                .peerVersion(2)
                .peerMinimumVersion(0)
                .androidSdkVersion(Build.VERSION.SDK_INT);

        if (config.migrating && config.role == ROLE_SERVER) {
            String prevPeerNodeId = wearable.getClockworkNodePreferences().getPeerNodeId();
            b.migrating(true);
            if (prevPeerNodeId != null) {
                Log.i(TAG, "Migration handshake: migratingFromNodeId=" + prevPeerNodeId);
                b.migratingFromNodeId(prevPeerNodeId);
            } else {
                Log.w(TAG, "Migration requested but no previous peer nodeId stored");
            }
        }

        return b.build();
    }

}
