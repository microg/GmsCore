package org.microg.wearos;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class WearOsService extends Service {
    private static final String TAG = "WearOsService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WearOS Service created");
        WearOsSupport.initialize(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}