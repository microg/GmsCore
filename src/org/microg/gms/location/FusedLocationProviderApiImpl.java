package org.microg.gms.location;

import android.app.PendingIntent;
import android.location.Location;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.microg.gms.common.GmsConnector;

public class FusedLocationProviderApiImpl implements FusedLocationProviderApi {
    private static final String TAG = "GmsFusedApiImpl";
    
    @Override
    public Location getLastLocation(GoogleApiClient client) {
        try {
            return LocationClientImpl.get(client).getLastLocation();
        } catch (RemoteException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    @Override
    public PendingResult requestLocationUpdates(GoogleApiClient client, LocationRequest request,
            LocationListener listener) {

        //LocationClientImpl.get(client).requestLocationUpdates(request, listener);
        return null;
    }

    @Override
    public PendingResult requestLocationUpdates(GoogleApiClient client, LocationRequest request,
            LocationListener listener, Looper looper) {
        //LocationClientImpl.get(client).requestLocationUpdates(request, listener, looper);
        return null;
    }

    @Override
    public PendingResult requestLocationUpdates(GoogleApiClient client, LocationRequest request,
            PendingIntent callbackIntent) {
        //LocationClientImpl.get(client).requestLocationUpdates(request, callbackIntent);
        return null;
    }

    @Override
    public PendingResult removeLocationUpdates(GoogleApiClient client, LocationListener listener) {
        //LocationClientImpl.get(client).removeLocationUpdates(listener);
        return null;
    }

    @Override
    public PendingResult removeLocationUpdates(GoogleApiClient client,
            PendingIntent callbackIntent) {
        //LocationClientImpl.get(client).removeLocationUpdates(callbackIntent);
        return null;
    }

    @Override
    public PendingResult setMockMode(GoogleApiClient client, boolean isMockMode) {
        //LocationClientImpl.get(client).setMockMode(isMockMode);
        return null;
    }

    @Override
    public PendingResult setMockLocation(GoogleApiClient client, Location mockLocation) {
        //LocationClientImpl.get(client).setMockLocation(mockLocation);
        return null;
    }
}
