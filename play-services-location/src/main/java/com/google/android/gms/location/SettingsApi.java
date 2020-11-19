/*
 * Copyright (C) 2017 microG Project Team
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

package com.google.android.gms.location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;

/**
 * The main entry point for interacting with the location settings-enabler APIs.
 * <p>
 * This API makes it easy for an app to ensure that the device's system settings are properly
 * configured for the app's location needs.
 */
@Deprecated
public interface SettingsApi {
    /**
     * Checks if the relevant system settings are enabled on the device to carry out the desired
     * location requests.
     *
     * @param client                  an existing GoogleApiClient. It does not need to be connected
     *                                at the time of this call, but the result will be delayed until
     *                                the connection is complete.
     * @param locationSettingsRequest an object that contains all the location requirements that the
     *                                client is interested in.
     * @return result containing the status of the request.
     */
    PendingResult<LocationSettingsResult> checkLocationSettings(GoogleApiClient client, LocationSettingsRequest locationSettingsRequest);
}
