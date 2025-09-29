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
public class LocationUploadResponse extends AbstractSafeParcelable {
    @Field(1)
    public final int unknownInt1;
    @Field(2)
    public final LocationReportingStatus locationReportingStatus;

    @Constructor
    public LocationUploadResponse(@Param(1) int unknownInt1, @Param(2) LocationReportingStatus locationReportingStatus) {
        this.unknownInt1 = unknownInt1;
        this.locationReportingStatus = locationReportingStatus;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LocationUploadResponse> CREATOR = findCreator(LocationUploadResponse.class);
}
