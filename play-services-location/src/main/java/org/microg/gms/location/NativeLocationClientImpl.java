/*
 * Copyright 2013-2015 microG Project Team
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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.HashMap;
import java.util.Map;

public class NativeLocationClientImpl {
    private final static String TAG = "GmsToNativeLocClient";
    private final static Criteria DEFAULT_CRITERIA = new Criteria();
    private final static Map<PendingIntent, Integer> pendingCount = new HashMap<PendingIntent, Integer>();
    private final static Map<PendingIntent, PendingIntent> nativePendingMap = new HashMap<PendingIntent, PendingIntent>();
    private static final String EXTRA_PENDING_INTENT = "pending_intent";

    private final Context context;
    private final LocationManager locationManager;
    private final Map<LocationListener, NativeListener> nativeListenerMap = new HashMap<LocationListener, NativeListener>();

    public NativeLocationClientImpl(LocationClientImpl client) {
        context = client.getContext();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    private static Criteria makeNativeCriteria(LocationRequest request) {
        Criteria criteria = new Criteria();
        switch (request.getPriority()) {
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setPowerRequirement(Criteria.POWER_HIGH);
                break;
            case LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY:
            default:
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
                break;
            case LocationRequest.PRIORITY_NO_POWER:
            case LocationRequest.PRIORITY_LOW_POWER:
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                criteria.setPowerRequirement(Criteria.POWER_LOW);
        }
        return criteria;
    }

    public Location getLastLocation() {
        Log.d(TAG, "getLastLocation()");
        return locationManager.getLastKnownLocation(
                locationManager.getBestProvider(DEFAULT_CRITERIA, true));
    }

    public void requestLocationUpdates(LocationRequest request, LocationListener listener) {
        requestLocationUpdates(request, listener, Looper.getMainLooper());
    }

    public void requestLocationUpdates(LocationRequest request, PendingIntent pendingIntent) {
        Log.d(TAG, "requestLocationUpdates()");
        Intent i = new Intent(context, NativePendingIntentForwarder.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_PENDING_INTENT, pendingIntent);
        i.putExtras(bundle);
        pendingCount.put(pendingIntent, request.getNumUpdates());
        nativePendingMap.put(pendingIntent, PendingIntent.getActivity(context, 0, i, 0));
        locationManager.requestLocationUpdates(request.getInterval(),
                request.getSmallestDesplacement(), makeNativeCriteria(request),
                nativePendingMap.get(pendingIntent));
    }

    public void requestLocationUpdates(LocationRequest request, LocationListener listener, Looper
            looper) {
        Log.d(TAG, "requestLocationUpdates()");
        if (nativeListenerMap.containsKey(listener)) {
            removeLocationUpdates(listener);
        }
        nativeListenerMap.put(listener, new NativeListener(listener, request.getNumUpdates()));
        locationManager.requestLocationUpdates(request.getInterval(),
                request.getSmallestDesplacement(), makeNativeCriteria(request),
                nativeListenerMap.get(listener), looper);
    }

    public void removeLocationUpdates(LocationListener listener) {
        Log.d(TAG, "removeLocationUpdates()");
        locationManager.removeUpdates(nativeListenerMap.get(listener));
        nativeListenerMap.remove(listener);
    }

    public void removeLocationUpdates(PendingIntent pendingIntent) {
        Log.d(TAG, "removeLocationUpdates()");
        locationManager.removeUpdates(nativePendingMap.get(pendingIntent));
        nativePendingMap.remove(pendingIntent);
        pendingCount.remove(pendingIntent);
    }

    public void setMockMode(boolean isMockMode) {
        Log.d(TAG, "setMockMode()");
        // not yet supported
    }

    public void setMockLocation(Location mockLocation) {
        Log.d(TAG, "setMockLocation()");
        // not yet supported
    }

    public static class NativePendingIntentForwarder extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
                PendingIntent pendingIntent = intent.getExtras().getParcelable
                        (EXTRA_PENDING_INTENT);
                try {
                    intent.putExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED,
                            intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED));
                    pendingIntent.send(context, 0, intent);
                    pendingCount.put(pendingIntent, pendingCount.get(pendingIntent) - 1);
                    if (pendingCount.get(pendingIntent) == 0) {
                        ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE))
                                .removeUpdates(nativePendingMap.get(pendingIntent));
                        nativePendingMap.remove(pendingIntent);
                        pendingCount.remove(pendingIntent);
                    }
                } catch (PendingIntent.CanceledException e) {
                    ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE))
                            .removeUpdates(nativePendingMap.get(pendingIntent));
                    nativePendingMap.remove(pendingIntent);
                    pendingCount.remove(pendingIntent);
                }
            }
        }
    }

    public class NativeListener implements android.location.LocationListener {

        private final LocationListener listener;
        private int count;

        private NativeListener(LocationListener listener, int count) {
            this.listener = listener;
            this.count = count;
        }

        @Override
        public void onLocationChanged(Location location) {
            listener.onLocationChanged(location);
            count--;
            if (count == 0) {
                locationManager.removeUpdates(this);
                nativeListenerMap.remove(listener);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NativeListener that = (NativeListener) o;

            if (!listener.equals(that.listener)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }
    }
}
