package com.google.android.gms.location.internal;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.internal.IGeofencerCallbacks;

interface IGoogleLocationManagerService {
    void addGeofences(in List<com.google.android.gms.location.Geofence> geofences, in PendingIntent pendingIntent, IGeofencerCallbacks callback, String str);
}
