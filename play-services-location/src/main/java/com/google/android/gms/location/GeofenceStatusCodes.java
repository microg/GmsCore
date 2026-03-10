/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.app.PendingIntent;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

/**
 * Geofence specific status codes, for use in {@link Status#getStatusCode()}
 */
public class GeofenceStatusCodes extends CommonStatusCodes {
    /**
     * Geofence service is not available now. Typically this is because the user turned off location access in settings > location access.
     */
    public static final int GEOFENCE_NOT_AVAILABLE = 1000;

    /**
     * Your app has registered more than 100 geofences. Remove unused ones before adding new geofences.
     */
    public static final int GEOFENCE_TOO_MANY_GEOFENCES = 1001;

    /**
     * You have provided more than 5 different PendingIntents to the {@link GeofencingApi#addGeofences(GoogleApiClient, GeofencingRequest, PendingIntent)} call.
     */
    public static final int GEOFENCE_TOO_MANY_PENDING_INTENTS = 1002;

    /**
     * The client doesn't have sufficient location permission to perform geofencing operations.
     */
    public static final int GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION = 1004;

    /**
     * Your app has been adding Geofences too frequently.
     */
    public static final int GEOFENCE_REQUEST_TOO_FREQUENT = 1005;

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
            case GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION:
                return "GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION";
            case GEOFENCE_REQUEST_TOO_FREQUENT:
                return "GEOFENCE_REQUEST_TOO_FREQUENT";
            default:
                return CommonStatusCodes.getStatusCodeString(statusCode);
        }
    }
}
