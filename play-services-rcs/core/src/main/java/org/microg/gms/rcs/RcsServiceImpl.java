package org.microg.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.rcs.internal.IRcsService;

public class RcsServiceImpl extends Service {
    private static final String TAG = "GmsRcsSvcImpl";
    private boolean isEnabled = true;

    private final IRcsService.Stub binder = new IRcsService.Stub() {
        @Override
        public boolean isRcsEnabled() throws RemoteException {
            Log.d(TAG, "isRcsEnabled: " + isEnabled);
            return isEnabled;
        }

        @Override
        public void setRcsEnabled(boolean enabled) throws RemoteException {
            Log.d(TAG, "setRcsEnabled: " + enabled);
            isEnabled = enabled;
        }

        @Override
        public int getRcsState() throws RemoteException {
            Log.d(TAG, "getRcsState called. Returning CONNECTED.");
            return 1; // STATE_CONNECTED
        }

        @Override
        public String getProvisioningUrl() throws RemoteException {
            Log.d(TAG, "getProvisioningUrl called");
            return "https://rcs.telephony.goog/provisioning";
        }

        @Override
        public void notifyProvisioningSuccess(String token) throws RemoteException {
            Log.d(TAG, "Provisioning succeeded with token: " + token);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RcsServiceImpl started");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "RcsServiceImpl bound via intent: " + intent);
        return binder;
    }
}
