package org.microg.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class RcsProvisioningService extends Service {
    private final IRcsProvisioning.Stub binder = new IRcsProvisioning.Stub() {
        @Override
        public void provision(String imsi, String msisdn) throws RemoteException {
            // Accept provisioning request
            // In production, this would contact carrier's RCS provisioning server
            // For microG, we assume success
        }

        @Override
        public int getProvisioningStatus() throws RemoteException {
            return 0; // PROVISIONING_STATUS_SUCCESS
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }
}
