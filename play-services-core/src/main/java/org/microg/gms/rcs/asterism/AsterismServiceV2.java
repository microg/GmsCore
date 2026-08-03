package org.microg.gms.rcs.asterism;

import android.os.RemoteException;
import com.google.android.gms.rcs.asterism.IAsterismService;
import com.google.android.gms.rcs.asterism.IAsterismCallback;

public class AsterismServiceV2 extends IAsterismService.Stub {
    @Override
    public void registerAsterismCallback(IAsterismCallback callback) throws RemoteException {
        // Registration logic for Google Messages RCS asterism stream
    }

    @Override
    public void unregisterAsterismCallback(IAsterismCallback callback) throws RemoteException {
        // Unregistration logic
    }
}
