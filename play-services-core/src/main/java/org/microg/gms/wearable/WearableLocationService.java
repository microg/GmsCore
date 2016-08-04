/*
 * Copyright 2013-2016 microG Project Team
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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.android.gms.location.internal.LocationRequestInternal;

import java.util.Collection;

public class WearableLocationService extends Service {
    // TODO: Implement and use WearableListenerService
    private static final String TAG = "GmsWearLocSvc";

    private WearableLocationListener listener;

    @Override
    public void onCreate() {
        listener = new WearableLocationListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals("com.google.android.gms.wearable.BIND_LISTENER")) {
            return listener.asBinder();
        }
        return null;
    }

    public void onLocationRequests(String nodeId, Collection<LocationRequestInternal> requests, boolean triggerUpdate) {

    }

    public void onCapabilityQuery(String nodeId) {

    }
}
