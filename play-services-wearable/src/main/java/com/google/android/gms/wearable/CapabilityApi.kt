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

import android.net.Uri
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Result
import com.google.android.gms.common.api.Status
import org.microg.gms.common.PublicApi
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Exposes an API to learn about capabilities provided by nodes on the Wear network.
 *
 *
 * Capabilities are local to an application.
 */
@PublicApi
interface CapabilityApi {
    /**
     * Registers a listener to be notified of a specific capability being added to or removed from
     * the Wear network. Calls to this method should be balanced with [.removeCapabilityListener]
     * to avoid leaking resources.
     *
     *
     * Listener events will be called on the main thread, or the handler specified on `client`
     * when it was built (using [Builder.setHandler]).
     *
     *
     * Callers wishing to be notified of events in the background should use [WearableListenerService].
     */
    fun addCapabilityListener(client: GoogleApiClient?, listener: CapabilityListener?, capability: String?): PendingResult<Status?>?

    /**
     * Registers a listener to be notified of capabilities being added to or removed from the Wear
     * network. Calls to this method should be balanced with [.removeListener]
     * to avoid leaking resources.
     *
     *
     * `uri` and `filterType` can be used to filter the capability changes sent to the
     * listener. For example, if `uri` and `filterType` create a prefix filter, then
     * only capabilities matching that prefix will be notified. The `uri` follows the rules
     * of the `<data>` element of `<intent-filter>`. The path is ignored if a URI host
     * is not specified. To match capabilities by name or name prefix, the host must be `*`. i.e:
     *
     *
     * <pre>wear:// * /<capability_name></capability_name></pre>
     * Listener events will be called on the main thread, or the handler specified on `client`
     * when it was built (using [Builder.setHandler]).
     *
     * Callers wishing to be notified of events in the background should use WearableListenerService.
     */
    fun addListener(client: GoogleApiClient?, listener: CapabilityListener?, uri: Uri?, @CapabilityFilterType filterType: Int): PendingResult<Status?>?

    /**
     * Announces that a capability has become available on the local node.
     */
    fun addLocalCapability(client: GoogleApiClient?, capability: String?): PendingResult<AddLocalCapabilityResult?>?

    /**
     * Returns information about all capabilities, including the nodes that declare those
     * capabilities. The filter parameter controls whether all nodes are returned, [.FILTER_ALL],
     * or only those that are currently reachable by this node, [.FILTER_REACHABLE].
     *
     *
     * The local node will never be returned in the set of nodes.
     */
    fun getAllCapabilities(client: GoogleApiClient?, @NodeFilterType nodeFilter: Int): PendingResult<GetAllCapabilitiesResult?>?

    /**
     * Returns information about a capabilities, including the nodes that declare this capability.
     * The filter parameter controls whether all nodes are returned, [.FILTER_ALL], or only
     * those that are currently reachable by this node, [.FILTER_REACHABLE].
     *
     *
     * The local node will never be returned in the set of nodes.
     */
    fun getCapability(client: GoogleApiClient?, capability: String?, @NodeFilterType nodeFilter: Int): PendingResult<GetCapabilityResult?>?

    /**
     * Removes a listener which was previously added through [.addCapabilityListener].
     * The listener is only removed from listening for the capability provided and will continue to
     * receive messages for any other capabilities it was previously registered for that have not
     * also been removed.
     */
    fun removeCapabilityListener(client: GoogleApiClient?, listener: CapabilityListener?, capability: String?): PendingResult<Status?>?

    /**
     * Removes a listener which was previously added through [.addListener].
     * The listener is only removed from listening for the capability provided and will continue to
     * receive messages for any other capabilities it was previously registered for that have not
     * also been removed.
     */
    fun removeListener(client: GoogleApiClient?, listener: CapabilityListener?): PendingResult<Status?>?

    /**
     * Announces that a capability is no longer available on the local node. Note: this will not
     * remove any capabilities announced in the Manifest for an app.
     */
    fun removeLocalCapability(client: GoogleApiClient?, capability: String?): PendingResult<RemoveLocalCapabilityResult?>?

    /**
     * Result returned from [.addLocalCapability]
     */
    interface AddLocalCapabilityResult : Result

    @Retention(RetentionPolicy.SOURCE)
    annotation class CapabilityFilterType

    /**
     * Listener for changes in the reachable nodes providing a capability.
     */
    interface CapabilityListener {
        fun onCapabilityChanged(capabilityInfo: CapabilityInfo?)
    }

    /**
     * Result returned from [.getAllCapabilities]
     */
    interface GetAllCapabilitiesResult : Result {
        val allCapabilities: Map<String?, CapabilityInfo?>?
    }

    /**
     * Result returned from [.getCapability]
     */
    interface GetCapabilityResult : Result {
        fun getCapability(): CapabilityInfo?
    }

    @Retention(RetentionPolicy.SOURCE)
    annotation class NodeFilterType

    /**
     * Result returned from [.removeLocalCapability]
     */
    interface RemoveLocalCapabilityResult : Result
    companion object {
        /**
         * Capability changed action for use in manifest-based listener filters.
         *
         *
         * Capability events do not support filtering by host, but can be filtered by path.
         *
         * @see WearableListenerService
         */
        const val ACTION_CAPABILITY_CHANGED = "com.google.android.gms.wearable.CAPABILITY_CHANGED"

        /**
         * Filter type for [.getCapability], [.getAllCapabilities]:
         * If this filter is set then the full set of nodes that declare the given capability will be
         * included in the capability's CapabilityInfo.
         */
        const val FILTER_ALL = 0

        /**
         * Filter type for [.addListener], if this
         * filter is set, the given URI will be taken as a literal path, and the operation will apply
         * to the matching capability only.
         */
        const val FILTER_LITERAL = 0

        /**
         * Filter type for [.addListener], if this
         * filter is set, the given URI will be taken as a path prefix, and the operation will apply
         * to all matching capabilities.
         */
        const val FILTER_PREFIX = 1

        /**
         * Filter type for [.getCapability], [:][.getAllCapabilities]
         */
        const val FILTER_REACHABLE = 1
    }
}