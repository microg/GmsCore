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
public class IneligibilityRationale extends AbstractSafeParcelable {
    @Field(1)
    public final String unknownStr1;
    @Field(2)
    public final boolean unknownBool2;
    @Field(3)
    public final String unknownStr3;
    @Field(4)
    public final boolean unknownBool4;
    @Field(5)
    public final boolean unknownBool5;
    @Field(6)
    public final boolean unknownBool6;

    @Constructor
    public IneligibilityRationale(@Param(1) String unknownStr1, @Param(2) boolean unknownBool2, @Param(3) String unknownStr3,
                                  @Param(4) boolean unknownBool4, @Param(5) boolean unknownBool5, @Param(6) boolean unknownBool6) {
        this.unknownStr1 = unknownStr1;
        this.unknownBool2 = unknownBool2;
        this.unknownStr3 = unknownStr3;
        this.unknownBool4 = unknownBool4;
        this.unknownBool5 = unknownBool5;
        this.unknownBool6 = unknownBool6;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<IneligibilityRationale> CREATOR = findCreator(IneligibilityRationale.class);
}
