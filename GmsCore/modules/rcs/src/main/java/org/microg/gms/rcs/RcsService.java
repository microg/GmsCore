package org.microg.gms.rcs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.common.GmsService;

public class RcsService extends Service {
    private final IRcsService.Stub binder = new IRcsService.Stub() {
        @Override
        public boolean isRcsAvailable() throws RemoteException {
            return true;
        }

        @Override
        public void startProvisioning() throws RemoteException {
            // Trigger provisioning flow
            Intent intent = new Intent(RcsService.this, RcsProvisioningService.class);
            startService(intent);
        }

        @Override
        public void verifyPhoneNumber(String phoneNumber) throws RemoteException {
            // Simulate successful verification
            // In practice, this would interact with carrier RCS backend
            // For microG, we assume the phone number is valid
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    // Bind to GmsService
    @Override
    public void onCreate() {
        super.onCreate();
        GmsService.register("rcs", this);
    }
}
