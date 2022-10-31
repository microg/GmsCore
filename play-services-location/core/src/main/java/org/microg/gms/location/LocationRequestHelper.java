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

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.location.ILocationCallback;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.internal.LocationRequestUpdateData;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.UUID;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class LocationRequestHelper {
    public static final String TAG = "GmsLocRequestHelper";
    private final Context context;
    public final LocationRequest locationRequest;
    public final boolean initialHasFinePermission;
    public final boolean initialHasCoarsePermission;
    public final String packageName;
    public final int uid;
    private final boolean selfHasAppOpsRights;
    public ILocationListener listener;
    public PendingIntent pendingIntent;
    public ILocationCallback callback;
    public String id = UUID.randomUUID().toString();

    private Location lastReport;
    private int numReports = 0;

    private LocationRequestHelper(Context context, LocationRequest locationRequest, String packageName, int uid) {
        this.context = context;
        this.locationRequest = locationRequest;
        this.packageName = packageName;
        this.uid = uid;

        this.initialHasFinePermission = context.getPackageManager().checkPermission(ACCESS_FINE_LOCATION, packageName) == PackageManager.PERMISSION_GRANTED;
        this.initialHasCoarsePermission = context.getPackageManager().checkPermission(ACCESS_COARSE_LOCATION, packageName) == PackageManager.PERMISSION_GRANTED;

        this.selfHasAppOpsRights = context.getPackageManager().checkPermission("android.permission.UPDATE_APP_OPS_STATS", context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }

    public LocationRequestHelper(Context context, LocationRequest locationRequest, String packageName, int uid, ILocationListener listener) {
        this(context, locationRequest, packageName, uid);
        this.listener = listener;
    }

    public LocationRequestHelper(Context context, LocationRequest locationRequest, String packageName, int uid, PendingIntent pendingIntent) {
        this(context, locationRequest, packageName, uid);
        this.pendingIntent = pendingIntent;
    }

    public LocationRequestHelper(Context context, String packageName, int uid, LocationRequestUpdateData data) {
        this(context, data.request.request, packageName, uid);
        this.listener = data.listener;
        this.pendingIntent = data.pendingIntent;
        this.callback = data.callback;
    }

    public boolean isActive() {
        if (!hasCoarsePermission()) return false;
        if (locationRequest.getExpirationTime() < SystemClock.elapsedRealtime()) return false;
        if (listener != null) {
            try {
                return listener.asBinder().isBinderAlive();
            } catch (Exception e) {
                return false;
            }
        } else if (pendingIntent != null) {
            return true;
        } else if (callback != null) {
            try {
                return callback.asBinder().isBinderAlive();
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean locationIsValid(Location location) {
        if (location == null) return false;
        if (Double.isNaN(location.getLatitude()) || location.getLatitude() > 90 || location.getLatitude() < -90) return false;
        if (Double.isNaN(location.getLongitude()) || location.getLongitude() > 180 || location.getLongitude() < -180) return false;
        return true;
    }

    /**
     * @return whether to continue sending reports to this {@link LocationRequestHelper}
     */
    public boolean report(Location location) {
        if (!isActive()) return false;
        if (!locationIsValid(location)) return true;
        if (lastReport != null) {
            if (location.equals(lastReport)) {
                return true;
            }
            if (location.getTime() - lastReport.getTime() < locationRequest.getFastestInterval()) {
                return true;
            }
            if (location.distanceTo(lastReport) < locationRequest.getSmallestDisplacement()) {
                return true;
            }
        }
        lastReport = new Location(location);
        lastReport.setProvider("fused");
        Log.d(TAG, "sending Location: " + location + " to " + packageName);
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
                ", hasFinePermission=" + hasFinePermission() +
                ", hasCoarsePermission=" + hasCoarsePermission() +
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

        if (!locationRequest.equals(that.locationRequest)) return false;
        if (packageName != null ? !packageName.equals(that.packageName) : that.packageName != null) return false;
        if (listener != null ? !listener.equals(that.listener) : that.listener != null) return false;
        if (pendingIntent != null ? !pendingIntent.equals(that.pendingIntent) : that.pendingIntent != null)
            return false;
        return !(callback != null ? !callback.equals(that.callback) : that.callback != null);
    }

    public boolean hasFinePermission() {
        if (Build.VERSION.SDK_INT >= 19) {
            return isAppOpsAllowed(AppOpsManager.OPSTR_FINE_LOCATION, initialHasFinePermission);
        } else {
            return initialHasFinePermission;
        }
    }

    public boolean hasCoarsePermission() {
        if (Build.VERSION.SDK_INT >= 19) {
            return isAppOpsAllowed(AppOpsManager.OPSTR_COARSE_LOCATION, initialHasCoarsePermission);
        } else {
            return initialHasCoarsePermission;
        }
    }

    @TargetApi(19)
    private boolean isAppOpsAllowed(String op, boolean def) {
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOpsManager == null) return def;
        try {
            if (Binder.getCallingUid() == uid && Build.VERSION.SDK_INT >= 23) {
                return appOpsManager.noteProxyOpNoThrow(op, packageName) == AppOpsManager.MODE_ALLOWED;
            } else if (Build.VERSION.SDK_INT >= 29) {
                return appOpsManager.noteProxyOpNoThrow(op, packageName, uid) == AppOpsManager.MODE_ALLOWED;
            } else if (selfHasAppOpsRights) {
                return appOpsManager.noteOpNoThrow(op, uid, packageName) == AppOpsManager.MODE_ALLOWED;
            } else {
                // TODO: More variant that works pre-29 and without perms?
                Log.w(TAG, "Can't check appops (yet)");
                return def;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
            return def;
        }
    }

    @Override
    public int hashCode() {
        int result = locationRequest.hashCode();
        result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
        result = 31 * result + (listener != null ? listener.hashCode() : 0);
        result = 31 * result + (pendingIntent != null ? pendingIntent.hashCode() : 0);
        result = 31 * result + (callback != null ? callback.hashCode() : 0);
        return result;
    }

    public void dump(PrintWriter writer) {
        writer.println("  " + id + " package=" + packageName);
        writer.println("    request: " + locationRequest);
        writer.println("    last location: " + lastReport);
    }
}
