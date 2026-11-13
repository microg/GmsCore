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
public class WordBoxParcel extends AbstractSafeParcelable {
    @Field(2)
    public final SymbolBoxParcel[] symbols;

    @Field(3)
    public final BoundingBoxParcel boundingBox;

    @Field(4)
    public final BoundingBoxParcel originalBoundingBox;

    @Field(5)
    public final String value;

    @Field(6)
    public final float confidence;

    @Field(7)
    public final String language;

    @Field(8)
    public final boolean hasSpace;

    @Constructor
    public WordBoxParcel(@Param(2) SymbolBoxParcel[] symbols, @Param(3) BoundingBoxParcel boundingBox, @Param(4) BoundingBoxParcel originalBoundingBox, @Param(5) String value, @Param(6) float confidence, @Param(7) String language, @Param(8) boolean hasSpace) {
        this.symbols = symbols;
        this.boundingBox = boundingBox;
        this.originalBoundingBox = originalBoundingBox;
        this.value = value;
        this.confidence = confidence;
        this.language = language;
        this.hasSpace = hasSpace;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<WordBoxParcel> CREATOR = findCreator(WordBoxParcel.class);
}
