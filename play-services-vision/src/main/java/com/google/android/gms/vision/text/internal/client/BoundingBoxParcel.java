/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text.internal.client;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class BoundingBoxParcel extends AbstractSafeParcelable {
    @Field(2)
    public final int left;

    @Field(3)
    public final int top;

    @Field(4)
    public final int width;

    @Field(5)
    public final int height;

    @Field(6)
    public final float angle;

    @Constructor
    public BoundingBoxParcel(@Param(2) int left, @Param(3) int top, @Param(4) int width, @Param(5) int height, @Param(6) float angle) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.angle = angle;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<BoundingBoxParcel> CREATOR = findCreator(BoundingBoxParcel.class);
}
