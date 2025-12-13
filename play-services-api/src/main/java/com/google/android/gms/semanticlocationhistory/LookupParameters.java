/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class LookupParameters extends AbstractSafeParcelable {

    @Field(1)
    public final int type;
    @Field(2)
    public final String segmentId;
    @Field(3)
    public final TimeRangeFilter timeRangeFilter;
    @Field(4)
    public final boolean b4;
    @Field(5)
    public final Integer i5;
    @Field(6)
    public final Integer i6;
    @Field(7)
    public final Long fprint;

    @Constructor
    public LookupParameters(@Param(1) int type, @Param(2) String segmentId, @Param(3) TimeRangeFilter timeRangeFilter, @Param(4) boolean b4, @Param(5) Integer i5, @Param(6) Integer i6, @Param(7) Long fprint){
        this.type = type;
        this.segmentId = segmentId;
        this.timeRangeFilter = timeRangeFilter;
        this.b4 = b4;
        this.i5 = i5;
        this.i6 = i6;
        this.fprint = fprint;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LookupParameters> CREATOR = findCreator(LookupParameters.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("LookupParameters")
                .field("type", type)
                .field("segmentId", segmentId)
                .field("timeRangeFilter", timeRangeFilter)
                .field("fprint", fprint)
                .end();
    }
}
