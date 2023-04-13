/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
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
