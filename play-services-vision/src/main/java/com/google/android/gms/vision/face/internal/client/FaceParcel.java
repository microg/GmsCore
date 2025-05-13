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

    @Field(value = 1, defaultValue = "1")
    public final int versionCode;
    @Field(2)
    public final int id;
    @Field(3)
    public final float centerX;
    @Field(4)
    public final float centerY;
    @Field(5)
    public final float width;
    @Field(6)
    public final float height;
    @Field(7)
    public final float eulerY;
    @Field(8)
    public final float eulerZ;
    @Field(14)
    public final float eulerX;
    @Field(9)
    public final Landmark[] landmarks;
    @Field(10)
    public final float leftEyeOpenProbability;
    @Field(11)
    public final float rightEyeOpenProbability;
    @Field(12)
    public final float smileProbability;
    @Field(13)
    public final Contour[] contours;
    @Field(value = 15, defaultValue = "-1.0f")
    public final float confidenceScore;

    @Constructor
    public FaceParcel(@Param(1) int versionCode, @Param(2) int id, @Param(3) float centerX, @Param(4) float centerY, @Param(5) float width, @Param(6) float height, @Param(7) float eulerY, @Param(8) float eulerZ, @Param(14) float eulerX, @Param(9) Landmark[] landmarks, @Param(10) float leftEyeOpenProbability, @Param(11) float rightEyeOpenProbability, @Param(12) float smileProbability, @Param(13) Contour[] contours, @Param(15) float confidenceScore) {
        this.versionCode = versionCode;
        this.id = id;
        this.centerX = centerX;
        this.centerY = centerY;
        this.width = width;
        this.height = height;
        this.eulerY = eulerY;
        this.eulerZ = eulerZ;
        this.eulerX = eulerX;
        this.landmarks = landmarks;
        this.leftEyeOpenProbability = leftEyeOpenProbability;
        this.rightEyeOpenProbability = rightEyeOpenProbability;
        this.smileProbability = smileProbability;
        this.contours = contours;
        this.confidenceScore = confidenceScore;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FaceParcel> CREATOR = findCreator(FaceParcel.class);
}
