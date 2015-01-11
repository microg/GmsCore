package org.microg.gms.location;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.internal.ILocationListener;

import java.util.HashMap;
import java.util.Map;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static org.microg.gms.maps.Constants.KEY_MOCK_LOCATION;

public class GoogleLocationManager {
    private static final String MOCK_PROVIDER = KEY_MOCK_LOCATION;

    private Context context;
    private LocationManager locationManager;
    private Map<String, Location> lastKnownLocaton = new HashMap<>();

    public GoogleLocationManager(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        updateLastKnownLocation();
    }

    private void updateLastKnownLocation() {
        lastKnownLocaton.put(GPS_PROVIDER, locationManager.getLastKnownLocation(GPS_PROVIDER));
        lastKnownLocaton.put(NETWORK_PROVIDER,
                locationManager.getLastKnownLocation(NETWORK_PROVIDER));
    }

    public Location getLastLocation(String packageName) {
        if (lastKnownLocaton.get(KEY_MOCK_LOCATION) != null)
            return lastKnownLocaton.get(KEY_MOCK_LOCATION);
        if (hasFineLocationPermission()) {
            Location network = lastKnownLocaton.get(NETWORK_PROVIDER);
            Location gps = lastKnownLocaton.get(GPS_PROVIDER);
            if (network == null)
                return gps;
            if (gps == null)
                return network;
            if (gps.getTime() > network.getTime())
                return gps;
            return network;
        } else if (hasCoarseLocationPermission()) {
            return lastKnownLocaton.get(NETWORK_PROVIDER);
        }
        return null;
    }

    private boolean hasCoarseLocationPermission() {
        return context.checkCallingPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasFineLocationPermission() {
        return context.checkCallingPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasMockLocationPermission() {
        return context.checkCallingPermission(Manifest.permission.ACCESS_MOCK_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationUpdates(LocationRequest request, ILocationListener listener,
            String packageName) {

    }

    public void requestLocationUpdates(LocationRequest request, PendingIntent intent) {

    }

    public void removeLocationUpdates(ILocationListener listener) {

    }

    public void removeLocationUpdates(PendingIntent intent) {

    }

    public void setMockMode(boolean mockMode) {
        if (!hasMockLocationPermission())
            return;
        if (!mockMode)
            lastKnownLocaton.put(MOCK_PROVIDER, null);
    }

    public void setMockLocation(Location mockLocation) {
        if (!hasMockLocationPermission())
            return;
        if (mockLocation.getExtras() == null) {
            mockLocation.setExtras(new Bundle());
        }
        mockLocation.getExtras().putBoolean(KEY_MOCK_LOCATION, false);
        lastKnownLocaton.put(MOCK_PROVIDER, mockLocation);
    }
}
