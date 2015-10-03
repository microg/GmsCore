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
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetConfigResponse;
import com.google.android.gms.wearable.internal.GetConfigsResponse;
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetLocalNodeResponse;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.DataItemParcelable;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.IWearableService;
import com.google.android.gms.wearable.internal.NodeParcelable;

import org.microg.gms.common.PackageUtils;

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

    private long seqIdBlock;
    private long seqIdInBlock = -1;

    public WearableServiceImpl(Context context, NodeDatabaseHelper nodeDatabase, ConfigurationDatabaseHelper configDatabase, String packageName) {
        this.context = context;
        this.nodeDatabase = nodeDatabase;
        this.configDatabase = configDatabase;
        this.packageName = packageName;
    }

    private String getLocalNodeId() {
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
        Log.d(TAG, "putData: " + request);
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

    private DataItemRecord putDataItem(String packageName, String signatureDigest, String source, DataItemInternal dataItem) {
        DataItemRecord record = new DataItemRecord();
        record.packageName = packageName;
        record.signatureDigest = signatureDigest;
        record.deleted = false;
        record.source = source;
        record.dataItem = dataItem;
        record.v1SeqId = getNextSeqId();
        if (record.source.equals(getLocalNodeId())) record.seqId = record.v1SeqId;
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
                }
            }
            return Asset.createFromRef(digest);
        }
        return null;
    }

    private File createAssetFile(String digest) {
        File dir = new File(new File(context.getFilesDir(), "assets"), digest.substring(digest.length() - 2, digest.length()));
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
        Log.d(TAG, "getConnectedNodes[fak]");
        callbacks.onGetConnectedNodesResponse(new GetConnectedNodesResponse(0, new ArrayList<NodeParcelable>()));
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
        Log.d(TAG, "putConfig[nyp]: " + config);
        configDatabase.putConfiguration(config);
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void getConfig(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConfig");
        ConnectionConfiguration[] configurations = configDatabase.getAllConfigurations();
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
            callbacks.onGetConfigsResponse(new GetConfigsResponse(0, configDatabase.getAllConfigurations()));
        } catch (Exception e) {
            callbacks.onGetConfigsResponse(new GetConfigsResponse(8, new ConnectionConfiguration[0]));
        }
    }

    @Override
    public void disableConnection(IWearableCallbacks callbacks, String name) throws RemoteException {
        configDatabase.setEnabledState(name, false);
        callbacks.onStatus(Status.SUCCESS);
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
    public void onMessageReceived(MessageEventParcelable messageEvent) throws RemoteException {
        for (IWearableListener listener : listeners) {
            listener.onMessageReceived(messageEvent);
        }
    }

    @Override
    public void onPeerConnected(NodeParcelable node) throws RemoteException {
        for (IWearableListener listener : listeners) {
            listener.onPeerConnected(node);
        }
    }

    @Override
    public void onPeerDisconnected(NodeParcelable node) throws RemoteException {
        for (IWearableListener listener : listeners) {
            listener.onPeerDisconnected(node);
        }
    }

    @Override
    public void onConnectedNodes(List<NodeParcelable> nodes) throws RemoteException {
        for (IWearableListener listener : listeners) {
            listener.onConnectedNodes(nodes);
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
}
