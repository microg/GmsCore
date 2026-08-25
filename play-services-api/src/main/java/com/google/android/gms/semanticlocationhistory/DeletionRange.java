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

@SafeParcelable.Class
public class DeletionRange extends AbstractSafeParcelable {
    @Field(1)
    public final long startTimestampSeconds;
    @Field(2)
    public final long endTimestampSeconds;

    @Constructor
    public DeletionRange(@Param(1) long startTimestampSeconds, @Param(2) long endTimestampSeconds) {
        this.startTimestampSeconds = startTimestampSeconds;
        this.endTimestampSeconds = endTimestampSeconds;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DeletionRange> CREATOR = findCreator(DeletionRange.class);
}
