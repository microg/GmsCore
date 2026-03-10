/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.barcode.internal;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class CalendarDateTime extends AbstractSafeParcelable {
    @Field(1)
    public int year;
    @Field(2)
    public int month;
    @Field(3)
    public int day;
    @Field(4)
    public int hours;
    @Field(5)
    public int minutes;
    @Field(6)
    public int seconds;
    @Field(7)
    public boolean isUtc;
    @Field(8)
    @Nullable
    public String rawValue;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CalendarDateTime> CREATOR = findCreator(CalendarDateTime.class);
}
