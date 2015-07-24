/*
 * Copyright 2013-2015 µg Project Team
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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.AddListenerRequest;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.GetConfigResponse;
import com.google.android.gms.wearable.GetConfigsResponse;
import com.google.android.gms.wearable.GetConnectedNodesResponse;
import com.google.android.gms.wearable.GetDataItemResponse;
import com.google.android.gms.wearable.GetLocalNodeResponse;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.PutDataResponse;
import com.google.android.gms.wearable.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.DataItemAssetParcelable;
import com.google.android.gms.wearable.internal.DataItemParcelable;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.IWearableService;
import com.google.android.gms.wearable.internal.NodeParcelable;

import org.microg.gms.common.PackageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WearableServiceImpl extends IWearableService.Stub {
    private static final String TAG = "GmsWearSvcImpl";

    private static final String CLOCKWORK_NODE_PREFERENCES = "cw_node";
    private static final String CLOCKWORK_NODE_PREFERENCE_NODE_ID = "node_id";

    private final Context context;
    private final String packageName;
    private final NodeDatabaseHelper nodeDatabase;
    private final ConfigurationDatabaseHelper configDatabase;


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

    @Override
    public void putData(IWearableCallbacks callbacks, PutDataRequest request) throws RemoteException {
        Log.d(TAG, "putData: " + request);
        String host = request.getUri().getHost();
        if (TextUtils.isEmpty(host)) host = getLocalNodeId();
        ContentValues prepared = new ContentValues();
        prepared.put("sourceNode", getLocalNodeId());
        prepared.put("deleted", false);
        prepared.put("data", request.getData());
        prepared.put("timestampMs", System.currentTimeMillis());
        prepared.put("seqId", 0xFFFFFFFFL);
        prepared.put("assetsPresent", false);
        nodeDatabase.putDataItem(packageName, PackageUtils.firstSignatureDigest(context, packageName), host, request.getUri().getPath(), prepared);
        Map<String, DataItemAssetParcelable> assetMap = new HashMap<String, DataItemAssetParcelable>();
        for (String key : request.getAssets().keySet()) {
            assetMap.put(key, new DataItemAssetParcelable());
        }
        if (!assetMap.isEmpty()) {
            prepared.put("assetsPresent", true);
            nodeDatabase.putDataItem(packageName, PackageUtils.firstSignatureDigest(context, packageName), host, request.getUri().getPath(), prepared);
        }
        callbacks.onPutDataResponse(new PutDataResponse(0, new DataItemParcelable(request.getUri(), assetMap)));
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
        nodeDatabase.deleteDataItem(packageName, PackageUtils.firstSignatureDigest(context, packageName), uri.getHost(), uri.getPath());
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
    }

    @Override
    public void removeListener(IWearableCallbacks callbacks, RemoveListenerRequest request) throws RemoteException {
        Log.d(TAG, "removeListener[nyp]: " + request);
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
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
