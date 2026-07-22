/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.app.Activity;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

/**
 * Result of checking settings via {@link SettingsApi#checkLocationSettings(GoogleApiClient, LocationSettingsRequest)},
 * indicates whether a dialog should be shown to ask the user's consent to change their
 * settings.
 * <p>
 * The method {@link #getStatus()} can be used to confirm if the request was successful. If the current location settings don't
 * satisfy the app's requirements and the user has permission to change the settings, the app could use
 * {@link Status#startResolutionForResult(Activity, int)} to start an intent to show a dialog, asking for user's consent to
 * change the settings.
 * <p>
 * The current location settings states can be accessed via {@link #getLocationSettingsStates()}. See
 * {@link LocationSettingsStates} for more details.
 */
@PublicApi
@SafeParcelable.Class
public class LocationSettingsResult extends AbstractSafeParcelable implements Result {

    @Field(1000)
    int versionCode = 1;

    @Field(value = 1, getterName = "getStatus")
    @NonNull
    private final Status status;

    @Field(value = 2, getterName = "getLocationSettingsStates")
    @Nullable
    private final LocationSettingsStates settings;


    /**
     * Retrieves the location settings states.
     */
    @Nullable
    public LocationSettingsStates getLocationSettingsStates() {
        return settings;
    }

    @Override
    @NonNull
    public Status getStatus() {
        return status;
    }

    @Hide
    @Constructor
    public LocationSettingsResult(@Param(1) @NonNull Status status, @Param(2) @Nullable LocationSettingsStates settings) {
        this.settings = settings;
        this.status = status;
    }

    @Hide
    public LocationSettingsResult(@NonNull Status status) {
        this(status, null);
    }

    public static final SafeParcelableCreatorAndWriter<LocationSettingsResult> CREATOR = findCreator(LocationSettingsResult.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
