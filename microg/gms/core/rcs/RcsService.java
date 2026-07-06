package org.microg.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.rcs.IRcsService;
import com.google.android.gms.rcs.RcsProvisioningResult;

/**
 * Service that provides RCS capabilities to Google Messages.
 * This is a stub that returns success for provisioning requests.
 */
public class RcsService extends Service {

    private static final String TAG = "MicroG-RcsService";

    private final IRcsService.Stub binder = new IRcsService.Stub() {
        @Override
        public RcsProvisioningResult provisionRcs(String msisdn, String imsi, String simSlot) throws RemoteException {
            Log.d(TAG, "provisionRcs called with msisdn=" + msisdn + ", imsi=" + imsi + ", simSlot=" + simSlot);
            // Return a successful provisioning result
            RcsProvisioningResult result = new RcsProvisioningResult();
            result.setStatus(RcsProvisioningResult.STATUS_SUCCESS);
            result.setProvisioningToken("microg_provisioning_token");
            result.setRcsUrl("https://rcs.example.com");
            return result;
        }

        @Override
        public boolean isRcsEnabled() throws RemoteException {
            return true;
        }

        @Override
        public void setRcsEnabled(boolean enabled) throws RemoteException {
            Log.d(TAG, "setRcsEnabled: " + enabled);
        }

        @Override
        public String getRcsClientVersion() throws RemoteException {
            return "microg-rcs-1.0";
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        if ("com.google.android.gms.rcs.SERVICE".equals(intent.getAction())) {
            return binder;
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
