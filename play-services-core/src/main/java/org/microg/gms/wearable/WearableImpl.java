/*
 * Copyright 2013-2016 microG Project Team
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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.PutDataRequest;

import org.microg.gms.common.MultiListenerProxy;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.Utils;
import org.microg.wearable.SocketConnectionThread;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.AckAsset;
import org.microg.wearable.proto.AppKey;
import org.microg.wearable.proto.AppKeys;
import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.FilePiece;
import org.microg.wearable.proto.RootMessage;
import org.microg.wearable.proto.SetAsset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okio.ByteString;

public class WearableImpl {

    private static final String TAG = "GmsWear";

    private static final int WEAR_TCP_PORT = 5601;

    private final Context context;
    private final NodeDatabaseHelper nodeDatabase;
    private final ConfigurationDatabaseHelper configDatabase;
    private final Map<String, List<IWearableListener>> listeners = new HashMap<String, List<IWearableListener>>();
    private final Set<Node> connectedNodes = new HashSet<Node>();
    private final Set<WearableConnection> activeConnections = new HashSet<WearableConnection>();
    private SocketConnectionThread sct;
    private ConnectionConfiguration[] configurations;
    private boolean configurationsUpdated = false;
    private ClockworkNodePreferences clockworkNodePreferences;

    public WearableImpl(Context context, NodeDatabaseHelper nodeDatabase, ConfigurationDatabaseHelper configDatabase) {
        this.context = context;
        this.nodeDatabase = nodeDatabase;
        this.configDatabase = configDatabase;
        this.clockworkNodePreferences = new ClockworkNodePreferences(context);
    }

    public String getLocalNodeId() {
        return clockworkNodePreferences.getLocalNodeId();
    }

    public DataItemRecord putDataItem(String packageName, String signatureDigest, String source, DataItemInternal dataItem) {
        DataItemRecord record = new DataItemRecord();
        record.packageName = packageName;
        record.signatureDigest = signatureDigest;
        record.deleted = false;
        record.source = source;
        record.dataItem = dataItem;
        record.v1SeqId = clockworkNodePreferences.getNextSeqId();
        if (record.source.equals(getLocalNodeId())) record.seqId = record.v1SeqId;
        nodeDatabase.putRecord(record);
        return record;
    }

    public DataItemRecord putDataItem(DataItemRecord record) {
        nodeDatabase.putRecord(record);
        try {
            if (listeners.containsKey(record.packageName)) {
                MultiListenerProxy.get(IWearableListener.class, listeners.get(record.packageName)).onDataChanged(getDataItemForRecord(record));
            } else {

            }
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
        return record;
    }

    private Asset prepareAsset(String packageName, Asset asset) {
        if (asset.getFd() != null && asset.data == null) {
            try {
                asset.data = Utils.readStreamToEnd(new FileInputStream(asset.getFd().getFileDescriptor()));
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }
        if (asset.data != null) {
            String digest = calculateDigest(asset.data);
            File assetFile = createAssetFile(digest);
            boolean success = assetFile.exists();
            if (!success) {
                File tmpFile = new File(assetFile.getParent(), assetFile.getName() + ".tmp");

                try {
                    FileOutputStream stream = new FileOutputStream(tmpFile);
                    stream.write(asset.data);
                    stream.close();
                    success = tmpFile.renameTo(assetFile);
                } catch (IOException e) {
                    Log.w(TAG, e);
                }
            }
            if (success) {
                Log.d(TAG, "Successfully created asset file " + assetFile);
                return Asset.createFromRef(digest);
            } else {
                Log.w(TAG, "Failed creating asset file " + assetFile);
            }
        }
        return null;
    }

    private File createAssetFile(String digest) {
        File dir = new File(new File(context.getFilesDir(), "assets"), digest.substring(digest.length() - 2));
        dir.mkdirs();
        return new File(dir, digest + ".asset");
    }

    private File createAssetReceiveTempFile(String name) {
        File dir = new File(context.getFilesDir(), "piece");
        dir.mkdirs();
        return new File(dir, name);
    }

    private String calculateDigest(byte[] data) {
        try {
            return Base64.encodeToString(MessageDigest.getInstance("SHA1").digest(data), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ConnectionConfiguration[] getConfigurations() {
        if (configurations == null) {
            configurations = configDatabase.getAllConfigurations();
        }
        if (configurationsUpdated) {
            configurationsUpdated = false;
            ConnectionConfiguration[] newConfigurations = configDatabase.getAllConfigurations();
            for (ConnectionConfiguration configuration : configurations) {
                for (ConnectionConfiguration newConfiguration : newConfigurations) {
                    if (newConfiguration.name.equals(configuration.name)) {
                        newConfiguration.connected = configuration.connected;
                        newConfiguration.peerNodeId = configuration.peerNodeId;
                        break;
                    }
                }
            }
            configurations = newConfigurations;
        }
        Log.d(TAG, "Configurations reported: " + Arrays.toString(configurations));
        return configurations;
    }

    private void addConnectedNode(Node node) {
        connectedNodes.add(node);
        onConnectedNodes(getConnectedNodesParcelableList());
    }

    private void removeConnectedNode(String nodeId) {
        for (Node connectedNode : new ArrayList<Node>(connectedNodes)) {
            if (connectedNode.getId().equals(nodeId))
                connectedNodes.remove(connectedNode);
        }
        onConnectedNodes(getConnectedNodesParcelableList());
    }


    public Context getContext() {
        return context;
    }

    public void syncToPeer(WearableConnection connection, String nodeId, long seqId) {
        Log.d(TAG, "-- Start syncing over " + connection + ", nodeId " + nodeId + " starting with seqId " + seqId);
        Cursor cursor = nodeDatabase.getModifiedDataItems(nodeId, seqId, true);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (!syncRecordToPeer(connection, DataItemRecord.fromCursor(cursor))) break;
            }
            cursor.close();
        }
        Log.d(TAG, "-- Done syncing over " + connection + ", nodeId " + nodeId + " starting with seqId " + seqId);
    }


    private void syncRecordToAll(DataItemRecord record) {
        Log.d(TAG, "Syncing record " + record + " over " + activeConnections.size() + " connections.");
        for (WearableConnection connection : new ArrayList<WearableConnection>(activeConnections)) {
            if (!syncRecordToPeer(connection, record)) {
                Log.d(TAG, "Removing connection as it seems not usable: " + connection);
                activeConnections.remove(connection);
            }
        }
    }

    private boolean syncRecordToPeer(WearableConnection connection, DataItemRecord record) {
        for (Asset asset : record.dataItem.getAssets().values()) {
            syncAssetToPeer(connection, record, asset);
        }
        Log.d(TAG, "Sync over " + connection + ": " + record);

        try {
            connection.writeMessage(new RootMessage.Builder().setDataItem(record.toSetDataItem()).build());
        } catch (IOException e) {
            Log.w(TAG, e);
            return false;
        }
        return true;
    }

    private void syncAssetToPeer(WearableConnection connection, DataItemRecord record, Asset asset) {
        try {
            Log.d(TAG, "Sync over " + connection + ": " + asset);
            RootMessage announceMessage = new RootMessage.Builder().setAsset(new SetAsset.Builder()
                    .digest(asset.getDigest())
                    .appkeys(new AppKeys(Collections.singletonList(new AppKey(record.packageName, record.signatureDigest))))
                    .build()).hasAsset(true).build();
            connection.writeMessage(announceMessage);
            File assetFile = createAssetFile(asset.getDigest());
            String fileName = calculateDigest(announceMessage.toByteArray());
            FileInputStream fis = new FileInputStream(assetFile);
            byte[] arr = new byte[12215];
            ByteString lastPiece = null;
            int c = 0;
            while ((c = fis.read(arr)) > 0) {
                if (lastPiece != null) {
                    Log.d(TAG, "Sync over " + connection + ": Asset piece for fileName " + fileName + ": " + lastPiece);
                    connection.writeMessage(new RootMessage.Builder().filePiece(new FilePiece(fileName, false, lastPiece, null)).build());
                }
                lastPiece = ByteString.of(arr, 0, c);
            }
            Log.d(TAG, "Sync over " + connection + ": Last asset piece for fileName " + fileName + ": " + lastPiece);
            connection.writeMessage(new RootMessage.Builder().filePiece(new FilePiece(fileName, true, lastPiece, asset.getDigest())).build());
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    public void addAssetToDatabase(Asset asset, List<AppKey> appKeys) {
        nodeDatabase.putAsset(asset, false);
        for (AppKey appKey : appKeys) {
            nodeDatabase.allowAssetAccess(asset.getDigest(), appKey.packageName, appKey.signatureDigest);
        }
    }

    public long getCurrentSeqId(String nodeId) {
        return nodeDatabase.getCurrentSeqId(nodeId);
    }

    public void handleFilePiece(WearableConnection connection, String fileName, byte[] bytes, String finalPieceDigest) {
        File file = createAssetReceiveTempFile(fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(bytes);
            fos.close();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        if (finalPieceDigest != null) {
            // This is a final piece. If digest matches we're so happy!
            try {
                String digest = calculateDigest(Utils.readStreamToEnd(new FileInputStream(file)));
                if (digest.equals(finalPieceDigest)) {
                    if (file.renameTo(createAssetFile(digest))) {
                        // TODO: Mark as stored in db
                        connection.writeMessage(new RootMessage.Builder().ackAsset(new AckAsset(digest)).build());
                    } else {
                        Log.w(TAG, "Could not rename to target file name. delete=" + file.delete());
                    }
                } else {
                    Log.w(TAG, "Received digest does not match. delete=" + file.delete());
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed working with temp file. delete=" + file.delete(), e);
            }
        }
    }

    public void onConnectReceived(WearableConnection connection, String nodeId, Connect connect) {
        for (ConnectionConfiguration config : getConfigurations()) {
            if (config.nodeId.equals(nodeId)) {
                config.peerNodeId = connect.id;
                config.connected = true;
            }
        }
        Log.d(TAG, "Adding connection to list of open connections: " + connection);
        activeConnections.add(connection);
        onPeerConnected(new NodeParcelable(connect.id, connect.name));
    }

    public void onDisconnectReceived(WearableConnection connection, String nodeId, Connect connect) {
        for (ConnectionConfiguration config : getConfigurations()) {
            if (config.nodeId.equals(nodeId)) {
                config.connected = false;
            }
        }
        Log.d(TAG, "Removing connection from list of open connections: " + connection);
        activeConnections.remove(connection);
        onPeerDisconnected(new NodeParcelable(connect.id, connect.name));
    }

    public List<NodeParcelable> getConnectedNodesParcelableList() {
        List<NodeParcelable> nodes = new ArrayList<NodeParcelable>();
        for (Node connectedNode : connectedNodes) {
            nodes.add(new NodeParcelable(connectedNode));
        }
        return nodes;
    }

    public IWearableListener getAllListeners() {
        return MultiListenerProxy.get(IWearableListener.class, new MultiListenerProxy.MultiCollectionListenerPool<IWearableListener>(listeners.values()));
    }

    public void onPeerConnected(NodeParcelable node) {
        Log.d(TAG, "onPeerConnected: " + node);
        try {
            getAllListeners().onPeerConnected(node);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
        addConnectedNode(node);
    }

    public void onPeerDisconnected(NodeParcelable node) {
        Log.d(TAG, "onPeerDisconnected: " + node);
        try {
            getAllListeners().onPeerDisconnected(node);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
        removeConnectedNode(node.getId());
    }

    public void onConnectedNodes(List<NodeParcelable> nodes) {
        Log.d(TAG, "onConnectedNodes: " + nodes);
        try {
            getAllListeners().onConnectedNodes(nodes);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
    }

    public DataItemRecord putData(PutDataRequest request, String packageName) {
        String host = request.getUri().getHost();
        if (TextUtils.isEmpty(host)) host = getLocalNodeId();
        DataItemInternal dataItem = new DataItemInternal(host, request.getUri().getPath());
        for (Map.Entry<String, Asset> assetEntry : request.getAssets().entrySet()) {
            Asset asset = prepareAsset(packageName, assetEntry.getValue());
            if (asset != null) {
                nodeDatabase.putAsset(asset, true);
                dataItem.addAsset(assetEntry.getKey(), asset);
            }
        }
        dataItem.data = request.getData();
        DataItemRecord record = putDataItem(packageName, PackageUtils.firstSignatureDigest(context, packageName), getLocalNodeId(), dataItem);
        syncRecordToAll(record);
        return record;
    }

    public DataHolder getDataItems(String packageName) {
        Cursor dataHolderItems = nodeDatabase.getDataItemsForDataHolder(packageName, PackageUtils.firstSignatureDigest(context, packageName));
        while (dataHolderItems.moveToNext()) {
            Log.d(TAG, "getDataItems[]: path=" + Uri.parse(dataHolderItems.getString(1)).getPath());
        }
        dataHolderItems.moveToFirst();
        dataHolderItems.moveToPrevious();
        return DataHolder.fromCursor(dataHolderItems, 0, null);
    }

    public DataHolder getDataItemsByUri(Uri uri, String packageName) {
        String firstSignature;
        try {
            firstSignature = PackageUtils.firstSignatureDigest(context, packageName);
        } catch (Exception e) {
            return null;
        }
        Cursor dataHolderItems = nodeDatabase.getDataItemsForDataHolderByHostAndPath(packageName, firstSignature, uri.getHost(), uri.getPath());
        while (dataHolderItems.moveToNext()) {
            Log.d(TAG, "getDataItems[]: path=" + Uri.parse(dataHolderItems.getString(1)).getPath());
        }
        dataHolderItems.moveToFirst();
        dataHolderItems.moveToPrevious();
        return DataHolder.fromCursor(dataHolderItems, 0, null);
    }

    public DataHolder getDataItemForRecord(DataItemRecord record) {
        Cursor dataHolderItems = nodeDatabase.getDataItemsForDataHolderByHostAndPath(record.packageName, record.signatureDigest, record.dataItem.uri.getHost(), record.dataItem.uri.getPath());
        while (dataHolderItems.moveToNext()) {
            Log.d(TAG, "getDataItems[]: path=" + Uri.parse(dataHolderItems.getString(1)).getPath());
        }
        dataHolderItems.moveToFirst();
        dataHolderItems.moveToPrevious();
        return DataHolder.fromCursor(dataHolderItems, 0, null);
    }

    public synchronized void addListener(String packageName, IWearableListener listener) {
        if (!listeners.containsKey(packageName)) {
            listeners.put(packageName, new ArrayList<IWearableListener>());
        }
        listeners.get(packageName).add(listener);
    }

    public void removeListener(IWearableListener listener) {
        for (List<IWearableListener> list : listeners.values()) {
            list.remove(listener);
        }
    }

    public void enableConnection(String name) {
        configDatabase.setEnabledState(name, true);
        configurationsUpdated = true;
        if (name.equals("server") && sct == null) {
            (sct = SocketConnectionThread.serverListen(WEAR_TCP_PORT, new MessageHandler(this, configDatabase.getConfiguration(name)))).start();
        }
    }

    public void disableConnection(String name) {
        configDatabase.setEnabledState(name, false);
        configurationsUpdated = true;
        if (name.equals("server") && sct != null) {
            activeConnections.remove(sct.getWearableConnection());
            sct.close();
            sct.interrupt();
            sct = null;
        }
    }

    public void deleteConnection(String name) {
        configDatabase.deleteConfiguration(name);
        configurationsUpdated = true;
    }

    public void createConnection(ConnectionConfiguration config) {
        if (config.nodeId == null) config.nodeId = getLocalNodeId();
        Log.d(TAG, "putConfig[nyp]: " + config);
        configDatabase.putConfiguration(config);
        configurationsUpdated = true;
    }

    public int deleteDataItems(Uri uri, String packageName) {
        List<DataItemRecord> records = nodeDatabase.deleteDataItems(packageName, PackageUtils.firstSignatureDigest(context, packageName), uri.getHost(), uri.getPath());
        for (DataItemRecord record : records) {
            syncRecordToAll(record);
        }
        return records.size();
    }

    public void sendMessageReceived(MessageEventParcelable messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent);
        try {
            getAllListeners().onMessageReceived(messageEvent);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
    }

    public DataItemRecord getDataItemByUri(Uri uri, String packageName) {
        Cursor cursor = nodeDatabase.getDataItemsForDataHolderByHostAndPath(packageName, PackageUtils.firstSignatureDigest(context, packageName), uri.getHost(), uri.getPath());
        DataItemRecord record = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                record = DataItemRecord.fromCursor(cursor);
            }
            cursor.close();
        }
        return record;
    }
}
