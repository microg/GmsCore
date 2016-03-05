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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.AmsEntityUpdateParcelable;
import com.google.android.gms.wearable.internal.AncsNotificationParcelable;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.DataItemParcelable;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetConfigResponse;
import com.google.android.gms.wearable.internal.GetConfigsResponse;
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetLocalNodeResponse;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.IWearableService;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;

import org.microg.gms.common.PackageUtils;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.AssetEntry;
import org.microg.wearable.proto.RootMessage;
import org.microg.wearable.proto.SetDataItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import okio.ByteString;

public class WearableServiceImpl extends IWearableService.Stub implements IWearableListener {
    private static final String TAG = "GmsWearSvcImpl";

    private static final String CLOCKWORK_NODE_PREFERENCES = "cw_node";
    private static final String CLOCKWORK_NODE_PREFERENCE_NODE_ID = "node_id";
    private static final String CLOCKWORK_NODE_PREFERENCE_NEXT_SEQ_ID_BLOCK = "nextSeqIdBlock";

    private final Context context;
    private final String packageName;
    private final NodeDatabaseHelper nodeDatabase;
    private final ConfigurationDatabaseHelper configDatabase;
    private Set<IWearableListener> listeners = new HashSet<IWearableListener>();
    private Set<Node> connectedNodes = new HashSet<Node>();
    private ConnectionConfiguration[] configurations;
    private boolean configurationsUpdated = false;
    private NetworkConnectionThread nct;

    private long seqIdBlock;
    private long seqIdInBlock = -1;

    public WearableServiceImpl(Context context, NodeDatabaseHelper nodeDatabase, ConfigurationDatabaseHelper configDatabase, String packageName) {
        this.context = context;
        this.nodeDatabase = nodeDatabase;
        this.configDatabase = configDatabase;
        this.packageName = packageName;
    }

    public String getLocalNodeId() {
        SharedPreferences preferences = context.getSharedPreferences(CLOCKWORK_NODE_PREFERENCES, Context.MODE_PRIVATE);
        String nodeId = preferences.getString(CLOCKWORK_NODE_PREFERENCE_NODE_ID, null);
        if (nodeId == null) {
            nodeId = UUID.randomUUID().toString();
            preferences.edit().putString(CLOCKWORK_NODE_PREFERENCE_NODE_ID, nodeId).apply();
        }
        return nodeId;
    }

    private synchronized long getNextSeqId() {
        SharedPreferences preferences = context.getSharedPreferences(CLOCKWORK_NODE_PREFERENCES, Context.MODE_PRIVATE);
        if (seqIdInBlock < 0) seqIdInBlock = 1000;
        if (seqIdInBlock >= 1000) {
            seqIdBlock = preferences.getLong(CLOCKWORK_NODE_PREFERENCE_NEXT_SEQ_ID_BLOCK, 100);
            preferences.edit().putLong(CLOCKWORK_NODE_PREFERENCE_NEXT_SEQ_ID_BLOCK, seqIdBlock + seqIdInBlock).apply();
            seqIdInBlock = 0;
        }
        return seqIdBlock + seqIdInBlock++;
    }

    @Override
    public void putData(IWearableCallbacks callbacks, PutDataRequest request) throws RemoteException {
        Log.d(TAG, "putData: " + request.toString(true));
        String host = request.getUri().getHost();
        if (TextUtils.isEmpty(host)) host = getLocalNodeId();
        DataItemInternal dataItem = new DataItemInternal(host, request.getUri().getPath());
        for (Map.Entry<String, Asset> assetEntry : request.getAssets().entrySet()) {
            Asset asset = prepareAsset(packageName, assetEntry.getValue());
            if (asset != null) {
                dataItem.addAsset(assetEntry.getKey(), asset);
            }
        }
        dataItem.data = request.getData();
        DataItemParcelable parcelable = putDataItem(packageName,
                PackageUtils.firstSignatureDigest(context, packageName), getLocalNodeId(), dataItem).toParcelable();
        callbacks.onPutDataResponse(new PutDataResponse(0, parcelable));
    }

    public DataItemRecord putDataItem(String packageName, String signatureDigest, String source, DataItemInternal dataItem) {
        DataItemRecord record = new DataItemRecord();
        record.packageName = packageName;
        record.signatureDigest = signatureDigest;
        record.deleted = false;
        record.source = source;
        record.dataItem = dataItem;
        record.v1SeqId = getNextSeqId();
        if (record.source.equals(getLocalNodeId())) record.seqId = record.v1SeqId;
        return putDataItem(record);
    }

    public DataItemRecord putDataItem(DataItemRecord record) {
        nodeDatabase.putRecord(record);
        return record;
    }

    private Asset prepareAsset(String packageName, Asset asset) {
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

    private String calculateDigest(byte[] data) {
        try {
            return Base64.encodeToString(MessageDigest.getInstance("SHA1").digest(data), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getDataItem(IWearableCallbacks callbacks, Uri uri) throws RemoteException {
        Log.d(TAG, "getDataItem: " + uri);
        Cursor cursor = nodeDatabase.getDataItemsForDataHolderByHostAndPath(packageName, PackageUtils.firstSignatureDigest(context, packageName), uri.getHost(), uri.getPath());
        if (cursor != null) {
            if (cursor.moveToNext()) {
                DataItemParcelable dataItem = new DataItemParcelable(new Uri.Builder().scheme("wear").authority(cursor.getString(0)).path(cursor.getString(1)).build());
                dataItem.data = cursor.getBlob(2);
                Log.d(TAG, "getDataItem.asset " + cursor.getString(5));
                // TODO: assets
                callbacks.onGetDataItemResponse(new GetDataItemResponse(0, dataItem));
            }
            cursor.close();
        }
        // TODO: negative
    }

    @Override
    public void getDataItems(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getDataItems: " + callbacks);
        Cursor dataHolderItems = nodeDatabase.getDataItemsForDataHolder(packageName, PackageUtils.firstSignatureDigest(context, packageName));
        while (dataHolderItems.moveToNext()) {
            Log.d(TAG, "getDataItems[]: path=" + Uri.parse(dataHolderItems.getString(1)).getPath());
        }
        dataHolderItems.moveToFirst();
        dataHolderItems.moveToPrevious();
        callbacks.onDataHolder(DataHolder.fromCursor(dataHolderItems, 0, null));
    }

    @Override
    public void getDataItemsByUri(IWearableCallbacks callbacks, Uri uri, int i) throws RemoteException {
        Log.d(TAG, "getDataItemsByUri: " + uri);
        Cursor dataHolderItems = nodeDatabase.getDataItemsForDataHolderByHostAndPath(packageName, PackageUtils.firstSignatureDigest(context, packageName), uri.getHost(), uri.getPath());
        callbacks.onDataHolder(DataHolder.fromCursor(dataHolderItems, 0, null));
    }

    @Override
    public void deleteDataItems(IWearableCallbacks callbacks, Uri uri) throws RemoteException {
        Log.d(TAG, "deleteDataItems: " + uri);
        int count = nodeDatabase.deleteDataItems(packageName, PackageUtils.firstSignatureDigest(context, packageName), uri.getHost(), uri.getPath());
        callbacks.onDeleteDataItemsResponse(new DeleteDataItemsResponse(0, count));
    }

    @Override
    public void optInCloudSync(IWearableCallbacks callbacks, boolean enable) throws RemoteException {
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void getLocalNode(IWearableCallbacks callbacks) throws RemoteException {
        try {
            callbacks.onGetLocalNodeResponse(new GetLocalNodeResponse(0, new NodeParcelable(getLocalNodeId(), getLocalNodeId())));
        } catch (Exception e) {
            callbacks.onGetLocalNodeResponse(new GetLocalNodeResponse(8, null));
        }
    }

    @Override
    public void getConnectedNodes(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConnectedNodes");
        callbacks.onGetConnectedNodesResponse(new GetConnectedNodesResponse(0, getConnectedNodesParcelableList()));
    }

    private List<NodeParcelable> getConnectedNodesParcelableList() {
        List<NodeParcelable> nodes = new ArrayList<NodeParcelable>();
        for (Node connectedNode : connectedNodes) {
            nodes.add(new NodeParcelable(connectedNode));
        }
        return nodes;
    }

    @Override
    public void addListener(IWearableCallbacks callbacks, AddListenerRequest request) throws RemoteException {
        Log.d(TAG, "addListener[nyp]: " + request);
        listeners.add(request.listener);
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void removeListener(IWearableCallbacks callbacks, RemoveListenerRequest request) throws RemoteException {
        Log.d(TAG, "removeListener[nyp]: " + request);
        listeners.remove(request.listener);
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void putConfig(IWearableCallbacks callbacks, ConnectionConfiguration config) throws RemoteException {
        if (config.nodeId == null) config.nodeId = getLocalNodeId();
        Log.d(TAG, "putConfig[nyp]: " + config);
        configDatabase.putConfiguration(config);
        configurationsUpdated = true;
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void deleteConfig(IWearableCallbacks callbacks, String name) throws RemoteException {
        configDatabase.deleteConfiguration(name);
        configurationsUpdated = true;
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void getConfig(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConfig");
        ConnectionConfiguration[] configurations = getConfigurations();
        if (configurations == null || configurations.length == 0) {
            callbacks.onGetConfigResponse(new GetConfigResponse(1, new ConnectionConfiguration(null, null, 0, 0, false)));
        } else {
            callbacks.onGetConfigResponse(new GetConfigResponse(0, configurations[0]));
        }
    }

    @Override
    public void getConfigs(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConfigs");
        try {
            callbacks.onGetConfigsResponse(new GetConfigsResponse(0, getConfigurations()));
        } catch (Exception e) {
            callbacks.onGetConfigsResponse(new GetConfigsResponse(8, new ConnectionConfiguration[0]));
        }
    }

    private synchronized ConnectionConfiguration[] getConfigurations() {
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
        return configurations;
    }

    @Override
    public void enableConnection(IWearableCallbacks callbacks, String name) throws RemoteException {
        Log.d(TAG, "enableConnection: " + name);
        configDatabase.setEnabledState(name, true);
        configurationsUpdated = true;
        callbacks.onStatus(Status.SUCCESS);
        if (name.equals("server")) {
            // TODO: hackady hack
            (nct = new NetworkConnectionThread(this, configDatabase.getConfiguration(name))).start();
        }
    }

    @Override
    public void disableConnection(IWearableCallbacks callbacks, String name) throws RemoteException {
        Log.d(TAG, "disableConnection: " + name);
        configDatabase.setEnabledState(name, false);
        configurationsUpdated = true;
        callbacks.onStatus(Status.SUCCESS);
        if (name.equals("server")) {
            // TODO: hacady hack
            if (nct != null) {
                nct.close();
                nct.interrupt();
                nct = null;
            }
        }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }

    @Override
    public void onDataChanged(DataHolder data) throws RemoteException {
        for (IWearableListener listener : listeners) {
            listener.onDataChanged(data);
        }
    }

    @Override
    public void onMessageReceived(MessageEventParcelable messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent);
        for (IWearableListener listener : new ArrayList<IWearableListener>(listeners)) {
            try {
                listener.onMessageReceived(messageEvent);
            } catch (RemoteException e) {
                listeners.remove(listener);
            }
        }
    }

    @Override
    public void onPeerConnected(NodeParcelable node) {
        Log.d(TAG, "onPeerConnected: " + node);
        for (IWearableListener listener : new ArrayList<IWearableListener>(listeners)) {
            try {
                listener.onPeerConnected(node);
            } catch (RemoteException e) {
                listeners.remove(listener);
            }
        }
        addConnectedNode(node);
    }

    private void addConnectedNode(Node node) {
        connectedNodes.add(node);
        onConnectedNodes(getConnectedNodesParcelableList());
    }

    private void removeConnectedNode(String nodeId) {
        Node toRemove = null;
        for (Node node : connectedNodes) {
            if (node.getId().equals(nodeId)) {
                toRemove = node;
                break;
            }
        }
        connectedNodes.remove(toRemove);
        onConnectedNodes(getConnectedNodesParcelableList());
    }

    @Override
    public void onPeerDisconnected(NodeParcelable node) throws RemoteException {
        for (IWearableListener listener : listeners) {
            listener.onPeerDisconnected(node);
        }
    }

    @Override
    public void onConnectedNodes(List<NodeParcelable> nodes) {
        Log.d(TAG, "onConnectedNodes: " + nodes);
        for (IWearableListener listener : listeners) {
            try {
                listener.onConnectedNodes(nodes);
            } catch (RemoteException e) {
                listeners.remove(listener);
            }
        }
    }

    @Override
    public void onNotificationReceived(AncsNotificationParcelable notification) throws RemoteException {
        for (IWearableListener listener : listeners) {
            listener.onNotificationReceived(notification);
        }
    }

    @Override
    public void onChannelEvent(ChannelEventParcelable channelEvent) throws RemoteException {
        for (IWearableListener listener : listeners) {
            listener.onChannelEvent(channelEvent);
        }
    }

    @Override
    public void onConnectedCapabilityChanged(CapabilityInfoParcelable capabilityInfo) throws RemoteException {
        for (IWearableListener listener : listeners) {
            listener.onConnectedCapabilityChanged(capabilityInfo);
        }
    }

    @Override
    public void onEntityUpdate(AmsEntityUpdateParcelable update) throws RemoteException {
        for (IWearableListener listener : listeners) {
            listener.onEntityUpdate(update);
        }
    }

    public Context getContext() {
        return context;
    }

    public void syncToPeer(WearableConnection connection, String nodeId, long seqId) {
        Log.d(TAG, "-- Start syncing over " + connection + ", nodeId " + nodeId + " starting with seqId " + seqId);
        Cursor cursor = nodeDatabase.getModifiedDataItems(nodeId, seqId, true);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DataItemRecord record = DataItemRecord.fromCursor(cursor);
                Log.d(TAG, "Sync over " + connection + ": " + record);
                SetDataItem.Builder builder = new SetDataItem.Builder()
                        .packageName(record.packageName)
                        .signatureDigest(record.signatureDigest)
                        .uri(record.dataItem.uri.toString())
                        .seqId(record.seqId)
                        .deleted(record.deleted)
                        .lastModified(record.lastModified);
                if (record.source != null) builder.source(record.source);
                if (record.dataItem.data != null) builder.data(ByteString.of(record.dataItem.data));
                List<AssetEntry> protoAssets = new ArrayList<AssetEntry>();
                Map<String, Asset> assets = record.dataItem.getAssets();
                for (String key : assets.keySet()) {
                    protoAssets.add(new AssetEntry.Builder()
                            .key(key)
                            .unknown3(4)
                            .value(new org.microg.wearable.proto.Asset.Builder()
                                    .digest(assets.get(key).getDigest())
                                    .build()).build());
                }
                builder.assets(protoAssets);
                try {
                    connection.writeMessage(new RootMessage.Builder().setDataItem(builder.build()).build());
                } catch (IOException e) {
                    Log.w(TAG, e);
                    break;
                }
            }
            cursor.close();
        }
        Log.d(TAG, "-- Done syncing over " + connection + ", nodeId " + nodeId + " starting with seqId " + seqId);
    }

    public long getCurrentSeqId(String nodeId) {
        return nodeDatabase.getCurrentSeqId(nodeId);
    }
}
