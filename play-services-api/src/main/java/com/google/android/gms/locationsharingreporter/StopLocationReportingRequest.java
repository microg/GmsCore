/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class StopLocationReportingRequest extends AbstractSafeParcelable {
    @Field(1)
    public final LocationShare locationShare;
    @Constructor
    public StopLocationReportingRequest(@Param(1) LocationShare locationShare) {
        this.locationShare = locationShare;
    }
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<StopLocationReportingRequest> CREATOR = findCreator(StopLocationReportingRequest.class);

    @Override
    public String toString() {
        return "StopLocationReportingRequest{" +
                "locationShare=" + (locationShare != null ? locationShare.toString() : "null") +
                '}';
    }
}
