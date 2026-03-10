/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;

import org.microg.gms.location.FusedLocationProviderApiImpl;
import org.microg.gms.location.FusedLocationProviderClientImpl;
import org.microg.gms.location.GeofencingApiImpl;
import org.microg.gms.location.GeofencingClientImpl;
import org.microg.gms.location.LocationServicesApiClientBuilder;
import org.microg.gms.location.SettingsApiImpl;
import org.microg.gms.location.SettingsClientImpl;

/**
 * The main entry point for location services integration.
 */
public class LocationServices {
    /**
     * Token to pass to {@link GoogleApiClient.Builder#addApi(Api)} to enable LocationServices.
     *
     * @deprecated Use {@link FusedLocationProviderClient} instead.
     */
    @Deprecated
    public static final Api<Api.ApiOptions.NoOptions> API = new Api<Api.ApiOptions.NoOptions>(new LocationServicesApiClientBuilder());

    /**
     * Old entry point to the Fused Location Provider APIs.
     *
     * @deprecated Use {@link FusedLocationProviderClient} instead.
     */
    @Deprecated
    public static final FusedLocationProviderApi FusedLocationApi = new FusedLocationProviderApiImpl();

    /**
     * Old entry point to the geofencing APIs.
     *
     * @deprecated Use {@link GeofencingClient} instead.
     */
    @Deprecated
    public static final GeofencingApi GeofencingApi = new GeofencingApiImpl();

    /**
     * Old entry point to the location settings APIs.
     *
     * @deprecated Use {@link SettingsClient} instead.
     */
    @Deprecated
    public static final SettingsApi SettingsApi = new SettingsApiImpl();

    /**
     * Create a new instance of {@link FusedLocationProviderClient} for use in an {@link Activity}.
     * Error resolutions will be automatically launched from the provided Activity, displaying UI when necessary.
     */
    public static FusedLocationProviderClient getFusedLocationProviderClient(Activity activity) {
        return new FusedLocationProviderClientImpl(activity);
    }

    /**
     * Create a new instance of {@link FusedLocationProviderClient} for use in a non-activity {@link Context}.
     * Error resolutions will be automatically launched from the provided Context, displaying system tray notifications
     * when necessary.
     */
    public static FusedLocationProviderClient getFusedLocationProviderClient(Context context) {
        return new FusedLocationProviderClientImpl(context);
    }

    /**
     * Create a new instance of {@link GeofencingClient} for use in an {@link Activity}.
     * Error resolutions will be automatically launched from the provided Activity, displaying UI when necessary.
     */
    public static GeofencingClient getGeofencingClient(Activity activity) {
        return new GeofencingClientImpl(activity);
    }

    /**
     * Create a new instance of {@link GeofencingClient} for use in a non-activity {@link Context}.
     * Error resolutions will be automatically launched from the provided Context, displaying system tray notifications
     * when necessary.
     */
    public static GeofencingClient getGeofencingClient(Context context) {
        return new GeofencingClientImpl(context);
    }

    /**
     * Create a new instance of {@link SettingsClient} for use in an {@link Activity}.
     * Error resolutions will be automatically launched from the provided Activity, displaying UI when necessary.
     */
    public static SettingsClient getSettingsClient(Activity activity) {
        return new SettingsClientImpl(activity);
    }

    /**
     * Create a new instance of {@link SettingsClient} for use in a non-activity {@link Context}.
     * Error resolutions will be automatically launched from the provided Context, displaying system tray notifications
     * when necessary.
     */
    public static SettingsClient getSettingsClient(Context context) {
        return new SettingsClientImpl(context);
    }
}
