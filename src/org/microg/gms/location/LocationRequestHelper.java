package org.microg.gms.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationRequestHelper {
    public static final String TAG = "GmsLocationRequestHelper";
    private final Context context;
    public final LocationRequest locationRequest;
    public final boolean hasFinePermission;
    public final boolean hasCoarsePermission;
    public final String packageName;
    private ILocationListener listener;
    private PendingIntent pendingIntent;

    private Location lastReport;
    private int numReports = 0;

    public LocationRequestHelper(Context context, LocationRequest locationRequest,
            boolean hasFinePermission, boolean hasCoarsePermission, String packageName,
            ILocationListener listener) {
        this.context = context;
        this.locationRequest = locationRequest;
        this.hasFinePermission = hasFinePermission;
        this.hasCoarsePermission = hasCoarsePermission;
        this.packageName = packageName;
        this.listener = listener;
    }

    public LocationRequestHelper(Context context, LocationRequest locationRequest,
            boolean hasFinePermission, boolean hasCoarsePermission, String packageName,
            PendingIntent pendingIntent) {
        this.context = context;
        this.locationRequest = locationRequest;
        this.hasFinePermission = hasFinePermission;
        this.hasCoarsePermission = hasCoarsePermission;
        this.packageName = packageName;
        this.pendingIntent = pendingIntent;
    }

    /**
     * @return whether to continue sending reports to this {@link LocationRequestHelper}
     */
    public boolean report(Location location) {
        if (lastReport != null) {
            if (location.getTime() - lastReport.getTime() < locationRequest.getFastestInterval()) {
                return true;
            }
            if (location.distanceTo(lastReport) < locationRequest.getSmallestDesplacement()) {
                return true;
            }
        }
        Log.d(TAG, "sending Location: " + location);
        if (listener != null) {
            try {
                listener.onLocationChanged(location);
            } catch (RemoteException e) {
                return false;
            }
        } else if (pendingIntent != null) {
            Intent intent = new Intent();
            intent.putExtra("com.google.android.location.LOCATION", location);
            try {
                pendingIntent.send(context, 0, intent);
            } catch (PendingIntent.CanceledException e) {
                return false;
            }
        }
        lastReport = location;
        numReports++;
        if (numReports >= locationRequest.getNumUpdates()) {
            return false;
        }
        return true;
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

    public boolean respondsTo(PendingIntent pendingIntent) {
        return this.pendingIntent != null && this.pendingIntent.equals(pendingIntent);
    }
}
