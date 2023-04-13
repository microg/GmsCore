package com.google.android.gms.location.internal;

import android.app.PendingIntent;

interface IGeofencerCallbacks {
    void onAddGeofenceResult(int statusCode, in String[] requestIds) = 0;
    void onRemoveGeofencesByRequestIdsResult(int statusCode, in String[] requestIds) = 1;
    void onRemoveGeofencesByPendingIntentResult(int statusCode, in PendingIntent pendingIntent) = 2;
}
