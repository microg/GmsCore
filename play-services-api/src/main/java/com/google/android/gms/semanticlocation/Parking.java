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
public class Parking extends AbstractSafeParcelable {
    @Field(1)
    public final long startTime;
    @Field(2)
    public final long endTime;
    @Field(3)
    public final PlaceCandidate.Point point;
    @Field(4)
    int i4;
    @Field(5)
    int i5;
    @Field(6)
    int i6;
    @Field(7)
    float f7;

    @Constructor
    public Parking(@Param(1) long startTime, @Param(2) long endTime, @Param(3) PlaceCandidate.Point point) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.point = point;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Parking")
                .field("startTime", startTime)
                .field("endTime", endTime)
                .field("point", point)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Parking> CREATOR = findCreator(Parking.class);
}
