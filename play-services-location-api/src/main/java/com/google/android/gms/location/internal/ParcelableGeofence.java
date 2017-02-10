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

package com.google.android.gms.location.internal;

import com.google.android.gms.location.Geofence;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ParcelableGeofence extends AutoSafeParcelable implements Geofence {

    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    public String requestId;

    @SafeParceled(2)
    public long expirationTime;

    @SafeParceled(3)
    public int regionType;

    @SafeParceled(4)
    public double latitude;

    @SafeParceled(5)
    public double longitude;

    @SafeParceled(6)
    public float radius;

    @SafeParceled(7)
    public int transitionType;

    @SafeParceled(8)
    public int notificationResponsiveness;

    @SafeParceled(9)
    public int loiteringDelay;

    private ParcelableGeofence() {
    }

    public ParcelableGeofence(String requestId, long expirationTime, int regionType, double latitude, double longitude, float radius, int transitionType, int notificationResponsiveness, int loiteringDelay) {
        this.requestId = requestId;
        this.expirationTime = expirationTime;
        this.regionType = regionType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.transitionType = transitionType;
        this.notificationResponsiveness = notificationResponsiveness;
        this.loiteringDelay = loiteringDelay;
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    public static final Creator<ParcelableGeofence> CREATOR = new AutoCreator<ParcelableGeofence>(ParcelableGeofence.class);
}
