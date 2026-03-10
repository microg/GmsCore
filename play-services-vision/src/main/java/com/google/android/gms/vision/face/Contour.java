/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.face;

import android.graphics.PointF;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class Contour extends AbstractSafeParcelable {

    @Field(1)
    public int type;
    @Field(2)
    public List<PointF> points;

    @Constructor
    public Contour(@Param(1) int type, @Param(2) List<PointF> points) {
        this.type = type;
        this.points = points;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Contour> CREATOR = findCreator(Contour.class);

}
