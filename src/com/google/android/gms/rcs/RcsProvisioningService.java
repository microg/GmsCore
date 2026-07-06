package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.rcs.internal.IRcsProvisioningService;
import com.google.android.gms.rcs.internal.IRcsProvisioningCallback;

public class RcsProvisioningService extends Service {
    private static final String TAG = "RcsProvisioningService";

    private IRcsProvisioningService.Stub binder = new IRcsProvisioningService.Stub() {
        @Override
        public void startProvisioning(String phoneNumber, IRcsProvisioningCallback callback) throws RemoteException {
            Log.d(TAG, "startProvisioning for: " + phoneNumber);
            // In a real implementation, send an SMS to verify the number.
            // For now, assume verification succeeds after a short delay.
            new android.os.Handler().postDelayed(() -> {
                try {
                    callback.onProvisioningSuccess(phoneNumber);
                } catch (RemoteException e) {
                    Log.e(TAG, "Callback error", e);
                }
            }, 2000);
        }

        @Override
        public void cancelProvisioning() throws RemoteException {
            Log.d(TAG, "cancelProvisioning");
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
