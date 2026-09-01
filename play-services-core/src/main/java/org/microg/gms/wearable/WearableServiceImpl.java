/*
 * Copyright (C) 2013-2026 microG Project Team
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
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse;
import com.google.android.gms.wearable.internal.GetLocalNodeResponse;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.IWearableService;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.SendMessageResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WearableServiceImpl extends IWearableService.Stub {
    private static final String TAG = "WearableServiceImpl";
    private final Context context;
    private final AtomicInteger messageSequence = new AtomicInteger(1);

    public WearableServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public void getLocalNode(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getLocalNode");
        NodeParcelable localNode = NodeManager.getLocalNode(context);
        GetLocalNodeResponse response = new GetLocalNodeResponse(0, localNode);
        if (callbacks != null) {
            callbacks.onGetLocalNodeResponse(response);
        }
    }

    @Override
    public void getConnectedNodes(IWearableCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getConnectedNodes");
        List<NodeParcelable> nodes = NodeManager.getConnectedNodes(context);
        GetConnectedNodesResponse response = new GetConnectedNodesResponse(0, nodes);
        if (callbacks != null) {
            callbacks.onGetConnectedNodesResponse(response);
        }
    }

    @Override
    public void sendMessage(IWearableCallbacks callbacks, String nodeId, String path, byte[] data) throws RemoteException {
        Log.d(TAG, "sendMessage to nodeId: " + nodeId + ", path: " + path + " (length: " + (data != null ? data.length : 0) + ")");
        int requestId = messageSequence.getAndIncrement();
        SendMessageResponse response = new SendMessageResponse(0, requestId);
        if (callbacks != null) {
            callbacks.onSendMessageResponse(response);
        }
    }

    @Override
    public void putDataItem(IWearableCallbacks callbacks, PutDataRequest request) throws RemoteException {
        Log.d(TAG, "putDataItem uri: " + (request != null ? request.getUri() : "null"));
        PutDataResponse response = new PutDataResponse(0, null);
        if (callbacks != null) {
            callbacks.onPutDataResponse(response);
        }
    }

    @Override
    public void getDataItem(IWearableCallbacks callbacks, Uri uri) throws RemoteException {
        Log.d(TAG, "getDataItem uri: " + uri);
        if (callbacks != null) {
            callbacks.onStatus(Status.SUCCESS);
        }
    }

    @Override
    public void deleteDataItems(IWearableCallbacks callbacks, Uri uri) throws RemoteException {
        Log.d(TAG, "deleteDataItems uri: " + uri);
        if (callbacks != null) {
            callbacks.onStatus(Status.SUCCESS);
        }
    }

    @Override
    public void addListener(IWearableCallbacks callbacks, AddListenerRequest request) throws RemoteException {
        Log.d(TAG, "addListener");
        if (callbacks != null) {
            callbacks.onStatus(Status.SUCCESS);
        }
    }

    @Override
    public void removeListener(IWearableCallbacks callbacks, RemoveListenerRequest request) throws RemoteException {
        Log.d(TAG, "removeListener");
        if (callbacks != null) {
            callbacks.onStatus(Status.SUCCESS);
        }
    }

    @Override
    public void getCompanionPackageForNode(IWearableCallbacks callbacks, String nodeId) throws RemoteException {
        Log.d(TAG, "getCompanionPackageForNode nodeId: " + nodeId);
        if (callbacks != null) {
            callbacks.onStatus(Status.SUCCESS);
        }
    }
}