/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.app.PendingIntent;

/**
 * Status codes that can be returned to listeners to indicate the success or failure of an operation.
 * @deprecated Use {@link GeofenceStatusCodes}
 */
@Deprecated
public class LocationStatusCodes {
    /**
     * The operation was successful.
     */
    public static final int SUCCESS = 0;
    /**
     * An unspecified error occurred; no more specific information is available. The device logs may provide additional
     * data.
     */
    public static final int ERROR = 1;
    /**
     * Geofence service is not available now. Typically this is because the user turned off location access in
     * settings > location access.
     */
    public static final int GEOFENCE_NOT_AVAILABLE = 1000;
    /**
     * Your app has registered more than 100 geofences. Remove unused ones before adding new geofences.
     */
    public static final int GEOFENCE_TOO_MANY_GEOFENCES = 1001;
    /**
     * You have provided more than 5 different PendingIntents to the {@link GeofencingApi#addGeofences(com.google.android.gms.common.api.GoogleApiClient, GeofencingRequest, PendingIntent)} call.
     */
    public static final int GEOFENCE_TOO_MANY_PENDING_INTENTS = 1002;
}
