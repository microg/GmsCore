package org.microg.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.rcs.IRcsService;

public class RcsServiceImpl extends Service {
    private static final String TAG = "RcsService";
    private boolean rcsEnabled = true;
    private String provisioningToken = "microg-rcs-token";

    private final IRcsService.Stub binder = new IRcsService.Stub() {
        @Override
        public boolean isRcsEnabled() throws RemoteException {
            Log.d(TAG, "isRcsEnabled called, returning true");
            return rcsEnabled;
        }

        @Override
        public void setRcsEnabled(boolean enabled) throws RemoteException {
            Log.d(TAG, "setRcsEnabled: " + enabled);
            rcsEnabled = enabled;
        }

        @Override
        public String getProvisioningToken() throws RemoteException {
            Log.d(TAG, "getProvisioningToken called");
            return provisioningToken;
        }

        @Override
        public void requestProvisioning() throws RemoteException {
            Log.d(TAG, "requestProvisioning called");
            // Simulate successful provisioning
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: " + intent.getAction());
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }
}
