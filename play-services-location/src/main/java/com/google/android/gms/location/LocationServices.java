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

import android.content.Context;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient.Builder;

import org.microg.gms.location.FusedLocationProviderApiImpl;
import org.microg.gms.location.GeofencingApiImpl;
import org.microg.gms.location.LocationServicesApiClientBuilder;
import org.microg.gms.location.SettingsApiImpl;

/**
 * The main entry point for location services integration.
 */
public class LocationServices {
    /**
     * Token to pass to {@link Builder#addApi(Api)} to enable LocationServices.
     */
    public static final Api<Api.ApiOptions.NoOptions> API = new Api<Api.ApiOptions.NoOptions>(new LocationServicesApiClientBuilder());

    /**
     * Entry point to the fused location APIs.
     */
    @Deprecated
    public static final FusedLocationProviderApi FusedLocationApi = new FusedLocationProviderApiImpl();

    /**
     * Entry point to the geofencing APIs.
     */
    @Deprecated
    public static final GeofencingApi GeofencingApi = new GeofencingApiImpl();

    /**
     * Entry point to the location settings-enabler dialog APIs.
     */
    @Deprecated
    public static final SettingsApi SettingsApi = new SettingsApiImpl();

    public static FusedLocationProviderClient getFusedLocationProviderClient(Context context) {
        return new FusedLocationProviderClient(context);
    }
}
