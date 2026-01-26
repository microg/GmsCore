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

import java.util.List;

public class NodeApiImpl implements NodeApi {
    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, NodeListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<GetConnectedNodesResult> getConnectedNodes(GoogleApiClient client) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().getConnectedNodes(new BaseWearableCallbacks() {
                @Override
                public void onGetConnectedNodesResponse(GetConnectedNodesResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new GetConnectedNodesResultImpl(new Status(response.statusCode), response.nodes));
                }
            });
        });
    }

    @Override
    public PendingResult<GetLocalNodeResult> getLocalNode(GoogleApiClient client) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().getLocalNode(new BaseWearableCallbacks() {
                @Override
                public void onGetLocalNodeResponse(GetLocalNodeResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new GetLocalNodeResultImpl(new Status(response.statusCode), response.node));
                }
            });
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, NodeListener listener) {
        throw new UnsupportedOperationException();
    }

    private static class GetLocalNodeResultImpl implements GetLocalNodeResult {
        private final Status status;
        private final Node node;

        private GetLocalNodeResultImpl(Status status, Node node) {
            this.status = status;
            this.node = node;
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

    private static class GetConnectedNodesResultImpl implements GetConnectedNodesResult {
        private final Status status;
        private final List<Node> nodes;

        private GetConnectedNodesResultImpl(Status status, List<Node> nodes) {
            this.status = status;
            this.nodes = nodes;
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
}
