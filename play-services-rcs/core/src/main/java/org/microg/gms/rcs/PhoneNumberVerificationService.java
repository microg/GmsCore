package org.microg.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class PhoneNumberVerificationService extends Service {
    private static final String TAG = "GmsPhoneVerifySvc";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "PhoneNumberVerificationService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting phone number verification...");
        // Mock success response
        Intent successIntent = new Intent("com.google.android.gms.rcs.VERIFICATION_SUCCESS");
        successIntent.putExtra("verificationToken", "dummy_token_12345");
        sendBroadcast(successIntent);
        
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
