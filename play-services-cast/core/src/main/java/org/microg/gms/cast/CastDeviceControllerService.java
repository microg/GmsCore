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

import com.google.android.gms.common.Feature;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class CastDeviceControllerService extends BaseService {
    private static final String TAG = CastDeviceControllerService.class.getSimpleName();

    private static final Feature[] FEATURES = new Feature[] {
            new Feature("cxless_connect", 1L),
            new Feature("cxless_set_listener", 1L)
    };

    public CastDeviceControllerService() {
        super("GmsCastDeviceControllerSvc", GmsService.CAST);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.features = FEATURES;

        callback.onPostInitCompleteWithConnectionInfo(
                CommonStatusCodes.SUCCESS,
                new CastDeviceControllerImpl(this, request.packageName, request.extras),
                connectionInfo
        );
    }
}