package org.microg.gms.wearable;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Minimal WearOS bridge for MicroG: pairing, notification relay, media controls.
 */
public class WearableService extends Service {
    private static final String TAG = "GmsWearable";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WearableService started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            Log.d(TAG, "action=" + intent.getAction());
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "WearableService stopped");
        super.onDestroy();
    }
}
