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
import android.net.Uri;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetConfigResponse;
import com.google.android.gms.wearable.internal.GetConfigsResponse;
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetLocalNodeResponse;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.IWearableService;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;

public class WearableServiceImpl extends IWearableService.Stub {
    private static final String TAG = "GmsWearSvcImpl";

    private final Context context;
    private final String packageName;
    private final WearableImpl wearable;

    public WearableServiceImpl(Context context, WearableImpl wearable, String packageName) {
        this.context = context;
        this.wearable = wearable;
        this.packageName = packageName;
    }


    @Override
    public void putData(IWearableCallbacks callbacks, PutDataRequest request) throws RemoteException {
        Log.d(TAG, "putData: " + request.toString(true));
        DataItemRecord record = wearable.putData(request, packageName);
        callbacks.onPutDataResponse(new PutDataResponse(0, record.toParcelable()));
    }

    @Override
    public void getDataItem(IWearableCallbacks callbacks, Uri uri) throws RemoteException {
        Log.d(TAG, "getDataItem: " + uri);

        DataItemRecord record = wearable.getDataItemByUri(uri, packageName);
        if (record != null) {
            callbacks.onGetDataItemResponse(new GetDataItemResponse(0, record.toParcelable()));
        } else {
            // TODO: negative
        }
    }

    @Override
    public void getDataItems(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getDataItems: " + callbacks);
        callbacks.onDataHolder(wearable.getDataItems(packageName));
    }

    @Override
    public void getDataItemsByUri(IWearableCallbacks callbacks, Uri uri, int i) throws RemoteException {
        Log.d(TAG, "getDataItemsByUri: " + uri);
        callbacks.onDataHolder(wearable.getDataItemsByUri(uri, packageName));
    }

    @Override
    public void deleteDataItems(IWearableCallbacks callbacks, Uri uri) throws RemoteException {
        Log.d(TAG, "deleteDataItems: " + uri);
        callbacks.onDeleteDataItemsResponse(new DeleteDataItemsResponse(0, wearable.deleteDataItems(uri, packageName)));
    }

    @Override
    public void optInCloudSync(IWearableCallbacks callbacks, boolean enable) throws RemoteException {
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void getLocalNode(IWearableCallbacks callbacks) throws RemoteException {
        try {
            callbacks.onGetLocalNodeResponse(new GetLocalNodeResponse(0, new NodeParcelable(wearable.getLocalNodeId(), wearable.getLocalNodeId())));
        } catch (Exception e) {
            callbacks.onGetLocalNodeResponse(new GetLocalNodeResponse(8, null));
        }
    }

    @Override
    public void getConnectedNodes(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConnectedNodes");
        callbacks.onGetConnectedNodesResponse(new GetConnectedNodesResponse(0, wearable.getConnectedNodesParcelableList()));
    }

    @Override
    public void addListener(IWearableCallbacks callbacks, AddListenerRequest request) throws RemoteException {
        Log.d(TAG, "addListener[nyp]: " + request);
        if (request.listener != null) {
            wearable.addListener(request.listener);
        }
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void removeListener(IWearableCallbacks callbacks, RemoveListenerRequest request) throws RemoteException {
        Log.d(TAG, "removeListener[nyp]: " + request);
        wearable.removeListener(request.listener);
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void putConfig(IWearableCallbacks callbacks, ConnectionConfiguration config) throws RemoteException {
        wearable.createConnection(config);
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void deleteConfig(IWearableCallbacks callbacks, String name) throws RemoteException {
        wearable.deleteConnection(name);
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void getConfig(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConfig");
        ConnectionConfiguration[] configurations = wearable.getConfigurations();
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
            callbacks.onGetConfigsResponse(new GetConfigsResponse(0, wearable.getConfigurations()));
        } catch (Exception e) {
            callbacks.onGetConfigsResponse(new GetConfigsResponse(8, new ConnectionConfiguration[0]));
        }
    }


    @Override
    public void enableConnection(IWearableCallbacks callbacks, String name) throws RemoteException {
        Log.d(TAG, "enableConnection: " + name);
        wearable.enableConnection(name);
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public void disableConnection(IWearableCallbacks callbacks, String name) throws RemoteException {
        Log.d(TAG, "disableConnection: " + name);
        wearable.disableConnection(name);
        callbacks.onStatus(Status.SUCCESS);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
