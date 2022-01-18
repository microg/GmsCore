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

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.internal.FusedLocationProviderResult;
import com.google.android.gms.location.internal.LocationRequestUpdateData;

import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.Utils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.location.LocationManager.GPS_PROVIDER;
import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;
import static com.google.android.gms.location.LocationRequest.PRIORITY_NO_POWER;

import androidx.lifecycle.Lifecycle;

public class GoogleLocationManager implements LocationChangeListener {
    private static final String TAG = "LocationManager";
    private static final String MOCK_PROVIDER = "mock";
    private static final long VERIFY_CURRENT_REQUESTS_INTERVAL_MS = 5000; // 5 seconds
    private static final long SWITCH_ON_FRESHNESS_CLIFF_MS = 30000; // 30 seconds
    private static final String ACCESS_MOCK_LOCATION = "android.permission.ACCESS_MOCK_LOCATION";

    private final Context context;
    private final Handler handler;
    private final Runnable verifyCurrentRequestsRunnable = this::verifyCurrentRequests;
    private final RealLocationProvider gpsProvider;
    private final UnifiedLocationProvider networkProvider;
    private final MockLocationProvider mockProvider;
    private final List<LocationRequestHelper> currentRequests = new ArrayList<LocationRequestHelper>();

    public GoogleLocationManager(Context context, Lifecycle lifecycle) {
        long callingIdentity = Binder.clearCallingIdentity();
        this.context = context;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (Utils.hasSelfPermissionOrNotify(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            this.gpsProvider = new RealLocationProvider(locationManager, GPS_PROVIDER, this);
        } else {
            this.gpsProvider = null;
        }
        if (Utils.hasSelfPermissionOrNotify(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            this.networkProvider = new UnifiedLocationProvider(context, this, lifecycle);
        } else {
            this.networkProvider = null;
        }
        mockProvider = new MockLocationProvider(this);
        handler = new Handler(Looper.getMainLooper());
        Binder.restoreCallingIdentity(callingIdentity);
    }

    public void invokeOnceReady(Runnable runnable) {
        Runnable networkRunnable = () -> {
            if (networkProvider != null) {
                networkProvider.invokeOnceReady(runnable);
            } else {
                runnable.run();
            }
        };
        if (gpsProvider != null) {
            gpsProvider.invokeOnceReady(networkRunnable);
        } else {
            networkRunnable.run();
        }
    }

    public Location getLastLocation(String packageName) {
        return getLocation(hasFineLocationPermission(), hasCoarseLocationPermission());
    }

    public Location getLocation(boolean gpsPermission, boolean networkPermission) {
        if (mockProvider.getLocation() != null)
            return mockProvider.getLocation();
        if (gpsPermission) {
            Location network = networkProvider == null ? null : networkProvider.getLastLocation();
            Location gps = gpsProvider == null ? null : gpsProvider.getLastLocation();
            if (network == null)
                return gps;
            if (gps == null)
                return network;
            if (gps.getTime() > network.getTime() - SWITCH_ON_FRESHNESS_CLIFF_MS)
                return gps;
            return network;
        } else if (networkPermission) {
            Location network = networkProvider == null ? null : networkProvider.getLastLocation();
            if (network != null && network.getExtras() != null && network.getExtras().getParcelable("no_gps_location") instanceof Location) {
                network = network.getExtras().getParcelable("no_gps_location");
            }
            return network;
        }
        return null;
    }

    private boolean hasCoarseLocationPermission() {
        return context.checkCallingPermission(ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED || hasFineLocationPermission();
    }

    private boolean hasFineLocationPermission() {
        return context.checkCallingPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED;
    }

    private boolean hasMockLocationPermission() {
        return context.checkCallingPermission(ACCESS_MOCK_LOCATION) == PERMISSION_GRANTED;
    }

    private void requestLocationUpdates(LocationRequestHelper request) {
        LocationRequestHelper old = null;
        for (LocationRequestHelper req : currentRequests) {
            if (req.respondsTo(request.pendingIntent) || req.respondsTo(request.listener) || req.respondsTo(request.callback)) {
                old = req;
                break;
            }
        }
        if (old != null) {
            Log.d(TAG, "Removing replaced location request: " + old);
            currentRequests.remove(old);
        }
        currentRequests.add(request);
        if (gpsProvider != null && request.hasFinePermission() && request.locationRequest.getPriority() == PRIORITY_HIGH_ACCURACY) {
            Log.d(TAG, "Registering request with high accuracy location provider");
            gpsProvider.addRequest(request);
        } else if (gpsProvider != null && old != null) {
            Log.d(TAG, "Unregistering request with high accuracy location provider");
            gpsProvider.removeRequest(old);
        } else {
            Log.w(TAG, "Not providing high accuracy location: missing permission");
        }
        if (networkProvider != null && request.hasCoarsePermission() && request.locationRequest.getPriority() != PRIORITY_NO_POWER) {
            Log.d(TAG, "Registering request with low accuracy location provider");
            networkProvider.addRequest(request);
        } else if (networkProvider != null && old != null) {
            Log.d(TAG, "Unregistering request with low accuracy location provider");
            networkProvider.removeRequest(old);
        } else {
            Log.w(TAG, "Not providing low accuracy location: missing permission");
        }
        handler.postDelayed(this::onLocationChanged, request.locationRequest.getFastestInterval());
    }

    public void requestLocationUpdates(LocationRequest request, ILocationListener listener, String packageName) {
        requestLocationUpdates(new LocationRequestHelper(context, request, packageName, Binder.getCallingUid(), listener));
    }

    public void requestLocationUpdates(LocationRequest request, PendingIntent intent, String packageName) {
        requestLocationUpdates(new LocationRequestHelper(context, request, packageName, Binder.getCallingUid(), intent));
    }

    private void removeLocationUpdates(LocationRequestHelper request) {
        currentRequests.remove(request);
        if (gpsProvider != null) gpsProvider.removeRequest(request);
        if (networkProvider != null) networkProvider.removeRequest(request);
    }

    public void removeLocationUpdates(ILocationListener listener, String packageName) {
        for (int i = 0; i < currentRequests.size(); i++) {
            if (currentRequests.get(i).respondsTo(listener)) {
                removeLocationUpdates(currentRequests.get(i));
                i--;
            }
        }
    }

    public void removeLocationUpdates(PendingIntent intent, String packageName) {
        for (int i = 0; i < currentRequests.size(); i++) {
            if (currentRequests.get(i).respondsTo(intent)) {
                removeLocationUpdates(currentRequests.get(i));
                i--;
            }
        }
    }

    public void updateLocationRequest(LocationRequestUpdateData data) {
        try {
            Log.d(TAG, "updateLocationRequest: " + data);
            String packageName = PackageUtils.getCallingPackage(context);
            if (data.pendingIntent != null)
                packageName = PackageUtils.packageFromPendingIntent(data.pendingIntent);
            Log.d(TAG, "Using source package: " + packageName);
            if (data.opCode == LocationRequestUpdateData.REQUEST_UPDATES) {
                requestLocationUpdates(new LocationRequestHelper(context, packageName, Binder.getCallingUid(), data));
            } else if (data.opCode == LocationRequestUpdateData.REMOVE_UPDATES) {
                for (int i = 0; i < currentRequests.size(); i++) {
                    if (currentRequests.get(i).respondsTo(data.listener)
                            || currentRequests.get(i).respondsTo(data.pendingIntent)
                            || currentRequests.get(i).respondsTo(data.callback)) {
                        removeLocationUpdates(currentRequests.get(i));
                        i--;
                    }
                }
            }
            Log.d(TAG, "Updated current requests, verifying");
            verifyCurrentRequests();
            if (data.fusedLocationProviderCallback != null) {
                try {
                    Log.d(TAG, "Send success result to " + packageName);
                    data.fusedLocationProviderCallback.onFusedLocationProviderResult(FusedLocationProviderResult.SUCCESS);
                } catch (RemoteException ignored) {
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception in updateLocationRequest", e);
            if (data.fusedLocationProviderCallback != null) {
                try {
                    Log.d(TAG, "Send internal error result");
                    data.fusedLocationProviderCallback.onFusedLocationProviderResult(FusedLocationProviderResult.create(Status.INTERNAL_ERROR));
                } catch (RemoteException ignored) {
                }
            }
        }
    }

    public void setMockMode(boolean mockMode) {
        if (!hasMockLocationPermission())
            return;
        mockProvider.setMockEnabled(mockMode);
    }

    public void setMockLocation(Location mockLocation) {
        if (!hasMockLocationPermission())
            return;
        mockProvider.setLocation(mockLocation);
    }

    private void verifyCurrentRequests() {
        handler.removeCallbacks(verifyCurrentRequestsRunnable);
        try {
            for (int i = 0; i < currentRequests.size(); i++) {
                LocationRequestHelper request = currentRequests.get(i);
                if (!request.isActive()) {
                    removeLocationUpdates(request);
                    i--;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        handler.postDelayed(verifyCurrentRequestsRunnable, VERIFY_CURRENT_REQUESTS_INTERVAL_MS);
    }

    @Override
    public void onLocationChanged() {
        for (int i = 0; i < currentRequests.size(); i++) {
            LocationRequestHelper request = currentRequests.get(i);
            if (!request.report(getLocation(request.initialHasFinePermission, request.initialHasCoarsePermission))) {
                removeLocationUpdates(request);
                i--;
            }
        }
    }

    public void dump(PrintWriter writer) {
        if (gpsProvider != null) gpsProvider.dump(writer);
        if (networkProvider != null) networkProvider.dump(writer);
        writer.println(currentRequests.size() + " requests:");
        for (LocationRequestHelper request : currentRequests) {
            writer.println("  " + request.id + " package=" + request.packageName + " interval=" + request.locationRequest.getInterval() + " smallestDisplacement=" + request.locationRequest.getSmallestDisplacement());
        }
    }
}
