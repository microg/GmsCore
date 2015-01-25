package com.google.android.gms.location;

import com.google.android.gms.common.api.Api;

import org.microg.gms.location.FusedLocationProviderApiImpl;
import org.microg.gms.location.GeofencingApiImpl;
import org.microg.gms.location.LocationServicesApiBuilder;

/**
 * The main entry point for location services integration.
 */
public class LocationServices {
    public static final Api<Api.ApiOptions.NoOptions> API = new Api<>(new
            LocationServicesApiBuilder());
    public static final FusedLocationProviderApi FusedLocationApi = new
            FusedLocationProviderApiImpl();
    public static final GeofencingApi GeofencingApi = new GeofencingApiImpl();
}
