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
public class PeriodicLocationUploadRequest extends AbstractSafeParcelable {
    @Field(1)
    public final LocationCollectionReason locationCollectionReason;
    @Field(2)
    public final LocationShare locationShare;
    @Field(3)
    public final int makePrimaryOption;
    @Field(4)
    public final long duration;
    @Field(5)
    public final boolean unknownBool5;
    @Field(6)
    public final String unknownStr6;
    @Field(7)
    public final boolean unknownBool7;

    @Constructor
    public PeriodicLocationUploadRequest(@Param(1) LocationCollectionReason locationCollectionReason, @Param(2) LocationShare locationShare,
                                         @Param(3) int makePrimaryOption, @Param(4) long duration, @Param(5) boolean unknownBool5, @Param(6) String unknownStr6, @Param(7) boolean unknownBool7) {
        this.locationCollectionReason = locationCollectionReason;
        this.locationShare = locationShare;
        this.makePrimaryOption = makePrimaryOption;
        this.duration = duration;
        this.unknownBool5 = unknownBool5;
        this.unknownStr6 = unknownStr6;
        this.unknownBool7 = unknownBool7;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PeriodicLocationUploadRequest> CREATOR = findCreator(PeriodicLocationUploadRequest.class);
}
