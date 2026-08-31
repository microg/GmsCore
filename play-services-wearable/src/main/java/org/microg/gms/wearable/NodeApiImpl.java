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

import org.microg.gms.common.GmsConnector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class NodeApiImpl implements NodeApi {
    private static final Map<NodeListener, WearableListenerStub> LISTENERS = new WeakHashMap<>();

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, NodeListener listener) {
        WearableListenerStub stub = LISTENERS.get(listener);
        if (stub == null) {
            stub = new WearableListenerStub(listener);
            LISTENERS.put(listener, stub);
        }
        final WearableListenerStub toAdd = stub;
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, Status>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().addListener(WearableListenerStub.statusCallback(resultProvider), toAdd.toAddRequest()));
    }

    @Override
    public PendingResult<GetConnectedNodesResult> getConnectedNodes(GoogleApiClient client) {
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, GetConnectedNodesResult>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().getConnectedNodes(new BaseWearableCallbacks() {
                    @Override
                    public void onGetConnectedNodesResponse(GetConnectedNodesResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new ConnectedNodesResultImpl(response));
                    }
                }));
    }

    @Override
    public PendingResult<GetLocalNodeResult> getLocalNode(GoogleApiClient client) {
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, GetLocalNodeResult>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().getLocalNode(new BaseWearableCallbacks() {
                    @Override
                    public void onGetLocalNodeResponse(GetLocalNodeResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new LocalNodeResultImpl(response));
                    }
                }));
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, NodeListener listener) {
        WearableListenerStub stub = LISTENERS.remove(listener);
        if (stub == null) {
            return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, Status>) (wearableClient, resultProvider) ->
                    resultProvider.onResultAvailable(Status.SUCCESS));
        }
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, Status>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().removeListener(WearableListenerStub.statusCallback(resultProvider), stub.toRemoveRequest()));
    }

    public static class LocalNodeResultImpl implements GetLocalNodeResult {
        private final GetLocalNodeResponse response;

        public LocalNodeResultImpl(GetLocalNodeResponse response) {
            this.response = response;
        }

        @Override
        public Node getNode() {
            return response.node;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }

    public static class ConnectedNodesResultImpl implements GetConnectedNodesResult {
        private final GetConnectedNodesResponse response;

        public ConnectedNodesResultImpl(GetConnectedNodesResponse response) {
            this.response = response;
        }

        @Override
        public List<Node> getNodes() {
            if (response.nodes == null) {
                return Collections.emptyList();
            }
            return new ArrayList<Node>(response.nodes);
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }
}
