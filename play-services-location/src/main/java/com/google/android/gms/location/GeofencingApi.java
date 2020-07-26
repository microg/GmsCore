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

package com.google.android.gms.location;

import android.app.PendingIntent;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import java.util.List;

/**
 * The main entry point for interacting with the geofencing APIs.
 * <p>
 * The methods must be used in conjunction with a GoogleApiClient. E.g.
 * <pre>
 *  new GoogleApiClient.Builder(context)
 *          .addApi(LocationServices.API)
 *          .addConnectionCallbacks(this)
 *          .addOnConnectionFailedListener(this)
 *          .build()
 * </pre>
 */
@Deprecated
public interface GeofencingApi {
    PendingResult<Status> addGeofences(GoogleApiClient client, GeofencingRequest geofencingRequest, PendingIntent pendingIntent);

    @Deprecated
    PendingResult<Status> addGeofences(GoogleApiClient client, List<Geofence> geofences, PendingIntent pendingIntent);

    PendingResult<Status> removeGeofences(GoogleApiClient client, List<String> geofenceRequestIds);

    PendingResult<Status> removeGeofences(GoogleApiClient client, PendingIntent pendingIntent);
}
