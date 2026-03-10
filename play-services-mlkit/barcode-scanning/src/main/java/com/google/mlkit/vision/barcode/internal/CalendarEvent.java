/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.barcode.internal;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class CalendarEvent extends AbstractSafeParcelable {
    @Field(1)
    public String summary;
    @Field(2)
    public String description;
    @Field(3)
    public String location;
    @Field(4)
    public String organizer;
    @Field(5)
    public String status;
    @Field(6)
    public CalendarDateTime start;
    @Field(7)
    public CalendarDateTime end;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CalendarEvent> CREATOR = findCreator(CalendarEvent.class);
}
