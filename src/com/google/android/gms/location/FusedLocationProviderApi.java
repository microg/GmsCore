package com.google.android.gms.location;

import android.app.PendingIntent;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;

import org.microg.gms.common.Constants;

public interface FusedLocationProviderApi {
    public static final String KEY_LOCATION_CHANGED = "com.google.android.location.LOCATION";
    public static final String KEY_MOCK_LOCATION = Constants.KEY_MOCK_LOCATION;

    public Location getLastLocation(GoogleApiClient client);

    public PendingResult requestLocationUpdates(GoogleApiClient client, LocationRequest request,
            LocationListener listener);

    public PendingResult requestLocationUpdates(GoogleApiClient client, LocationRequest request,
            LocationListener listener, Looper looper);

    public PendingResult requestLocationUpdates(GoogleApiClient client, LocationRequest request,
            PendingIntent callbackIntent);

    public PendingResult removeLocationUpdates(GoogleApiClient client, LocationListener listener);

    public PendingResult removeLocationUpdates(GoogleApiClient client,
            PendingIntent callbackIntent);

    public PendingResult setMockMode(GoogleApiClient client, boolean isMockMode);

    public PendingResult setMockLocation(GoogleApiClient client, Location mockLocation);
}
