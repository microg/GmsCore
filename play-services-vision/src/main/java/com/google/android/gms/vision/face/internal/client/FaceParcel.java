/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.face.internal.client;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.vision.face.Contour;
import com.google.android.gms.vision.face.Landmark;

@SafeParcelable.Class
public class FaceParcel extends AbstractSafeParcelable {

    @Field(1)
    private final int versionCode = 1;
    @Field(2)
    public int id;
    @Field(3)
    public Float screenWidth;
    @Field(4)
    public Float screenHeight;
    @Field(5)
    public Float width;
    @Field(6)
    public Float height;
    @Field(7)
    public Float eulerX;
    @Field(8)
    public Float eulerY;
    @Field(9)
    public Landmark[] landmarks;
    @Field(10)
    public Float leftEyeOpenProbability;
    @Field(11)
    public Float rightEyeOpenProbability;
    @Field(12)
    public Float smilingProbability;
    @Field(13)
    public Contour[] contours;
    @Field(14)
    public Float eulerZ;

    @Constructor
    public FaceParcel(@Param(2) int id, @Param(3) Float screenWidth, @Param(4) Float screenHeight, @Param(5) Float width, @Param(6) Float height, @Param(7) Float eulerX, @Param(8) Float eulerY, @Param(9) Landmark[] landmarks, @Param(10) Float leftEyeOpenProbability, @Param(11) Float rightEyeOpenProbability, @Param(12) Float smilingProbability, @Param(13) Contour[] contours, @Param(14) Float eulerZ) {
        this.id = id;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.width = width;
        this.height = height;
        this.eulerX = eulerX;
        this.eulerY = eulerY;
        this.landmarks = landmarks;
        this.leftEyeOpenProbability = leftEyeOpenProbability;
        this.rightEyeOpenProbability = rightEyeOpenProbability;
        this.smilingProbability = smilingProbability;
        this.contours = contours;
        this.eulerZ = eulerZ;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FaceParcel> CREATOR = findCreator(FaceParcel.class);
}
