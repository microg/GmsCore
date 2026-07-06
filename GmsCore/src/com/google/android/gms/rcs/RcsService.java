package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class RcsService extends Service {
    private static final String TAG = "RcsService";
    private RcsBinder binder;

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new RcsBinder(this);
        Log.d(TAG, "RCS Service created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "RCS Service started");
        return START_STICKY;
    }
}