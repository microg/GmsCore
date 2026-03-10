/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face.aidls;

import android.graphics.Rect;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class FaceParcel extends AbstractSafeParcelable {
    @Field(1)
    public final int id;

    @Field(2)
    public final Rect boundingBox;

    @Field(3)
    public final float rollAngle;

    @Field(4)
    public final float panAngle;

    @Field(5)
    public final float tiltAngle;

    @Field(6)
    public final float leftEyeOpenProbability;

    @Field(7)
    public final float rightEyeOpenProbability;

    @Field(8)
    public final float smileProbability;

    @Field(9)
    public final float confidenceScore;

    @Field(10)
    public final List<LandmarkParcel> landmarkParcelList;

    @Field(11)
    public final List<ContourParcel> contourParcelList;

    @Constructor
    public FaceParcel(@Param(1) int id, @Param(2) Rect boundingBox, @Param(3) float rollAngle, @Param(4) float panAngle, @Param(5) float tiltAngle, @Param(6) float leftEyeOpenProbability, @Param(7) float rightEyeOpenProbability, @Param(8) float smileProbability, @Param(9) float confidenceScore, @Param(10) List<LandmarkParcel> landmarkParcelList, @Param(11) List<ContourParcel> contourParcelList) {
        this.id = id;
        this.boundingBox = boundingBox;
        this.rollAngle = rollAngle;
        this.panAngle = panAngle;
        this.tiltAngle = tiltAngle;
        this.leftEyeOpenProbability = leftEyeOpenProbability;
        this.rightEyeOpenProbability = rightEyeOpenProbability;
        this.smileProbability = smileProbability;
        this.confidenceScore = confidenceScore;
        this.landmarkParcelList = landmarkParcelList;
        this.contourParcelList = contourParcelList;
    }


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FaceParcel> CREATOR = findCreator(FaceParcel.class);
}
