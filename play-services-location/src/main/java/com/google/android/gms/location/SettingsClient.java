/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.location.LocationManager;
import android.provider.Settings;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

/**
 * The main entry point for interacting with the location settings-enabler APIs.
 * <p>
 * This API makes it easy for an app to ensure that the device's system settings are properly configured for the app's
 * location needs.
 */
public interface SettingsClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    /**
     * Checks if the relevant system settings are enabled on the device to carry out the desired location requests.
     *
     * @param locationSettingsRequest an object that contains all the location requirements that the client is interested in.
     */
    Task<LocationSettingsResponse> checkLocationSettings(LocationSettingsRequest locationSettingsRequest);

    /**
     * Returns true if the Google Location Accuracy setting is currently enabled. This setting is required for Fused Location
     * Provider APIs to be able to generate network (wifi, cell, etc) based locations. If Google Play services is chosen as the
     * platform {@link LocationManager#NETWORK_PROVIDER} (this is the case on all GMS compliant devices, which constitute the
     * vast majority of the Android ecosystem), then this setting is also required for the platform
     * {@link LocationManager#NETWORK_PROVIDER} to be enabled.
     * <p>
     * On Android P and above devices, the Google Location Accuracy setting may be found under location settings. Below
     * Android P, Google Location Accuracy is tied to the device location mode - it will be enabled if the device is in
     * {@link Settings.Secure#LOCATION_MODE_BATTERY_SAVING} or {@link Settings.Secure#LOCATION_MODE_HIGH_ACCURACY}, and
     * disabled in {@link Settings.Secure#LOCATION_MODE_SENSORS_ONLY}.
     */
    Task<Boolean> isGoogleLocationAccuracyEnabled();
}
