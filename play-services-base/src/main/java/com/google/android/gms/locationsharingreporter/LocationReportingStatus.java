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
public class LocationReportingStatus extends AbstractSafeParcelable {
    @Field(1)
    public final int unknownInt1;
    @Field(2)
    public final int unknownInt2;
    @Field(3)
    public final boolean unknownBool3;
    @Field(4)
    public final IneligibilityRationale ineligibilityRationale;

    @Constructor
    public LocationReportingStatus(@Param(1) int unknownInt1, @Param(2) int unknownInt2, @Param(3) boolean unknownBool3, @Param(4) IneligibilityRationale ineligibilityRationale0) {
        this.unknownInt1 = unknownInt1;
        this.unknownInt2 = unknownInt2;
        this.unknownBool3 = unknownBool3;
        this.ineligibilityRationale = ineligibilityRationale0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LocationReportingStatus> CREATOR = findCreator(LocationReportingStatus.class);
}
