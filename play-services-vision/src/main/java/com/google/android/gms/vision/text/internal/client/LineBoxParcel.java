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
public class LineBoxParcel extends AbstractSafeParcelable {
    @Field(2)
    public final WordBoxParcel[] words;

    @Field(3)
    public final BoundingBoxParcel boundingBox;

    @Field(4)
    public final BoundingBoxParcel originalBoundingBox;

    @Field(5)
    public final BoundingBoxParcel baselineBox;

    @Field(6)
    public final String value;

    @Field(7)
    public final float confidence;

    @Field(8)
    public final String language;

    @Field(9)
    public final int id;

    @Field(10)
    public final boolean isVertical;

    @Field(11)
    public final int blockId;

    @Field(12)
    public final int lineId;

    @Constructor
    public LineBoxParcel(@Param(2) WordBoxParcel[] words, @Param(3) BoundingBoxParcel boundingBox, @Param(4) BoundingBoxParcel originalBoundingBox, @Param(5) BoundingBoxParcel baselineBox, @Param(6) String value, @Param(7) float confidence, @Param(8) String language, @Param(9) int id, @Param(10) boolean isVertical, @Param(11) int blockId, @Param(12) int lineId) {
        this.words = words;
        this.boundingBox = boundingBox;
        this.originalBoundingBox = originalBoundingBox;
        this.baselineBox = baselineBox;
        this.value = value;
        this.confidence = confidence;
        this.language = language;
        this.id = id;
        this.isVertical = isVertical;
        this.blockId = blockId;
        this.lineId = lineId;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LineBoxParcel> CREATOR = findCreator(LineBoxParcel.class);
}
