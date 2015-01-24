package org.microg.gms.location;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.microg.gms.common.api.GoogleApiClientImpl;

import java.util.HashMap;
import java.util.Map;

public class LocationClientImpl extends GoogleLocationManagerClient {
    private static final String TAG = "GmsLocationClientImpl";

    public LocationClientImpl(Context context, GoogleApiClient.ConnectionCallbacks callbacks,
            GoogleApiClient.OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener);
        Log.d(TAG, "<init>");
    }

    public static LocationClientImpl get(GoogleApiClient apiClient) {
        if (apiClient instanceof GoogleApiClientImpl) {
            return (LocationClientImpl) ((GoogleApiClientImpl) apiClient)
                    .getApiConnection(LocationServices.API);
        }
        return null;
    }

    private Map<LocationListener, ILocationListener> listenerMap = new HashMap<>();

    public Location getLastLocation() throws RemoteException {
        Log.d(TAG, "getLastLocation()");
        return getServiceInterface().getLastLocation();
    }

    public void requestLocationUpdates(LocationRequest request, final LocationListener listener)
            throws RemoteException {
        ILocationListener iLocationListener = new ILocationListener.Stub() {
            @Override
            public void onLocationChanged(Location location) throws RemoteException {
                listener.onLocationChanged(location);
            }
        };
        listenerMap.put(listener, iLocationListener);
        getServiceInterface().requestLocationUpdatesWithPackage(request,
                iLocationListener, getContext().getPackageName());
    }

    public void requestLocationUpdates(LocationRequest request, PendingIntent pendingIntent)
            throws RemoteException {
        getServiceInterface().requestLocationUpdatesWithIntent(request, pendingIntent);
    }

    public void requestLocationUpdates(LocationRequest request, LocationListener listener,
            Looper looper) throws RemoteException {
        requestLocationUpdates(request, listener); // TODO
    }

    public void removeLocationUpdates(LocationListener listener) throws RemoteException {
        getServiceInterface().removeLocationUpdatesWithListener(listenerMap.get(listener));
        listenerMap.remove(listener);
    }

    public void removeLocationUpdates(PendingIntent pendingIntent) throws RemoteException {
        getServiceInterface().removeLocationUpdatesWithIntent(pendingIntent);
    }

    public void setMockMode(boolean isMockMode) throws RemoteException {
        getServiceInterface().setMockMode(isMockMode);
    }

    public void setMockLocation(Location mockLocation) throws RemoteException {
        getServiceInterface().setMockLocation(mockLocation);
    }
}
