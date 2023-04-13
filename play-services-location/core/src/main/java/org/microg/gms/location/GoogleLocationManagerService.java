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

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.Feature;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class GoogleLocationManagerService extends BaseService {
    private final GoogleLocationManagerServiceImpl impl = new GoogleLocationManagerServiceImpl(this, getLifecycle());

    public GoogleLocationManagerService() {
        super("LocationManager", GmsService.LOCATION_MANAGER, GmsService.GEODATA, GmsService.PLACE_DETECTION);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        impl.invokeOnceReady(() -> {
            try {
                ConnectionInfo info = new ConnectionInfo();
                info.features = FEATURES;
                callback.onPostInitCompleteWithConnectionInfo(0, impl.asBinder(), info);
            } catch (RemoteException e) {
                Log.w(TAG, e);
            }
        });
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        impl.getLocationManager().dump(writer);
    }

    public static final Feature[] FEATURES = new Feature[] {
            new Feature("get_current_location", 1),
            new Feature("support_context_feature_id", 1),
            new Feature("name_ulr_private", 1),
            new Feature("driving_mode", 6),
            new Feature("name_sleep_segment_request", 1)
    };
}
