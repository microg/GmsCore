/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class FrameMetadataParcel extends AbstractSafeParcelable {

    @Field(1)
    public int format;
    @Field(2)
    public int width;
    @Field(3)
    public int height;
    @Field(4)
    public int rotation;
    @Field(5)
    public long timestampMillis;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FrameMetadataParcel> CREATOR = findCreator(FrameMetadataParcel.class);

}
