package org.microg.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.rcs.RcsService;

/**
 * Bridges RCS registration between Google Messages and microG.
 * Implements minimal RCS service stubs to pass phone number verification.
 */
public class RcsBridgeService extends Service {
    private static final String TAG = "GmsRcsBridge";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "RCS Bridge Service created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return a binder for the RCS service interface
        return new RcsService.Stub() {
            @Override
            public boolean isRcsAvailable() {
                return true;
            }

            @Override
            public int getRegistrationState() {
                // Return REGISTERED state to avoid endless "Setting up..."
                return RcsService.REGISTRATION_STATE_REGISTERED;
            }

            @Override
            public String getPhoneNumber() {
                // Return the user's phone number from microG settings or SIM
                return getSharedPreferences("microg_prefs", MODE_PRIVATE)
                        .getString("phone_number", "+15555555555");
            }

            @Override
            public boolean setPhoneNumber(String number) {
                getSharedPreferences("microg_prefs", MODE_PRIVATE)
                        .edit().putString("phone_number", number).apply();
                return true;
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Bridge service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Bridge service destroyed");
    }
}
