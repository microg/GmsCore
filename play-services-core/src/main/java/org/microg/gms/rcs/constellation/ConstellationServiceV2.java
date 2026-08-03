package org.microg.gms.rcs.constellation;

import android.os.RemoteException;
import com.google.android.gms.rcs.constellation.IConstellationService;
import com.google.android.gms.rcs.constellation.IConstellationCallback;

public class ConstellationServiceV2 extends IConstellationService.Stub {
    @Override
    public void getConstellationStatus(IConstellationCallback callback) throws RemoteException {
        if (callback != null) {
            callback.onResult(0, null);
        }
    }
}
