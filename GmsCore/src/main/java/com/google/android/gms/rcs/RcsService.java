package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class RcsService extends Service {
    private static final String TAG = "RcsService";

    @Override
    public IBinder onBind(Intent intent) {
        // Return a stub implementation of IRcsService
        return new IRcsService.Stub() {
            @Override
            public boolean provisionPhoneNumber(String phoneNumber, IProvisioningCallback callback) throws RemoteException {
                Log.i(TAG, "Provisioning phone number: " + phoneNumber);
                // Simulate successful provisioning
                callback.onProvisioningStatus(0); // 0 = success
                return true;
            }

            @Override
            public boolean isRcsEnabled() throws RemoteException {
                return true;
            }

            @Override
            public boolean setRcsEnabled(boolean enabled) throws RemoteException {
                return true;
            }

            @Override
            public boolean provisionDevice(String carrierId, IProvisioningCallback callback) throws RemoteException {
                Log.i(TAG, "Provisioning device with carrier: " + carrierId);
                callback.onProvisioningStatus(0);
                return true;
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "RcsService created");
    }
}