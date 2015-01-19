package com.google.android.gms.location;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.os.RemoteException;

import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;

import org.microg.gms.common.ForwardConnectionCallbacks;
import org.microg.gms.common.ForwardConnectionFailedListener;
import org.microg.gms.common.api.AbstractPlayServicesClient;
import org.microg.gms.common.api.GoogleApiClientImpl;
import org.microg.gms.location.LocationClientImpl;

@Deprecated
public class LocationClient extends AbstractPlayServicesClient {
    public static final String KEY_LOCATION_CHANGED = "com.google.android.location.LOCATION";

    public LocationClient(Context context, ConnectionCallbacks callbacks,
                          OnConnectionFailedListener connectionFailedListener) {
        super(new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new ForwardConnectionCallbacks(callbacks))
                .addOnConnectionFailedListener(new ForwardConnectionFailedListener(connectionFailedListener))
                .build());
    }

    public Location getLastLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }

    public PendingResult requestLocationUpdates(LocationRequest request, LocationListener listener) {
        return LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, listener);
    }

    public PendingResult requestLocationUpdates(LocationRequest request, LocationListener listener, Looper looper) {
        return LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, listener, looper);
    }

    public PendingResult requestLocationUpdates(LocationRequest request, PendingIntent callbackIntent) {
        return LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, callbackIntent);
    }

    public PendingResult removeLocationUpdates(LocationListener listener) {
        return LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, listener);
    }

    public PendingResult removeLocationUpdates(PendingIntent callbackIntent) {
        return LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, callbackIntent);
    }

    public PendingResult setMockMode(boolean isMockMode) {
        return LocationServices.FusedLocationApi.setMockMode(googleApiClient, isMockMode);
    }

    public PendingResult setMockLocation(Location mockLocation) {
        return LocationServices.FusedLocationApi.setMockLocation(googleApiClient, mockLocation);
    }
}
