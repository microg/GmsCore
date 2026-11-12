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

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.Feature;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.GmsService;

public class CastDeviceControllerService extends BaseService {
    private static final String TAG = CastDeviceControllerService.class.getSimpleName();

    /**
     * Features we announce to the client library. The GoogleApi based Cast client (play-services-cast 19+) refuses to
     * connect to a device unless "cxless_client_minimal" is available and only asks for feature flags if
     * "module_flag_control" is. Only add features here once the calls guarded by them are implemented.
     */
    private static final Feature[] FEATURES = new Feature[]{
            new Feature("cxless_client_minimal", 1),
            new Feature("module_flag_control", 1),
    };

    public CastDeviceControllerService() {
        super("GmsCastDeviceControllerSvc", GmsService.CAST, GmsService.CAST_API);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        IBinder binder;
        if (service == GmsService.CAST_API) {
            // Connection-less API, used by CastContext to query feature flags
            Log.d(TAG, "Cast API requested by " + request.packageName);
            binder = new CastServiceImpl();
        } else {
            try {
                String packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName);
                binder = new CastDeviceControllerImpl(this, packageName, request.extras);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Refusing service request", e);
                callback.onPostInitComplete(CommonStatusCodes.INTERNAL_ERROR, null, null);
                return;
            }
        }
        if (request.supportsConnectionInfo) {
            ConnectionInfo connectionInfo = new ConnectionInfo();
            connectionInfo.features = FEATURES;
            callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, binder, connectionInfo);
        } else {
            // Old client libraries don't know about ConnectionInfo and would never see the connection complete
            callback.onPostInitComplete(CommonStatusCodes.SUCCESS, binder, null);
        }
    }
}
