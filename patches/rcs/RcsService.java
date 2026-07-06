package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.IGmsServiceBroker;
import com.google.android.gms.common.internal.IGmsServiceCallbacks;

/**
 * Stub service for RCS provisioning required by Google Messages.
 * This service intercepts RCS provisioning requests and returns
 * success with default carrier configuration, enabling RCS setup
 * without device attestation.
 */
public class RcsService extends Service {

    private static final String TAG = "microg.RcsService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "RcsService bound with intent: " + intent);
        // Return a simple binder that responds to GmsServiceBroker queries
        return new IGmsServiceBroker.Stub() {
            @Override
            public void getService(IGmsServiceCallbacks callbacks, String serviceId) throws RemoteException {
                Log.d(TAG, "getService called for: " + serviceId);
                // Respond with success for RCS provisioning service
                if ("com.google.android.gms.rcs.RcsProvisioningService".equals(serviceId)) {
                    callbacks.onServiceConnected(0, new RcsProvisioningBinder());
                } else {
                    callbacks.onServiceConnected(1, null);
                }
            }
        };
    }

    private static class RcsProvisioningBinder extends IRcsProvisioningService.Stub {
        @Override
        public void requestRcsProvisioning(String msisdn, String imsi, String imei, String simSerial) throws RemoteException {
            Log.d(TAG, "Provisioning request: msisdn=" + msisdn + ", imsi=" + imsi);
            // Always return success without real provisioning
        }

        @Override
        public String getRcsConfig() throws RemoteException {
            Log.d(TAG, "Returning minimal RCS config");
            return "{\"rcsEnabled\":true,\"ims\":{\"method\":\"sip\",\"uri\":\"sip:user@ims.mnc000.mcc000.pub.3gppnetwork.org\"},\"sms\":{\"useRcsForSms\":true}}";
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RcsService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "RcsService destroyed");
    }
}