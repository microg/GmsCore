package com.google.android.gms.wearable;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Background service to maintain the connection between phone and watch.
 */
public class WearableService extends Service {
    private static final String TAG = "WearableService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "WearableService started for WearOS synchronization");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
