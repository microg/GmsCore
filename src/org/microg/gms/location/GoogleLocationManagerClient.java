package org.microg.gms.location;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.internal.IGmsServiceBroker;
import com.google.android.gms.location.internal.IGoogleLocationManagerService;

import org.microg.gms.Constants;
import org.microg.gms.common.GmsClient;

public abstract class GoogleLocationManagerClient extends GmsClient<IGoogleLocationManagerService> {
    public GoogleLocationManagerClient(Context context, GoogleApiClient.ConnectionCallbacks
            callbacks, GoogleApiClient.OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener);
    }

    @Override
    protected String getActionString() {
        return Constants.ACTION_GMS_LOCATION_MANAGER_SERVICE_START;
    }

    @Override
    protected IGoogleLocationManagerService interfaceFromBinder(IBinder binder) {
        return IGoogleLocationManagerService.Stub.asInterface(binder);
    }

    @Override
    protected void onConnectedToBroker(IGmsServiceBroker broker, GmsCallbacks callbacks)
            throws RemoteException {
        Bundle bundle = new Bundle();
        bundle.putString("client_name", "locationServices");
        broker.getGoogleLocationManagerService(callbacks, Constants.MAX_REFERENCE_VERSION,
                getContext().getPackageName(), bundle);
    }
}
