package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.rcs.internal.RcsProvisioningService;

public class RcsService extends Service {
    private static final String TAG = "RcsService";

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "RcsService bound: " + intent);
        // Return appropriate binder based on intent action
        if (RcsProvisioningService.SERVICE_INTERFACE.equals(intent.getAction())) {
            return new RcsProvisioningService().getBinder();
        }
        return null;
    }
}