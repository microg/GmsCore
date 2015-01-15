package org.microg.gms.location;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.os.RemoteException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import org.microg.gms.common.api.GoogleApiClientImpl;

public class LocationClientImpl extends GoogleLocationManagerClient {
    public LocationClientImpl(Context context) {
        super(context);
    }

    public static LocationClientImpl get(GoogleApiClient apiClient) {
        if (apiClient instanceof GoogleApiClientImpl) {
            return (LocationClientImpl) ((GoogleApiClientImpl) apiClient)
                    .getApiConnection(LocationServices.API);
        }
        return null;
    }

    public Location getLastLocation() throws RemoteException {
        return getServiceInterface().getLastLocation();
    }

    public void requestLocationUpdates(LocationRequest request, LocationListener listener)
            throws RemoteException {

    }

    public void requestLocationUpdates(LocationRequest request, PendingIntent pendingIntent)
            throws RemoteException {

    }

    public void requestLocationUpdates(LocationRequest request, LocationListener listener,
            Looper looper) throws RemoteException {

    }

    public void removeLocationUpdates(LocationListener listener) throws RemoteException {

    }

    public void removeLocationUpdates(PendingIntent pendingIntent) throws RemoteException {

    }

    public void setMockMode(boolean isMockMode) throws RemoteException {

    }

    public void setMockLocation(Location mockLocation) throws RemoteException {

    }
}
