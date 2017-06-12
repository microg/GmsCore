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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.location.ILocationCallback;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.internal.IFusedLocationProviderCallback;
import com.google.android.gms.location.internal.LocationRequestUpdateData;
import com.google.android.gms.location.internal.FusedLocationProviderResult;

import com.google.android.gms.common.api.Status;

import java.util.Arrays;
import java.util.Collections;

public class LocationRequestHelper {
    public static final String TAG = "GmsLocRequestHelper";
    private final Context context;
    public final LocationRequest locationRequest;
    public final boolean hasFinePermission;
    public final boolean hasCoarsePermission;
    public final String packageName;
    private ILocationListener listener;
    private PendingIntent pendingIntent;
    private ILocationCallback callback;

    private Location lastReport;
    private int numReports = 0;

    private LocationRequestHelper(Context context, LocationRequest locationRequest, boolean hasFinePermission,
                                  boolean hasCoarsePermission, String packageName) {
        this.context = context;
        this.locationRequest = locationRequest;
        this.hasFinePermission = hasFinePermission;
        this.hasCoarsePermission = hasCoarsePermission;
        this.packageName = packageName;
    }

    public LocationRequestHelper(Context context, LocationRequest locationRequest, boolean hasFinePermission,
                                 boolean hasCoarsePermission, String packageName, ILocationListener listener) {
        this(context, locationRequest, hasFinePermission, hasCoarsePermission, packageName);
        this.listener = listener;
    }

    public LocationRequestHelper(Context context, LocationRequest locationRequest, boolean hasFinePermission,
                                 boolean hasCoarsePermission, String packageName, PendingIntent pendingIntent) {
        this(context, locationRequest, hasFinePermission, hasCoarsePermission, packageName);
        this.pendingIntent = pendingIntent;
    }

    public LocationRequestHelper(Context context, boolean hasFinePermission, boolean hasCoarsePermission,
                                 String packageName, LocationRequestUpdateData data) {
        this(context, data.request.request, hasFinePermission, hasCoarsePermission, packageName);
        this.listener = data.listener;
        this.pendingIntent = data.pendingIntent;
        this.callback = data.callback;
    }

    /**
     * @return whether to continue sending reports to this {@link LocationRequestHelper}
     */
    public boolean report(Location location) {
        if (location == null) return true;
        if (lastReport != null) {
            if (location.getTime() - lastReport.getTime() < locationRequest.getFastestInterval()) {
                return true;
            }
            if (location.distanceTo(lastReport) < locationRequest.getSmallestDesplacement()) {
                return true;
            }
        }
        lastReport = new Location(location);
        lastReport.setProvider("fused");
        Log.d(TAG, "sending Location: " + location);
        if (listener != null) {
            try {
                listener.onLocationChanged(lastReport);
            } catch (RemoteException e) {
                return false;
            }
        } else if (pendingIntent != null) {
            Intent intent = new Intent();
            intent.putExtra("com.google.android.location.LOCATION", lastReport);
            try {
                pendingIntent.send(context, 0, intent);
            } catch (PendingIntent.CanceledException e) {
                return false;
            }
        } else if (callback != null) {
            try {
                callback.onLocationResult(LocationResult.create(Arrays.asList(lastReport)));
            } catch (RemoteException e) {
                return false;
            }
        }
        numReports++;
        return numReports < locationRequest.getNumUpdates();
    }

    @Override
    public String toString() {
        return "LocationRequestHelper{" +
                "locationRequest=" + locationRequest +
                ", hasFinePermission=" + hasFinePermission +
                ", hasCoarsePermission=" + hasCoarsePermission +
                ", packageName='" + packageName + '\'' +
                ", lastReport=" + lastReport +
                '}';
    }

    public boolean respondsTo(ILocationListener listener) {
        return this.listener != null && listener != null &&
                this.listener.asBinder().equals(listener.asBinder());
    }

    public boolean respondsTo(ILocationCallback callback) {
        return this.callback != null && callback != null &&
                this.callback.asBinder().equals(callback.asBinder());
    }

    public boolean respondsTo(PendingIntent pendingIntent) {
        return this.pendingIntent != null && this.pendingIntent.equals(pendingIntent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationRequestHelper that = (LocationRequestHelper) o;

        if (hasFinePermission != that.hasFinePermission) return false;
        if (hasCoarsePermission != that.hasCoarsePermission) return false;
        if (!locationRequest.equals(that.locationRequest)) return false;
        if (packageName != null ? !packageName.equals(that.packageName) : that.packageName != null) return false;
        if (listener != null ? !listener.equals(that.listener) : that.listener != null) return false;
        if (pendingIntent != null ? !pendingIntent.equals(that.pendingIntent) : that.pendingIntent != null)
            return false;
        return !(callback != null ? !callback.equals(that.callback) : that.callback != null);

    }

    @Override
    public int hashCode() {
        int result = locationRequest.hashCode();
        result = 31 * result + (hasFinePermission ? 1 : 0);
        result = 31 * result + (hasCoarsePermission ? 1 : 0);
        result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
        result = 31 * result + (listener != null ? listener.hashCode() : 0);
        result = 31 * result + (pendingIntent != null ? pendingIntent.hashCode() : 0);
        result = 31 * result + (callback != null ? callback.hashCode() : 0);
        return result;
    }
}
