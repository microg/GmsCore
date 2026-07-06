package com.google.android.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class RcsService extends Service {
    private static final String TAG = "RcsService";
    private RcsProvisioningManager provisioningManager;
    private IRcsService.Stub binder;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RcsService created");
        provisioningManager = new RcsProvisioningManager(this);

        binder = new IRcsService.Stub() {
            @Override
            public int getProvisioningState() throws RemoteException {
                return provisioningManager.getProvisioningState();
            }

            @Override
            public boolean isRcsCapable() throws RemoteException {
                return provisioningManager.isRcsCapable();
            }

            @Override
            public void startProvisioning(String phoneNumber) throws RemoteException {
                provisioningManager.startProvisioning(phoneNumber);
            }

            @Override
            public Map getRcsConfig() throws RemoteException {
                return provisioningManager.getRcsConfig();
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (RcsConstants.ACTION_RCS_SERVICE.equals(intent.getAction())) {
            return binder;
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
