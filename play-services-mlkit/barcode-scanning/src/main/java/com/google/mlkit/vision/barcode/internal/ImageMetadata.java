/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.barcode.internal;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class ImageMetadata extends AbstractSafeParcelable {
    @Field(1)
    public int format;
    @Field(2)
    public int width;
    @Field(3)
    public int height;
    @Field(4)
    public int rotation;
    @Field(5)
    public long timestamp;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ImageMetadata")
                .field("format", format)
                .field("width", width)
                .field("height", height)
                .field("rotation", rotation)
                .field("timestamp", timestamp)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ImageMetadata> CREATOR = findCreator(ImageMetadata.class);
}
