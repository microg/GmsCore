package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class RcsProvisioningService extends Service {
    private static final String TAG = "RcsProvisioningSvc";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RCS provisioning service created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return a stub that mimics Google's provisioning service
        return new IRcsProvisioningService.Stub() {
            @Override
            public boolean isRcsEnabled() throws RemoteException {
                // Always return true to indicate RCS is supported
                return true;
            }

            @Override
            public void startProvisioning(String msisdn, int subId) throws RemoteException {
                Log.d(TAG, "Provisioning started for MSISDN: " + msisdn + ", subId: " + subId);
                // Simulate success by not throwing
            }

            @Override
            public void finishProvisioning() throws RemoteException {
                Log.d(TAG, "Provisioning finished");
            }
        };
    }
}
