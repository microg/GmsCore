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
import java.util.List;

public class NodeApiImpl implements NodeApi {
    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, NodeListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                resultProvider.onResultAvailable(Status.SUCCESS);
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
                    public void onGetConnectedNodesResponse(final GetConnectedNodesResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetConnectedNodesResult() {
                            @Override
                            public List<Node> getNodes() {
                                if (response.nodes == null) return new ArrayList<Node>();
                                return new ArrayList<Node>(response.nodes);
                            }

                            @Override
                            public Status getStatus() {
                                return new Status(response.statusCode);
                            }
                        });
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
                    public void onGetLocalNodeResponse(final GetLocalNodeResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetLocalNodeResult() {
                            @Override
                            public Node getNode() {
                                return response.node;
                            }

                            @Override
                            public Status getStatus() {
                                return new Status(response.statusCode);
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, NodeListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                resultProvider.onResultAvailable(Status.SUCCESS);
            }
        });
    }
}
