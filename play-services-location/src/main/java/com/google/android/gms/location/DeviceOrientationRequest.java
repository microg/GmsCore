/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location;

import android.os.SystemClock;

import org.microg.safeparcel.AutoSafeParcelable;

public class DeviceOrientationRequest extends AutoSafeParcelable {
    @Field(1)
    public boolean shouldUseMag;
    @Field(2)
    public long minimumSamplingPeriodMs;
    @Field(3)
    public float smallesAngleChangeRadians;
    @Field(4)
    public long expirationTime;
    @Field(5)
    public int numUpdates;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Request[shouldUseMag=").append(shouldUseMag);
        sb.append(" minimumSamplingPeriod=").append(minimumSamplingPeriodMs).append("ms");
        sb.append(" smallesAngleChange=").append(smallesAngleChangeRadians).append("rad");
        if (expirationTime != Long.MAX_VALUE)
            sb.append(" expireIn=").append(expirationTime - SystemClock.elapsedRealtime()).append("ms");
        if (numUpdates != Integer.MAX_VALUE)
            sb.append(" num=").append(numUpdates);
        sb.append("]");
        return sb.toString();
    }

    public static final Creator<DeviceOrientationRequest> CREATOR = new AutoCreator<DeviceOrientationRequest>(DeviceOrientationRequest.class);
}
