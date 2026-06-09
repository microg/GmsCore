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

package org.microg.gms.cast;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class CastDeviceControllerService extends BaseService {
    private static final String TAG = CastDeviceControllerService.class.getSimpleName();

    /**
     * Feature flag required by the Cast SDK client for establishing a connection.
     * Without this, the client rejects the service binder before connect() is called,
     * causing a silent failure with no Cast button shown.
     */
    private static final String FEATURE_CXLESS_CLIENT_MINIMAL = "cxless_client_minimal";

    public CastDeviceControllerService() {
        super("GmsCastDeviceControllerSvc", GmsService.CAST);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request,
            GmsService service) throws RemoteException {
        // Advertise required feature flags so the Cast SDK does not abort the connection.
        if (request.extras != null
                && !request.extras.containsKey(FEATURE_CXLESS_CLIENT_MINIMAL)) {
            request.extras.putBoolean(FEATURE_CXLESS_CLIENT_MINIMAL, true);
        }

        CastDeviceControllerImpl controller =
                new CastDeviceControllerImpl(this, request.packageName, request.extras);

        callback.onPostInitComplete(0, controller, null);
    }
}
