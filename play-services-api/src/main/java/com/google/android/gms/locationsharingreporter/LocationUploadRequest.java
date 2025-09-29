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
public class LocationUploadRequest extends AbstractSafeParcelable {
    @Field(1)
    public final long accuracyMeters;
    @Field(2)
    public final int numberOfFix;
    @Field(3)
    public final long intervalMillis;
    @Field(4)
    public final long fastIntervalMillis;
    @Field(5)
    public final long expirationMillis;
    @Field(6)
    public final LocationCollectionReason locationCollectionReason;
    @Field(7)
    public final boolean unknownBool7;
    @Field(8)
    public final boolean throttleExempt;
    @Field(9)
    public final String moduleId;
    @Field(10)
    public final String unknownString10;
    @Field(11)
    public final long unknownLong11;

    @Constructor
    public LocationUploadRequest(@Param(1) long accuracyMeters, @Param(2) int numberOfFix, @Param(3) long intervalMillis, @Param(4) long fastIntervalMillis,
                                 @Param(5) long expirationMillis, @Param(6) LocationCollectionReason locationCollectionReason,
                                 @Param(7) boolean unknownBool7, @Param(8) boolean throttleExempt, @Param(9) String moduleId, @Param(10) String unknownString10, @Param(11) long unknownLong11) {
        this.accuracyMeters = accuracyMeters;
        this.numberOfFix = numberOfFix;
        this.intervalMillis = intervalMillis;
        this.fastIntervalMillis = fastIntervalMillis;
        this.expirationMillis = expirationMillis;
        this.locationCollectionReason = locationCollectionReason;
        this.unknownBool7 = unknownBool7;
        this.throttleExempt = throttleExempt;
        this.moduleId = moduleId;
        this.unknownString10 = unknownString10;
        this.unknownLong11 = unknownLong11;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LocationUploadRequest> CREATOR = findCreator(LocationUploadRequest.class);
}
