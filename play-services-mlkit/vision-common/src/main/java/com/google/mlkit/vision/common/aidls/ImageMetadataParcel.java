/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.common.aidls;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class ImageMetadataParcel extends AbstractSafeParcelable {
    @Field(1)
    public int format = 0;

    @Field(2)
    public int width = 0;

    @Field(3)
    public int height = 0;

    @Field(4)
    public int rotation = 0;

    @Field(5)
    public long timestampMillis = 0L;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ImageMetadataParcel> CREATOR = findCreator(ImageMetadataParcel.class);
}
