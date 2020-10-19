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
package com.google.android.gms.location

import android.app.PendingIntent
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Status

/**
 * The main entry point for interacting with the geofencing APIs.
 *
 *
 * The methods must be used in conjunction with a GoogleApiClient. E.g.
 * <pre>
 * new GoogleApiClient.Builder(context)
 * .addApi(LocationServices.API)
 * .addConnectionCallbacks(this)
 * .addOnConnectionFailedListener(this)
 * .build()
</pre> *
 */
@Deprecated("")
interface GeofencingApi {
    fun addGeofences(client: GoogleApiClient?, geofencingRequest: GeofencingRequest?, pendingIntent: PendingIntent?): PendingResult<Status?>?

    @Deprecated("")
    fun addGeofences(client: GoogleApiClient?, geofences: List<Geofence?>?, pendingIntent: PendingIntent?): PendingResult<Status?>?
    fun removeGeofences(client: GoogleApiClient?, geofenceRequestIds: List<String?>?): PendingResult<Status?>?
    fun removeGeofences(client: GoogleApiClient?, pendingIntent: PendingIntent?): PendingResult<Status?>?
}