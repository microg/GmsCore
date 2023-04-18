/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Response;
import com.google.android.gms.tasks.Task;
import org.microg.gms.common.PublicApi;

/**
 * Successful response of checking settings via {@link SettingsApi#checkLocationSettings(GoogleApiClient, LocationSettingsRequest)}.
 * <p>
 * If a {@link Task} with this response type fails, it will receive a {@link ResolvableApiException} which may be able to resolve the failure.
 * See {@link SettingsClient} for more details.
 * <p>
 * The current location settings states can be accessed via {@link #getLocationSettingsStates()}. See {@link LocationSettingsResult} for more details.
 */
public class LocationSettingsResponse extends Response<LocationSettingsResult> {
    /**
     * Retrieves the location settings states.
     */
    @Nullable
    public LocationSettingsStates getLocationSettingsStates() {
        return getResult().getLocationSettingsStates();
    }

    @PublicApi(exclude = true)
    public LocationSettingsResponse(@NonNull LocationSettingsResult result) {
        super(result);
    }
}
