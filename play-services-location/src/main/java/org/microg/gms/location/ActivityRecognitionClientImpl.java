/*
 * Copyright (C) 2017 microG Project Team
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

import android.app.PendingIntent;
import android.content.Context;
import android.os.RemoteException;

import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public class ActivityRecognitionClientImpl extends GoogleLocationManagerClient {
    public ActivityRecognitionClientImpl(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener);
    }

    public void requestActivityUpdates(long detectionIntervalMillis, PendingIntent callbackIntent) throws RemoteException {
        getServiceInterface().requestActivityUpdates(detectionIntervalMillis, true, callbackIntent);
    }

    public void removeActivityUpdates(PendingIntent callbackIntent) throws RemoteException {
        getServiceInterface().removeActivityUpdates(callbackIntent);
    }
}
