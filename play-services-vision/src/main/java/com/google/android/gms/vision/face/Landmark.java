/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.face;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class Landmark extends AbstractSafeParcelable {

    @Field(1)
    public int id;
    @Field(2)
    public Float x;
    @Field(3)
    public Float y;
    @Field(4)
    public int type;

    @Constructor
    public Landmark(@Param(1) int id, @Param(2) Float x, @Param(3) Float y, @Param(4) int type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Landmark> CREATOR = findCreator(Landmark.class);

}
