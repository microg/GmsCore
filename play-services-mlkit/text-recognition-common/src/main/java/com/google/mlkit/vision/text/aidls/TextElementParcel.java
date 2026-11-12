/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text.aidls;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import android.graphics.Rect;
import android.graphics.Point;

import java.util.List;
import java.util.ArrayList;

@SafeParcelable.Class
public class TextElementParcel extends AbstractSafeParcelable {
    @Field(1)
    public String text = "";

    @Field(2)
    public Rect boundingBox = new Rect();

    @Field(3)
    public List<Point> cornerPoints = new ArrayList<>();

    @Field(4)
    public String language = "und";

    @Field(5)
    public float confidence = 0f;

    @Field(6)
    public float angle = 0f;

    @Field(7)
    public List<TextSymbolParcel> symbols = new ArrayList<>();

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<TextElementParcel> CREATOR = findCreator(TextElementParcel.class);
}
