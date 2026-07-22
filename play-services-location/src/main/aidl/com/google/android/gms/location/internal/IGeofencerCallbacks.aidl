package com.google.android.gms.location.internal;

import android.app.PendingIntent;

interface IGeofencerCallbacks {
    oneway void onAddGeofenceResult(int statusCode, in String[] requestIds) = 0;
    oneway void onRemoveGeofencesByRequestIdsResult(int statusCode, in String[] requestIds) = 1;
    oneway void onRemoveGeofencesByPendingIntentResult(int statusCode, in PendingIntent pendingIntent) = 2;
}
