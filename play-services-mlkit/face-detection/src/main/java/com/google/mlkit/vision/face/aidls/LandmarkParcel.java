/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face.aidls;

import android.graphics.PointF;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class LandmarkParcel extends AbstractSafeParcelable {
    @Field(1)
    public final int type;
    @Field(2)
    public final PointF position;

    @Constructor
    public LandmarkParcel(@Param(1) int type, @Param(2) PointF position) {
        this.type = type;
        this.position = position;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LandmarkParcel> CREATOR = findCreator(LandmarkParcel.class);
}
