/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import com.google.android.gms.location.Geofence;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class ParcelableGeofence extends AutoSafeParcelable implements Geofence {

    @Field(1000)
    private int versionCode = 1;

    @Field(1)
    public String requestId;

    @Field(2)
    public long expirationTime;

    @Field(3)
    public int regionType;

    @Field(4)
    public double latitude;

    @Field(5)
    public double longitude;

    @Field(6)
    public float radius;

    @Field(7)
    public @TransitionTypes int transitionTypes;

    @Field(8)
    public int notificationResponsiveness;

    @Field(9)
    public int loiteringDelay;

    private ParcelableGeofence() {
        notificationResponsiveness = 0;
        loiteringDelay = -1;
    }

    public ParcelableGeofence(String requestId, long expirationTime, int regionType, double latitude, double longitude, float radius, @TransitionTypes int transitionTypes, int notificationResponsiveness, int loiteringDelay) {
        this.requestId = requestId;
        this.expirationTime = expirationTime;
        this.regionType = regionType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.transitionTypes = transitionTypes;
        this.notificationResponsiveness = notificationResponsiveness;
        this.loiteringDelay = loiteringDelay;
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    @Override
    public long getExpirationTime() {
        return expirationTime;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    @Override
    public float getRadius() {
        return radius;
    }

    @Override
    public @TransitionTypes int getTransitionTypes() {
        return transitionTypes;
    }

    @Override
    public int getNotificationResponsiveness() {
        return notificationResponsiveness;
    }

    @Override
    public int getLoiteringDelay() {
        return loiteringDelay;
    }

    public static final Creator<ParcelableGeofence> CREATOR = new AutoCreator<ParcelableGeofence>(ParcelableGeofence.class);
}
