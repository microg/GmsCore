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

package org.microg.gms.wearable;

import android.content.Context;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.IWearableService;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.GoogleApiClientImpl;
import org.microg.gms.common.api.OnConnectionFailedListener;

public class WearableClientImpl extends GmsClient<IWearableService> {
    private static final String TAG = "GmsWearClient";

    public WearableClientImpl(Context context, Wearable.WearableOptions options, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.WEARABLE.ACTION);
        serviceId = GmsService.WEARABLE.SERVICE_ID;
        if (options != null && options.firstPartyMode)
            extras.putBoolean("firstPartyMode", true);
        Log.d(TAG, "<init>");
    }

    @Override
    protected IWearableService interfaceFromBinder(IBinder binder) {
        return IWearableService.Stub.asInterface(binder);
    }

    public static WearableClientImpl get(GoogleApiClient apiClient) {
        if (apiClient instanceof GoogleApiClientImpl) {
            return (WearableClientImpl) ((GoogleApiClientImpl) apiClient).getApiConnection(Wearable.API);
        }
        return null;
    }
}
