package org.microg.gms.location;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RealLocationProvider {

    public static final String TAG = "GmsRealLocationProvider";
    private Location lastLocation;
    private LocationManager locationManager;
    private String name;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private long connectedMinTime;
    private float connectedMinDistance;
    private List<LocationRequestHelper> requests = new ArrayList<>();
    private final LocationChangeListener changeListener;
    private LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastLocation = location;
            changeListener.onLocationChanged();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    public RealLocationProvider(LocationManager locationManager, String name,
            LocationChangeListener changeListener) {
        this.locationManager = locationManager;
        this.name = name;
        this.changeListener = changeListener;
        updateLastLocation();
    }

    private void updateLastLocation() {
        lastLocation = locationManager.getLastKnownLocation(name);
    }

    public Location getLastLocation() {
        if (!connected.get()) {
            updateLastLocation();
        }
        return lastLocation;
    }

    public void addRequest(LocationRequestHelper request) {
        Log.d(TAG, name + ": addRequest " + request);
        requests.add(request);
        updateConnection();
    }

    public void removeRequest(LocationRequestHelper request) {
        Log.d(TAG, name + ": removeRequest " + request);
        requests.remove(request);
        updateConnection();
    }

    private synchronized void updateConnection() {
        if (connected.get() && requests.isEmpty()) {
            Log.d(TAG, name + ": no longer requesting location update");
            locationManager.removeUpdates(listener);
            connected.set(false);
        } else if (!requests.isEmpty()) {
            long minTime = Long.MAX_VALUE;
            float minDistance = Float.MAX_VALUE;
            for (LocationRequestHelper request : requests) {
                minTime = Math.min(request.locationRequest.getInterval(), minTime);
                minDistance = Math
                        .min(request.locationRequest.getSmallestDesplacement(), minDistance);
            }
            if (connected.get()) {
                if (connectedMinTime != minTime || connectedMinDistance != minDistance) {
                    locationManager.removeUpdates(listener);
                    locationManager.requestLocationUpdates(name, minTime, minDistance, listener,
                            Looper.getMainLooper());
                }
            } else {
                locationManager.requestLocationUpdates(name, minTime, minDistance, listener,
                        Looper.getMainLooper());
            }
            Log.d(TAG,
                    name + ": requesting location updates. minTime=" + minTime + " minDistance=" +
                            minDistance);
            connected.set(true);
            connectedMinTime = minTime;
            connectedMinDistance = minDistance;
        }
    }
}
