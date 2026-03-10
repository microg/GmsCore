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

import android.net.Uri;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.Builder;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

/**
 * Exposes an API to learn about capabilities provided by nodes on the Wear network.
 * <p/>
 * Capabilities are local to an application.
 */
@PublicApi
public interface CapabilityApi {
    /**
     * Capability changed action for use in manifest-based listener filters.
     * <p/>
     * Capability events do not support filtering by host, but can be filtered by path.
     *
     * @see WearableListenerService
     */
    String ACTION_CAPABILITY_CHANGED = "com.google.android.gms.wearable.CAPABILITY_CHANGED";

    /**
     * Filter type for {@link #getCapability(GoogleApiClient, String, int)}, {@link #getAllCapabilities(GoogleApiClient, int)}:
     * If this filter is set then the full set of nodes that declare the given capability will be
     * included in the capability's CapabilityInfo.
     */
    int FILTER_ALL = 0;

    /**
     * Filter type for {@link #addListener(GoogleApiClient, CapabilityListener, Uri, int)}, if this
     * filter is set, the given URI will be taken as a literal path, and the operation will apply
     * to the matching capability only.
     */
    int FILTER_LITERAL = 0;

    /**
     * Filter type for {@link #addListener(GoogleApiClient, CapabilityListener, Uri, int)}, if this
     * filter is set, the given URI will be taken as a path prefix, and the operation will apply
     * to all matching capabilities.
     */
    int FILTER_PREFIX = 1;

    /**
     * Filter type for {@link #getCapability(GoogleApiClient, String, int)}, {@link #getAllCapabilities(GoogleApiClient, int):
     * If this filter is set then only reachable nodes that declare the given capability will be
     * included in the capability's CapabilityInfo.
     */
    int FILTER_REACHABLE = 1;

    /**
     * Registers a listener to be notified of a specific capability being added to or removed from
     * the Wear network. Calls to this method should be balanced with {@link #removeCapabilityListener(GoogleApiClient, CapabilityListener, String)}
     * to avoid leaking resources.
     * <p/>
     * Listener events will be called on the main thread, or the handler specified on {@code client}
     * when it was built (using {@link Builder#setHandler(Handler)}).
     * <p/>
     * Callers wishing to be notified of events in the background should use {@link WearableListenerService}.
     */
    PendingResult<Status> addCapabilityListener(GoogleApiClient client, CapabilityListener listener, String capability);

    /**
     * Registers a listener to be notified of capabilities being added to or removed from the Wear
     * network. Calls to this method should be balanced with {@link #removeListener(GoogleApiClient, CapabilityListener)}
     * to avoid leaking resources.
     * <p/>
     * {@code uri} and {@code filterType} can be used to filter the capability changes sent to the
     * listener. For example, if {@code uri} and {@code filterType} create a prefix filter, then
     * only capabilities matching that prefix will be notified. The {@code uri} follows the rules
     * of the {@code <data>} element of {@code <intent-filter>}. The path is ignored if a URI host
     * is not specified. To match capabilities by name or name prefix, the host must be {@code *}. i.e:
     * <p/>
     * <pre>wear://* /<capability_name></pre>
     * Listener events will be called on the main thread, or the handler specified on {@code client}
     * when it was built (using {@link Builder#setHandler(Handler)}).
     *
     * Callers wishing to be notified of events in the background should use WearableListenerService.
     */
    PendingResult<Status> addListener(GoogleApiClient client, CapabilityListener listener, Uri uri, @CapabilityFilterType int filterType);

    /**
     * Announces that a capability has become available on the local node.
     */
    PendingResult<AddLocalCapabilityResult> addLocalCapability(GoogleApiClient client, String capability);

    /**
     * Returns information about all capabilities, including the nodes that declare those
     * capabilities. The filter parameter controls whether all nodes are returned, {@link #FILTER_ALL},
     * or only those that are currently reachable by this node, {@link #FILTER_REACHABLE}.
     * <p/>
     * The local node will never be returned in the set of nodes.
     */
    PendingResult<GetAllCapabilitiesResult> getAllCapabilities(GoogleApiClient client, @NodeFilterType int nodeFilter);

    /**
     * Returns information about a capabilities, including the nodes that declare this capability.
     * The filter parameter controls whether all nodes are returned, {@link #FILTER_ALL}, or only
     * those that are currently reachable by this node, {@link #FILTER_REACHABLE}.
     * <p/>
     * The local node will never be returned in the set of nodes.
     */
    PendingResult<GetCapabilityResult> getCapability(GoogleApiClient client, String capability, @NodeFilterType int nodeFilter);

    /**
     * Removes a listener which was previously added through {@link #addCapabilityListener(GoogleApiClient, CapabilityListener, String)}.
     * The listener is only removed from listening for the capability provided and will continue to
     * receive messages for any other capabilities it was previously registered for that have not
     * also been removed.
     */
    PendingResult<Status> removeCapabilityListener(GoogleApiClient client, CapabilityListener listener, String capability);

    /**
     * Removes a listener which was previously added through {@link #addListener(GoogleApiClient, CapabilityListener, Uri, int)}.
     * The listener is only removed from listening for the capability provided and will continue to
     * receive messages for any other capabilities it was previously registered for that have not
     * also been removed.
     */
    PendingResult<Status> removeListener(GoogleApiClient client, CapabilityListener listener);

    /**
     * Announces that a capability is no longer available on the local node. Note: this will not
     * remove any capabilities announced in the Manifest for an app.
     */
    PendingResult<RemoveLocalCapabilityResult> removeLocalCapability(GoogleApiClient client, String capability);

    /**
     * Result returned from {@link #addLocalCapability(GoogleApiClient, String)}
     */
    interface AddLocalCapabilityResult extends Result {
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface CapabilityFilterType {
    }

    /**
     * Listener for changes in the reachable nodes providing a capability.
     */
    interface CapabilityListener {
        void onCapabilityChanged(CapabilityInfo capabilityInfo);
    }

    /**
     * Result returned from {@link #getAllCapabilities(GoogleApiClient, int)}
     */
    interface GetAllCapabilitiesResult extends Result {
        Map<String, CapabilityInfo> getAllCapabilities();
    }

    /**
     * Result returned from {@link #getCapability(GoogleApiClient, String, int)}
     */
    interface GetCapabilityResult extends Result {
        CapabilityInfo getCapability();
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface NodeFilterType {
    }

    /**
     * Result returned from {@link #removeLocalCapability(GoogleApiClient, String)}
     */
    interface RemoveLocalCapabilityResult extends Result {
    }
}
