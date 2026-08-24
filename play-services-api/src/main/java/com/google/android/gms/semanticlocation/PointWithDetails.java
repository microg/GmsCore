/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class PointWithDetails extends AbstractSafeParcelable {
    @Field(1)
    public final PlaceCandidate.Point point;
    @Field(2)
    public final long timeOffset;
    @Field(3)
    @Deprecated
    int i3;

    @Constructor
    public PointWithDetails(@Param(1) PlaceCandidate.Point point, @Param(2) long timeOffset) {
        this.point = point;
        this.timeOffset = timeOffset;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("PointWithDetails")
                .field("point", point)
                .field("timeOffset", timeOffset)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PointWithDetails> CREATOR = findCreator(PointWithDetails.class);
}
