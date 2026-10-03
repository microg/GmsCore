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

import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CastDeviceControllerService extends BaseService {
    private static final String TAG = CastDeviceControllerService.class.getSimpleName();

    // Controllers that are not disconnected yet. Every connected client keeps this service bound, so the ones left
    // when it is destroyed were abandoned by their client.
    private final Set<CastDeviceControllerImpl> controllers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public CastDeviceControllerService() {
        super("GmsCastDeviceControllerSvc", GmsService.CAST, GmsService.CAST_API);
    }

    @Override
    public void onDestroy() {
        for (CastDeviceControllerImpl controller : controllers) {
            controller.disconnect();
        }
        controllers.clear();
        super.onDestroy();
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        if (service == GmsService.CAST_API) {
            ConnectionInfo info = new ConnectionInfo();
            info.features = CastServiceImplKt.CAST_SERVICE_FEATURES;
            callback.onPostInitCompleteWithConnectionInfo(0, new CastServiceImpl(request.packageName), info);
            return;
        }
        ConnectionInfo info = new ConnectionInfo();
        info.features = CastServiceImplKt.CAST_DEVICE_CONTROLLER_FEATURES;
        CastDeviceControllerImpl controller = new CastDeviceControllerImpl(request.packageName, request.extras, released -> {
            controllers.remove(released);
            return kotlin.Unit.INSTANCE;
        }, reopened -> {
            controllers.add(reopened);
            return kotlin.Unit.INSTANCE;
        });
        controllers.add(controller);
        if (!controller.getHasInitialListener()) {
            // The client calls connect() once it added its listener
            callback.onPostInitCompleteWithConnectionInfo(0, controller, info);
            return;
        }
        controller.connectBeforeInit(statusCode -> {
            // The client treats APP_NO_LONGER_RUNNING as connected
            boolean connected = statusCode == 0 || statusCode == CastDeviceControllerImpl.STATUS_APP_NO_LONGER_RUNNING;
            try {
                callback.onPostInitCompleteWithConnectionInfo(statusCode, connected ? controller : null, info);
            } catch (RemoteException e) {
                Log.w(TAG, "Client died while connecting", e);
                controller.disconnect();
                return kotlin.Unit.INSTANCE;
            }
            // Without the binder the client can never call disconnect()
            if (!connected) controller.disconnect();
            return kotlin.Unit.INSTANCE;
        });
    }
}
