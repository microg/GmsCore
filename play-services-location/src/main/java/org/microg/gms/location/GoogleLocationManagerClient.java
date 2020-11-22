/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.location;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.common.internal.IGmsServiceBroker;
import com.google.android.gms.location.internal.IGoogleLocationManagerService;

import org.microg.gms.common.Constants;
import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public abstract class GoogleLocationManagerClient extends GmsClient<IGoogleLocationManagerService> {
    public GoogleLocationManagerClient(Context context, ConnectionCallbacks
            callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.LOCATION_MANAGER.ACTION);
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
