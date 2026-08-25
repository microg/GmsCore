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
public class Date extends AbstractSafeParcelable {
    @Field(1)
    public final int year;
    @Field(2)
    public final int month;
    @Field(3)
    public final int day;

    @Constructor
    public Date(@Param(1) int year, @Param(2) int month, @Param(3) int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Date").value(String.format("%04d-%02d-%02d", year, month, day)).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Date> CREATOR = findCreator(Date.class);
}
