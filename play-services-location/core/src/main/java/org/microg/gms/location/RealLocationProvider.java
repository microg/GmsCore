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

package org.microg.gms.location;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("MissingPermission")
public class RealLocationProvider {
    public static final String TAG = "GmsLocProviderReal";
    private static final int MIN_GPS_TIME = 60000;

    private final LocationManager locationManager;
    private final String name;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final LocationChangeListener changeListener;

    private long connectedMinTime;
    private float connectedMinDistance;
    private Location lastLocation;
    private final List<LocationRequestHelper> requests = new ArrayList<LocationRequestHelper>();
    private LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastLocation = new Location(location);
            try {
                lastLocation.getExtras().keySet(); // call to unparcel()
            } catch (Exception e) {
                // Sometimes we need to define the correct ClassLoader before unparcel(). Ignore those.
            }
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

    public RealLocationProvider(LocationManager locationManager, String name, LocationChangeListener changeListener) {
        this.locationManager = locationManager;
        this.name = name;
        this.changeListener = changeListener;
        updateLastLocation();
    }

    private void updateLastLocation() {
        Location newLocation = locationManager.getLastKnownLocation(name);
        if (newLocation != null) lastLocation = newLocation;
    }

    public void invokeOnceReady(Runnable runnable) {
        // Always ready
        runnable.run();
    }

    public Location getLastLocation() {
        if (!connected.get()) {
            updateLastLocation();
        }
        if (lastLocation == null) {
            Log.d(TAG, "uh-ok: last location for " + name + " is null!");
        }
        return lastLocation;
    }

    public void addRequest(LocationRequestHelper request) {
        Log.d(TAG, name + ": addRequest " + request);
        for (int i = 0; i < requests.size(); i++) {
            LocationRequestHelper req = requests.get(i);
            if (req.respondsTo(request.pendingIntent) || req.respondsTo(request.listener) || req.respondsTo(request.callback)) {
                requests.remove(i);
                i--;
            }
        }
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
            StringBuilder sb = new StringBuilder();
            for (LocationRequestHelper request : requests) {
                minTime = Math.min(request.locationRequest.getInterval(), minTime);
                minDistance = Math.min(request.locationRequest.getSmallestDisplacement(), minDistance);
                if (sb.length() != 0) sb.append(", ");
                sb.append(request.packageName).append(":").append(request.locationRequest.getInterval()).append("ms");
            }
            if (minTime > MIN_GPS_TIME && name.equals("gps")) {
                Log.d(TAG, name + ": ignoring request as " + minTime + "ms (" + sb + "), is less than " + MIN_GPS_TIME);
                locationManager.removeUpdates(listener);
                connected.set(false);
                return;
            }
            Log.d(TAG, name + ": requesting location updates with interval " + minTime + "ms (" + sb + "), minDistance=" + minDistance);
            if (connected.get()) {
                if (connectedMinTime != minTime || connectedMinDistance != minDistance) {
                    locationManager.removeUpdates(listener);
                    locationManager.requestLocationUpdates(name, minTime, minDistance, listener,
                            Looper.getMainLooper());
                }
            } else {
                locationManager.requestLocationUpdates(name, minTime, minDistance, listener, Looper.getMainLooper());
            }
            connected.set(true);
            connectedMinTime = minTime;
            connectedMinDistance = minDistance;
        }
    }

    public void dump(PrintWriter writer) {
        if (writer != null) {
            writer.println(name + " provider:");
            writer.println("  last location: " + lastLocation);
            writer.println("  active: " + connected.get());
            if (connected.get()) {
                writer.println("  interval: " + connectedMinTime);
                writer.println("  distance: " + connectedMinDistance);
            }
        }
    }
}
