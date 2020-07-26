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
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import org.microg.gms.location.LocationConstants;

@Deprecated
public interface FusedLocationProviderApi {
    @Deprecated
    String KEY_LOCATION_CHANGED = "com.google.android.location.LOCATION";
    String KEY_MOCK_LOCATION = LocationConstants.KEY_MOCK_LOCATION;

    Location getLastLocation(GoogleApiClient client);

    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request,
                                                 LocationListener listener);

    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request,
                                         LocationListener listener, Looper looper);

    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request,
                                         PendingIntent callbackIntent);

    PendingResult<Status> removeLocationUpdates(GoogleApiClient client, LocationListener listener);

    PendingResult<Status> removeLocationUpdates(GoogleApiClient client,
                                        PendingIntent callbackIntent);

    PendingResult<Status> setMockMode(GoogleApiClient client, boolean isMockMode);

    PendingResult<Status> setMockLocation(GoogleApiClient client, Location mockLocation);
}
