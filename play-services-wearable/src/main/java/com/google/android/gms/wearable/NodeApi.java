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

package com.google.android.gms.wearable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;

import java.util.List;

/**
 * Exposes an API for to learn about local or connected Nodes.
 * <p/>
 * Node events are delivered to all applications on a device.
 */
@PublicApi
public interface NodeApi {

    /**
     * Registers a listener to receive all node events. Calls to this method should balanced with
     * {@link #removeListener(GoogleApiClient, NodeListener)}, to avoid leaking resources.
     * <p/>
     * Callers wishing to be notified of node events in the background should use WearableListenerService.
     */
    PendingResult<Status> addListener(GoogleApiClient client, NodeListener listener);

    /**
     * Gets a list of nodes to which this device is currently connected.
     * <p/>
     * The returned list will not include the {@link #getLocalNode(GoogleApiClient) local node}.
     */
    PendingResult<GetConnectedNodesResult> getConnectedNodes(GoogleApiClient client);

    /**
     * Gets the {@link Node} that refers to this device. The information in the returned Node
     * can be passed to other devices using the {@link MessageApi}, for example.
     */
    PendingResult<GetLocalNodeResult> getLocalNode(GoogleApiClient client);

    /**
     * Removes a listener which was previously added through
     * {@link #addListener(GoogleApiClient, NodeListener)}.
     */
    PendingResult<Status> removeListener(GoogleApiClient client, NodeListener listener);


    /**
     * Contains a list of connected nodes.
     */
    interface GetConnectedNodesResult extends Result {
        /**
         * @return a list of connected nodes. This list doesn't include the local node.
         */
        List<Node> getNodes();
    }

    /**
     * Contains the name and id that represents this device.
     */
    interface GetLocalNodeResult extends Result {
        /**
         * @return a {@link Node} object which represents this device.
         */
        Node getNode();
    }

    /**
     * Used with {@link NodeApi#addListener(GoogleApiClient, NodeListener)} to receive node events.
     */
    interface NodeListener {
        /**
         * Notification that a peer has been connected.
         */
        void onPeerConnected(Node peer);

        /**
         * Notification that a peer has been disconnected.
         */
        void onPeerDisconnected(Node peer);
    }
}
