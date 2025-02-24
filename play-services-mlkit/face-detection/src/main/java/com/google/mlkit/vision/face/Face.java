/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face;

import android.graphics.Rect;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class Face extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getTrackingId")
    private final int trackingId;
    @Field(value = 2, getterName = "getBoundingBox")
    private final Rect boundingBox;
    @Field(value = 3, getterName = "getHeadEulerAngleX")
    private final Float headEulerAngleX;
    @Field(value = 4, getterName = "getHeadEulerAngleY")
    private final Float headEulerAngleY;
    @Field(value = 5, getterName = "getHeadEulerAngleZ")
    private final Float headEulerAngleZ;
    @Field(value = 6, getterName = "getLeftEyeOpenProbability")
    private final Float leftEyeOpenProbability;
    @Field(value = 7, getterName = "getRightEyeOpenProbability")
    private final Float rightEyeOpenProbability;
    @Field(value = 8, getterName = "getSmilingProbability")
    private final Float smilingProbability;
    @Field(value = 9, getterName = "getRotation")
    private final Float rotation;
    @Field(value = 10, getterName = "getAllLandmarks")
    private final List<FaceLandmark> landmarks;
    @Field(value = 11, getterName = "getAllContours")
    private final List<FaceContour> contours;

    @Constructor
    public Face(@Param(1) int trackingId, @Param(2) Rect boundingBox, @Param(3) Float headEulerAngleX, @Param(4) Float headEulerAngleY, @Param(5) Float headEulerAngleZ, @Param(6) Float leftEyeOpenProbability, @Param(7) Float rightEyeOpenProbability, @Param(8) Float smilingProbability, @Param(9) Float rotation, @Param(10) List<FaceLandmark> landmarks, @Param(11) List<FaceContour> contours) {
        this.trackingId = trackingId;
        this.boundingBox = boundingBox;
        this.headEulerAngleX = headEulerAngleX;
        this.headEulerAngleY = headEulerAngleY;
        this.headEulerAngleZ = headEulerAngleZ;
        this.leftEyeOpenProbability = leftEyeOpenProbability;
        this.rightEyeOpenProbability = rightEyeOpenProbability;
        this.smilingProbability = smilingProbability;
        this.rotation = rotation;
        this.landmarks = landmarks;
        this.contours = contours;
    }

    public int getTrackingId() {
        return trackingId;
    }

    public Rect getBoundingBox() {
        return boundingBox;
    }

    public Float getHeadEulerAngleZ() {
        return headEulerAngleZ;
    }

    public Float getHeadEulerAngleY() {
        return headEulerAngleY;
    }

    public Float getHeadEulerAngleX() {
        return headEulerAngleX;
    }

    public Float getLeftEyeOpenProbability() {
        return leftEyeOpenProbability;
    }

    public Float getRightEyeOpenProbability() {
        return rightEyeOpenProbability;
    }

    public Float getSmilingProbability() {
        return smilingProbability;
    }

    public Float getRotation() {
        return rotation;
    }

    public List<FaceLandmark> getAllLandmarks() {
        return landmarks;
    }

    public List<FaceContour> getAllContours() {
        return contours;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Face> CREATOR = findCreator(Face.class);

    @Override
    public String toString() {
        return "Face{" +
                "trackingId=" + trackingId +
                ", boundingBox=" + boundingBox +
                ", headEulerAngleX=" + headEulerAngleX +
                ", headEulerAngleY=" + headEulerAngleY +
                ", headEulerAngleZ=" + headEulerAngleZ +
                ", leftEyeOpenProbability=" + leftEyeOpenProbability +
                ", rightEyeOpenProbability=" + rightEyeOpenProbability +
                ", smilingProbability=" + smilingProbability +
                ", rotation=" + rotation +
                ", landmarks=" + landmarks +
                ", contours=" + contours +
                '}';
    }
}
