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
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;

import org.microg.gms.common.GmsConnector;

public class ActivityRecognitionApiImpl implements ActivityRecognitionApi {
    private static final String TAG = "GmsActivityApiImpl";

    @Override
    public PendingResult<Status> removeActivityUpdates(GoogleApiClient client, final PendingIntent callbackIntent) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(ActivityRecognitionClientImpl client) throws RemoteException {
                client.removeActivityUpdates(callbackIntent);
            }
        });
    }

    @Override
    public PendingResult<Status> requestActivityUpdates(GoogleApiClient client, final long detectionIntervalMillis, final PendingIntent callbackIntent) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(ActivityRecognitionClientImpl client) throws RemoteException {
                client.requestActivityUpdates(detectionIntervalMillis, callbackIntent);
            }
        });
    }

    private PendingResult<Status> callVoid(GoogleApiClient client, final Runnable runnable) {
        return GmsConnector.call(client, ActivityRecognition.API, new GmsConnector.Callback<ActivityRecognitionClientImpl, Status>() {
            @Override
            public void onClientAvailable(ActivityRecognitionClientImpl client, ResultProvider<Status> resultProvider) throws RemoteException {
                runnable.run(client);
                resultProvider.onResultAvailable(Status.SUCCESS);
            }
        });
    }

    private interface Runnable {
        void run(ActivityRecognitionClientImpl client) throws RemoteException;
    }
}
