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
package com.google.android.gms.wearable

import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Result
import com.google.android.gms.common.api.Status
import org.microg.gms.common.PublicApi

/**
 * Exposes an API for to learn about local or connected Nodes.
 *
 *
 * Node events are delivered to all applications on a device.
 */
@PublicApi
interface NodeApi {
    /**
     * Registers a listener to receive all node events. Calls to this method should balanced with
     * [.removeListener], to avoid leaking resources.
     *
     *
     * Callers wishing to be notified of node events in the background should use WearableListenerService.
     */
    fun addListener(client: GoogleApiClient?, listener: NodeListener?): PendingResult<Status?>?

    /**
     * Gets a list of nodes to which this device is currently connected.
     *
     *
     * The returned list will not include the [local node][.getLocalNode].
     */
    fun getConnectedNodes(client: GoogleApiClient?): PendingResult<GetConnectedNodesResult?>?

    /**
     * Gets the [Node] that refers to this device. The information in the returned Node
     * can be passed to other devices using the [MessageApi], for example.
     */
    fun getLocalNode(client: GoogleApiClient?): PendingResult<GetLocalNodeResult?>?

    /**
     * Removes a listener which was previously added through
     * [.addListener].
     */
    fun removeListener(client: GoogleApiClient?, listener: NodeListener?): PendingResult<Status?>?

    /**
     * Contains a list of connected nodes.
     */
    interface GetConnectedNodesResult : Result {
        /**
         * @return a list of connected nodes. This list doesn't include the local node.
         */
        val nodes: List<Node?>?
    }

    /**
     * Contains the name and id that represents this device.
     */
    interface GetLocalNodeResult : Result {
        /**
         * @return a [Node] object which represents this device.
         */
        val node: Node?
    }

    /**
     * Used with [NodeApi.addListener] to receive node events.
     */
    interface NodeListener {
        /**
         * Notification that a peer has been connected.
         */
        fun onPeerConnected(peer: Node?)

        /**
         * Notification that a peer has been disconnected.
         */
        fun onPeerDisconnected(peer: Node?)
    }
}