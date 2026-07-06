package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.rcs.IProvisioningCallback;
import com.google.android.gms.common.rcs.IProvisioningService;

/**
 * Service that handles RCS provisioning requests from Google Messages.
 * Provides a simple stub that accepts provisioning without device attestation.
 */
public class RcsProvisioningService extends Service {

    private static final String TAG = "RcsProvisioningService";

    private final IProvisioningService.Stub binder = new IProvisioningService.Stub() {
        @Override
        public void startProvisioning(String userAgent, String carrierId, IProvisioningCallback callback) throws RemoteException {
            Log.i(TAG, "Provisioning request received: userAgent=" + userAgent + ", carrierId=" + carrierId);
            // Accept the provisioning immediately with success status
            // In a real implementation, this would contact the carrier's RCS provisioning server
            // For now, we simulate a successful provisioning with minimal data
            android.os.Bundle result = new android.os.Bundle();
            result.putString("rcs_config", "{\"rcsEnabled\":true,\"ims\":{\"enabled\":true}}");
            result.putBoolean("success", true);
            callback.onProvisioningComplete(result);
        }

        @Override
        public void cancelProvisioning() throws RemoteException {
            Log.i(TAG, "Provisioning cancelled");
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
    }
}