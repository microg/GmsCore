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

import com.google.android.gms.common.api.CommonStatusCodes;

import org.microg.gms.common.PublicApi;

/**
 * Error codes for wearable API failures. These values may be returned by APIs to indicate the
 * success or failure of a request.
 */
@PublicApi
public class WearableStatusCodes extends CommonStatusCodes {
    /**
     * Indicates that the targeted node is not accessible in the wearable network.
     */
    public static final int TARGET_NODE_NOT_CONNECTED = 4000;
    /**
     * Indicates that the specified listener is already registered.
     */
    public static final int DUPLICATE_LISTENER = 4001;
    /**
     * Indicates that the specified listener is not recognized.
     */
    public static final int UNKNOWN_LISTENER = 4002;
    /**
     * Indicates that the data item was too large to set.
     */
    public static final int DATA_ITEM_TOO_LARGE = 4003;
    /**
     * Indicates that the targeted node is not a valid node in the wearable network.
     */
    public static final int INVALID_TARGET_NODE = 4004;
    /**
     * Indicates that the requested asset is unavailable.
     */
    public static final int ASSET_UNAVAILABLE = 4005;
    /**
     * Indicates that the specified capability already exists.
     */
    public static final int DUPLICATE_CAPABILITY = 4006;
    /**
     * Indicates that the specified capability is not recognized.
     */
    public static final int UNKNOWN_CAPABILITY = 4007;
    /**
     * Indicates that the WiFi credential sync no credential fetched.
     */
    public static final int WIFI_CREDENTIAL_SYNC_NO_CREDENTIAL_FETCHED = 4008;
}
