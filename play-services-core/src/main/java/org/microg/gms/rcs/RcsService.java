package org.microg.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class RcsService extends Service {
    private static final String TAG = "GmsRcsService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RCS Subsystem Initialized");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Google Messages binding to RCS layer");
        return null; 
    }
}