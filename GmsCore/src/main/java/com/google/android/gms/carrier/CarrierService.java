package com.google.android.gms.carrier;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class CarrierService extends Service {
    private static final String TAG = "CarrierService";

    @Override
    public IBinder onBind(Intent intent) {
        // Return a stub implementation of ICarrierService
        return new ICarrierService.Stub() {
            @Override
            public boolean provisionCarrier(String carrierId, ICarrierProvisioningCallback callback) throws RemoteException {
                Log.i(TAG, "Provisioning carrier: " + carrierId);
                callback.onProvisioningStatus(0);
                return true;
            }

            @Override
            public boolean isCarrierProvisioned() throws RemoteException {
                return true;
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "CarrierService created");
    }
}