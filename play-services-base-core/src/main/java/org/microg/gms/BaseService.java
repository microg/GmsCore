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

package org.microg.gms;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.lifecycle.LifecycleService;

import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.IGmsServiceBroker;

import org.microg.gms.common.GmsService;

import java.util.Arrays;
import java.util.EnumSet;

public abstract class BaseService extends LifecycleService {
    private final IGmsServiceBroker broker;
    protected final String TAG;

    public BaseService(String tag, GmsService supportedService, GmsService... supportedServices) {
        this.TAG = tag;
        EnumSet<GmsService> services = EnumSet.of(supportedService);
        services.addAll(Arrays.asList(supportedServices));
        broker = new AbstractGmsServiceBroker(services) {
            @Override
            public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
                try {
                    request.extras.keySet(); // call to unparcel()
                } catch (Exception e) {
                    // Sometimes we need to define the correct ClassLoader before unparcel(). Ignore those.
                }
                Log.d(TAG, "bound by: " + request);
                BaseService.this.handleServiceRequest(callback, request, service);
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        Log.d(TAG, "onBind: " + intent);
        return broker.asBinder();
    }

    public abstract void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException;
}
