/**
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
    public int index;
    @Field(2)
    public String tag;
    @Field(3)
    public TimeRangeFilter timeRangeFilter;
    @Field(4)
    public boolean check;
    @Field(5)
    public Integer start;
    @Field(6)
    public Integer end;

    public LookupParameters() {
    }

    public LookupParameters(@Param(1) int index, @Param(2) String tag, @Param(3) TimeRangeFilter timeRangeFilter, @Param(4) boolean check, @Param(5) Integer start, @Param(6) Integer end){
        this.index = index;
        this.tag = tag;
        this.timeRangeFilter = timeRangeFilter;
        this.check = check;
        this.start = start;
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
                .field("index", index)
                .field("tag", tag)
                .field("timeRangeFilter", timeRangeFilter)
                .field("check", check)
                .field("start", start)
                .field("end", end)
                .end();
    }
}
