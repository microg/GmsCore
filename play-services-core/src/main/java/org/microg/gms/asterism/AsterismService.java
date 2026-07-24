package org.microg.gms.asterism;

import android.os.Binder;
import android.os.RemoteException;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class AsterismService extends BaseService {
    public AsterismService() {
        super("GmsAsterism", GmsService.ASTERISM);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        callback.onPostInitComplete(0, new Binder(), null);
    }
}