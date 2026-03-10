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
public class TimelineMemory extends AbstractSafeParcelable {
    @Field(1)
    public final Trip trip;
    @Field(2)
    public final Note note;

    @Constructor
    public TimelineMemory(@Param(1) Trip trip, @Param(2) Note note) {
        this.trip = trip;
        this.note = note;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("TimelineMemory")
                .field("trip", trip)
                .field("note", note)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<TimelineMemory> CREATOR = findCreator(TimelineMemory.class);
}
