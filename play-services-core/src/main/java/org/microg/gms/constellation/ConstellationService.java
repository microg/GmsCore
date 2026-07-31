package org.microg.gms.constellation;

import android.os.RemoteException;

import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class ConstellationService extends BaseService {
    public ConstellationService() {
        super("GmsConstSvc", GmsService.CONSTELLATION);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        callback.onPostInitComplete(0, new ConstellationServiceImpl(this).asBinder(), null);
    }
}
