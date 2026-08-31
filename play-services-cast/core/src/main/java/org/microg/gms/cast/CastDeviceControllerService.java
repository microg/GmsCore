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
import android.os.Parcel;
import android.util.ArrayMap;
import android.util.Log;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.internal.ICastDeviceControllerListener;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.BinderWrapper;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.Status;
import su.litvak.chromecast.api.v2.ChromeCastsListener;

public class CastDeviceControllerService extends BaseService {
    private static final String TAG = CastDeviceControllerService.class.getSimpleName();

    public CastDeviceControllerService() {
        // Modern connectionless senders (Netflix/Prime, Cast.API_CXLESS) bind this same service
        // but request serviceId CAST_API (161). Accept it too, or the broker rejects the bind with
        // "Service not supported" — which the sender surfaces as "No devices found". The request is
        // then served by the same CastDeviceControllerImpl, whose cxless connect/setListener path
        // handles it.
        super("GmsCastDeviceControllerSvc", GmsService.CAST, GmsService.CAST_API);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        // Advertise the API features the client requested so its availability check passes
        // (otherwise the SDK deems microG too old -> ConnectionResult=2). Echo all of them,
        // including the connectionless (cxless) features: CastDeviceControllerImpl now implements
        // the connectionless connect/setListener handshake, so the cxless path works.
        ConnectionInfo info = new ConnectionInfo();
        if (request.apiFeatures != null && request.apiFeatures.length > 0) {
            info.features = request.apiFeatures;
        } else {
            info.features = request.defaultFeatures;
        }
        callback.onPostInitCompleteWithConnectionInfo(
                0, new CastDeviceControllerImpl(this, request.packageName, request.extras), info);
    }
}
