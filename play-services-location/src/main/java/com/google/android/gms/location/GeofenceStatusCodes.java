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

package com.google.android.gms.location;

import com.google.android.gms.common.api.CommonStatusCodes;

public class GeofenceStatusCodes extends CommonStatusCodes {
    /**
     * Geofence service is not available now. Typically this is because the user turned off
     * location access in settings > location access.
     */
    public static final int GEOFENCE_NOT_AVAILABLE = 1000;

    /**
     * Your app has registered more than 100 geofences. Remove unused ones before adding new
     * geofences.
     */
    public static final int GEOFENCE_TOO_MANY_GEOFENCES = 1001;

    /**
     * You have provided more than 5 different PendingIntents to the addGeofences(GoogleApiClient,
     * GeofencingRequest, PendingIntent) call.
     */
    public static final int GEOFENCE_TOO_MANY_PENDING_INTENTS = 1002;

    /**
     * Returns an untranslated debug (not user-friendly!) string based on the current status code.
     */
    public static String getStatusCodeString(int statusCode) {
        switch (statusCode) {
            case GEOFENCE_NOT_AVAILABLE:
                return "GEOFENCE_NOT_AVAILABLE";
            case GEOFENCE_TOO_MANY_GEOFENCES:
                return "GEOFENCE_TOO_MANY_GEOFENCES";
            case GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "GEOFENCE_TOO_MANY_PENDING_INTENTS";
            default:
                return CommonStatusCodes.getStatusCodeString(statusCode);
        }
    }
}
