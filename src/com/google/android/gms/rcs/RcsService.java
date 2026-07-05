package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class RcsService extends Service {
    private static final String TAG = "RcsService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "RCS service created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new IRcsService.Stub() {
            @Override
            public boolean isRcsAvailable() {
                return true;
            }

            @Override
            public void setRcsEnabled(boolean enabled) {
                // Stub - actual implementation would persist setting
            }

            @Override
            public void startRegistration() {
                // Stub - would trigger SIM association
            }

            @Override
            public void stopRegistration() {
                // Stub
            }

            @Override
            public void getRegistrationStatus(IRcsStatusCallback callback) {
                // Always report registered for now
                try {
                    callback.onStatus(RegistrationStatus.REGISTERED);
                } catch (Exception e) {
                    Log.e(TAG, "Callback failed", e);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "RCS service destroyed");
    }
}
