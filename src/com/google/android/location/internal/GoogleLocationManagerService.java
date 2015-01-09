/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.location.internal;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.internal.IGeofencerCallbacks;
import com.google.android.gms.location.internal.IGoogleLocationManagerService;
import com.google.android.gms.common.AbstractGmsServiceBroker;
import com.google.android.gms.common.internal.IGmsCallbacks;

import java.util.List;

public class GoogleLocationManagerService extends Service {
    private static final String TAG = GoogleLocationManagerService.class.getName();

    @Override
    public IBinder onBind(Intent intent) {
        return new Broker(intent).asBinder();
    }

    private class Broker extends AbstractGmsServiceBroker {
        public Broker(Intent intent) {
            Log.d(TAG, "Incoming intent: " + intent.toString());
        }

        @Override
        public void getGoogleLocationManagerService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
            params.keySet();
            Log.d(TAG, "getGoogleLocationManagerService: " + versionCode + ", " + packageName + ", " + params);
            callback.onPostInitComplete(0, new IGoogleLocationManagerService.Stub() {

                @Override
                public void addGeofences(List<Geofence> geofences, PendingIntent pendingIntent, IGeofencerCallbacks callback, String str) throws RemoteException {

                }
            }, params);
        }
    }
}
