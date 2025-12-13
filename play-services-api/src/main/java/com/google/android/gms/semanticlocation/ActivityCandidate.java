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
public class ActivityCandidate extends AbstractSafeParcelable {
    @Field(1)
    public final int type;
    @Field(2)
    public final float probability;
    @Field(3)
    @Deprecated
    float f3;

    @Constructor
    public ActivityCandidate(@Param(1) int type, @Param(2) float probability) {
        this.type = type;
        this.probability = probability;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ActivityCandidate")
                .field("type", type)
                .field("probability", probability)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ActivityCandidate> CREATOR = findCreator(ActivityCandidate.class);
}
