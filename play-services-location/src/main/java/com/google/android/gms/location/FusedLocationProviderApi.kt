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
import android.location.Location
import android.os.Looper
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Status
import org.microg.gms.location.LocationConstants

@Deprecated("")
interface FusedLocationProviderApi {
    fun getLastLocation(client: GoogleApiClient?): Location?
    fun requestLocationUpdates(client: GoogleApiClient?, request: LocationRequest?,
                               listener: LocationListener?): PendingResult<Status?>?

    fun requestLocationUpdates(client: GoogleApiClient?, request: LocationRequest?,
                               listener: LocationListener?, looper: Looper?): PendingResult<Status?>?

    fun requestLocationUpdates(client: GoogleApiClient?, request: LocationRequest?,
                               callbackIntent: PendingIntent?): PendingResult<Status?>?

    fun removeLocationUpdates(client: GoogleApiClient?, listener: LocationListener?): PendingResult<Status?>?
    fun removeLocationUpdates(client: GoogleApiClient?,
                              callbackIntent: PendingIntent?): PendingResult<Status?>?

    fun setMockMode(client: GoogleApiClient?, isMockMode: Boolean): PendingResult<Status?>?
    fun setMockLocation(client: GoogleApiClient?, mockLocation: Location?): PendingResult<Status?>?

    companion object {
        @Deprecated("")
        const val KEY_LOCATION_CHANGED = "com.google.android.location.LOCATION"
        const val KEY_MOCK_LOCATION = LocationConstants.KEY_MOCK_LOCATION
    }
}