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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse;
import com.google.android.gms.wearable.internal.GetLocalNodeResponse;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;

import org.microg.gms.common.GmsConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeApiImpl implements NodeApi {
    private final Map<NodeListener, IWearableListener> listenerWrappers = new ConcurrentHashMap<NodeListener, IWearableListener>();

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, final NodeListener listener) {
        final IWearableListener wrapper = new NodeListenerWrapper(listener);
        listenerWrappers.put(listener, wrapper);
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, new AddListenerRequest(wrapper, null, null));
            }
        });
    }

    @Override
    public PendingResult<GetConnectedNodesResult> getConnectedNodes(GoogleApiClient client) {
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
    public PendingResult<Status> removeListener(GoogleApiClient client, final NodeListener listener) {
        final IWearableListener wrapper = listenerWrappers.remove(listener);
        if (wrapper == null) {
            return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
                @Override
                public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                    resultProvider.onResultAvailable(Status.SUCCESS);
                }
            });
        }
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, new RemoveListenerRequest(wrapper));
            }
        });
    }

    public static class GetConnectedNodesResultImpl implements GetConnectedNodesResult {
        private Status status;
        private List<Node> nodes;

        public GetConnectedNodesResultImpl(GetConnectedNodesResponse response) {
            this.status = new Status(response.statusCode);
            this.nodes = new ArrayList<Node>();
            if (response.nodes != null) {
                for (NodeParcelable node : response.nodes) {
                    this.nodes.add(node);
                }
            }
        }

        @Override
        public List<Node> getNodes() {
            return nodes;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    public static class GetLocalNodeResultImpl implements GetLocalNodeResult {
        private Status status;
        private Node node;

        public GetLocalNodeResultImpl(GetLocalNodeResponse response) {
            this.status = new Status(response.statusCode);
            this.node = response.node;
        }

        @Override
        public Node getNode() {
            return node;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    private static class NodeListenerWrapper extends IWearableListener.Stub {
        private final NodeListener listener;

        NodeListenerWrapper(NodeListener listener) {
            this.listener = listener;
        }

        @Override
        public void onPeerConnected(NodeParcelable node) throws RemoteException {
            listener.onPeerConnected(node);
        }

        @Override
        public void onPeerDisconnected(NodeParcelable node) throws RemoteException {
            listener.onPeerDisconnected(node);
        }
    }
}
