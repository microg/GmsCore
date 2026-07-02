/*
 * Copyright (C) 2013-2025 microG Project Team
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.google.android.gms.wearable;

import android.content.Context;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;

/**
 * The main entry point for Wearable APIs.
 * <p>
 * This class provides access to the Wearable APIs. These APIs allow apps to communicate
 * with Wear OS devices, send and receive data, and synchronize information between
 * handheld and wearable devices.
 */
public class Wearable {

    /**
     * API for interacting with the Wearable Data API.
     */
    public static final Api<DataApi> DATA_API = new Api<>("Wearable.API");

    /**
     * API for interacting with the Wearable Node API.
     */
    public static final Api<NodeApi> NODE_API = new Api<>("Wearable.API");

    /**
     * API for interacting with the Wearable Message API.
     */
    public static final ApiadeshApi> MESSAGE_API = new Api<>("Wearable.API");

    /**
     * API for interacting with the Wearable Capability API.
     */
    public static final Api<CapabilityApi> CAPABILITY_API = new Api<>("Wearable.API");

    /**
     * API for interacting with the Wearable Channel API.
     */
    public static final Api<ChannelApi> CHANNEL_API = new Api<>("Wearable.API");

    private Wearable() {
    }

    /**
     * Gets the Wearable API client.
     */
    public static WearableClient getClient(Context context) {
        return new WearableClientImpl(context);
    }
}

import org.microg.gms.common.PublicApi;
import org.microg.gms.wearable.DataApiImpl;
import org.microg.gms.wearable.MessageApiImpl;
import org.microg.gms.wearable.NodeApiImpl;
import org.microg.gms.wearable.WearableApiClientBuilder;

/**
 * An API for the Android Wear platform.
 */
@PublicApi
public class Wearable {
    /**
     * Token to pass to {@link GoogleApiClient.Builder#addApi(Api)} to enable the Wearable features.
     */
    public static final Api<WearableOptions> API = new Api<WearableOptions>(new WearableApiClientBuilder());

    public static final DataApi DataApi = new DataApiImpl();
    public static final MessageApi MessageApi = new MessageApiImpl();
    public static final NodeApi NodeApi = new NodeApiImpl();

    public static class WearableOptions implements Api.ApiOptions.Optional {
        /**
         * Special option for microG to allow implementation of a FOSS first party Android Wear app
         */
        @PublicApi(exclude = true)
        public boolean firstPartyMode = false;

        public static class Builder {
            public WearableOptions build() {
                return new WearableOptions();
            }
        }
    }
}
