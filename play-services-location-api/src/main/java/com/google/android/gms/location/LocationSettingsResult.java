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

package com.google.android.gms.location;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * Result of checking settings via checkLocationSettings(GoogleApiClient, LocationSettingsRequest),
 * indicates whether a dialog should be shown to ask the user's consent to change their settings.
 * The method getStatus() can be be used to confirm if the request was successful. If the current
 * location settings don't satisfy the app's requirements and the user has permission to change the
 * settings, the app could use startResolutionForResult(Activity, int) to start an intent to show a
 * dialog, asking for user's consent to change the settings. The current location settings states
 * can be accessed via getLocationSettingsStates(). See LocationSettingsResult for more details.
 */
@PublicApi
public class LocationSettingsResult extends AutoSafeParcelable implements Result {

    @SafeParceled(1000)
    private final int versionCode = 1;

    @SafeParceled(1)
    private final Status status;

    @SafeParceled(2)
    private LocationSettingsStates settings;


    /**
     * Retrieves the location settings states.
     */
    public LocationSettingsStates getLocationSettingsStates() {
        return settings;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @PublicApi(exclude = true)
    public LocationSettingsResult(LocationSettingsStates settings, Status status) {
        this.settings = settings;
        this.status = status;
    }

    @PublicApi(exclude = true)
    public LocationSettingsResult(Status status) {
        this.status = status;
    }

    public static final Creator<LocationSettingsResult> CREATOR = new AutoCreator<LocationSettingsResult>(LocationSettingsResult.class);
}
