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

import java.util.List;

@SafeParcelable.Class
public class ContourParcel extends AbstractSafeParcelable {
    @Field(1)
    public final int type;
    @Field(2)
    public final List<PointF> pointsList;

    @Constructor
    public ContourParcel(@Param(1) int type, @Param(2) List<PointF> pointsList) {
        this.type = type;
        this.pointsList = pointsList;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ContourParcel> CREATOR = findCreator(ContourParcel.class);
}
