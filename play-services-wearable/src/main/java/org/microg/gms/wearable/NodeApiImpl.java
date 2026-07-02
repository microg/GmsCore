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

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse;
import com.google.android.gms.wearable.internal.GetLocalNodeResponse;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;

import org.microg.gms.common.GmsConnector;

import java.util.ArrayList;
import java.util.List;

public class NodeApiImpl implements NodeApi {

    private static final String TAG = "GmsWearNodeApi";

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, NodeListener listener) {
        Log.d(TAG, "addListener: wrapping NodeListener for WearableImpl");
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                IWearableListener wearableListener = new NodeListenerWrapper(listener);
                AddListenerRequest request = new AddListenerRequest(wearableListener, null, null);
                client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, request);
            }
        });
    }

    @Override
    public PendingResult<GetConnectedNodesResult> getConnectedNodes(GoogleApiClient client) {
        Log.d(TAG, "getConnectedNodes: requesting via WearableImpl");
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetConnectedNodesResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<GetConnectedNodesResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getConnectedNodes(new BaseWearableCallbacks() {
                    @Override
                    public void onGetConnectedNodesResponse(GetConnectedNodesResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetConnectedNodesResultImpl(response));
                    }
                });
            }
        });
    }

    @Override
    public PendingResult<GetLocalNodeResult> getLocalNode(GoogleApiClient client) {
        Log.d(TAG, "getLocalNode: requesting via WearableImpl");
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetLocalNodeResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<GetLocalNodeResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getLocalNode(new BaseWearableCallbacks() {
                    @Override
                    public void onGetLocalNodeResponse(GetLocalNodeResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetLocalNodeResultImpl(response));
                    }
                });
            }
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, NodeListener listener) {
        Log.d(TAG, "removeListener: cleaning up NodeListener");
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                RemoveListenerRequest request = new RemoveListenerRequest();
                client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, request);
            }
        });
    }

    /**
     * Wraps a NodeApi.NodeListener into an IWearableListener AIDL interface.
     * Translates onPeerConnected/onPeerDisconnected callbacks from the WearableImpl
     * into the NodeListener interface expected by client apps.
     */
    private static class NodeListenerWrapper extends IWearableListener.Stub {
        private final NodeListener listener;
        private static final String TAG_W = "NodeListenerWrapper";

        public NodeListenerWrapper(NodeListener listener) {
            this.listener = listener;
        }

        @Override
        public void onPeerConnected(NodeParcelable node) throws RemoteException {
            Log.d(TAG_W, "onPeerConnected: " + node.getDisplayName() + " (" + node.getId() + ")");
            if (listener != null) {
                listener.onPeerConnected(new NodeWrapper(node));
            }
        }

        @Override
        public void onPeerDisconnected(NodeParcelable node) throws RemoteException {
            Log.d(TAG_W, "onPeerDisconnected: " + node.getDisplayName() + " (" + node.getId() + ")");
            if (listener != null) {
                listener.onPeerDisconnected(new NodeWrapper(node));
            }
        }

        @Override
        public void onDataChanged(com.google.android.gms.common.data.DataHolder data) throws RemoteException {
            // Not used by NodeApi
        }

        @Override
        public void onMessageReceived(com.google.android.gms.wearable.internal.MessageEventParcelable messageEvent) throws RemoteException {
            // Not used by NodeApi
        }

        @Override
        public void onConnectedNodes(List<NodeParcelable> nodes) throws RemoteException {
            // Not used by NodeApi
        }

        @Override
        public void onNotificationReceived(com.google.android.gms.wearable.internal.AncsNotificationParcelable notification) throws RemoteException {
            // Not used by NodeApi
        }

        @Override
        public void onChannelEvent(com.google.android.gms.wearable.internal.ChannelEventParcelable channelEvent) throws RemoteException {
            // Not used by NodeApi
        }

        @Override
        public void onConnectedCapabilityChanged(com.google.android.gms.wearable.internal.CapabilityInfoParcelable capabilityInfo) throws RemoteException {
            // Not used by NodeApi
        }

        @Override
        public void onEntityUpdate(com.google.android.gms.wearable.internal.AmsEntityUpdateParcelable update) throws RemoteException {
            // Not used by NodeApi
        }
    }

    /**
     * Wraps NodeParcelable to implement the public Node interface.
     */
    private static class NodeWrapper implements Node {
        private final NodeParcelable delegate;

        public NodeWrapper(NodeParcelable node) {
            this.delegate = node;
        }

        @Override
        public String getDisplayName() {
            return delegate.getDisplayName();
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public boolean isNearby() {
            return delegate.isNearby();
        }
    }

    /**
     * Result implementation for getConnectedNodes.
     */
    public static class GetConnectedNodesResultImpl implements GetConnectedNodesResult {
        private final GetConnectedNodesResponse response;

        public GetConnectedNodesResultImpl(GetConnectedNodesResponse response) {
            this.response = response;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }

        @Override
        public List<Node> getNodes() {
            if (response.nodes == null) {
                return new ArrayList<>();
            }
            List<Node> nodes = new ArrayList<>(response.nodes.size());
            for (NodeParcelable node : response.nodes) {
                nodes.add(new NodeWrapper(node));
            }
            return nodes;
        }
    }

    /**
     * Result implementation for getLocalNode.
     */
    public static class GetLocalNodeResultImpl implements GetLocalNodeResult {
        private final GetLocalNodeResponse response;

        public GetLocalNodeResultImpl(GetLocalNodeResponse response) {
            this.response = response;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }

        @Override
        public Node getNode() {
            if (response.node == null) {
                return null;
            }
            return new NodeWrapper(response.node);
        }
    }
}
