/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.libraries.camera.capture.lowlightboost.internal;

import android.graphics.Rect;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
@Hide
public class FrameMetadata extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getSensorTimestamp")
    private final long sensorTimestamp;
    @Field(value = 2, getterName = "getSensorSensitivity")
    private final int sensorSensitivity;
    @Field(value = 3, getterName = "getSensorExposureTime")
    private final long sensorExposureTime;
    @Field(value = 4, getterName = "getControlPostRawSensitivityBoost")
    private final int controlPostRawSensitivityBoost;
    @Field(value = 5, getterName = "getFaceBounds")
    @Nullable
    private final Rect[] faceBounds;
    @Field(value = 6, getterName = "getFaceScores")
    @Nullable
    private final int[] faceScores;
    @Field(value = 11, getterName = "getEdgeMode")
    @Nullable
    private final Integer edgeMode;
    @Field(value = 12, getterName = "getTonemapMode")
    @Nullable
    private final Integer tonemapMode;
    @Field(value = 13, getterName = "getTonemapGamma")
    @Nullable
    private final Float tonemapGamma;
    @Field(value = 14, getterName = "getTonemapRed")
    @Nullable
    private final float[] tonemapRed;
    @Field(value = 15, getterName = "getTonemapGreen")
    @Nullable
    private final float[] tonemapGreen;
    @Field(value = 16, getterName = "getTonemapBlue")
    @Nullable
    private final float[] tonemapBlue;
    @Field(value = 17, getterName = "getLensFocusDistance")
    @Nullable
    private final Float lensFocusDistance;
    @Field(value = 18, getterName = "getLensFocusRangeLower")
    @Nullable
    private final Float lensFocusRangeLower;
    @Field(value = 19, getterName = "getLensFocusRangeUpper")
    @Nullable
    private final Float lensFocusRangeUpper;
    @Field(value = 20, getterName = "getLensAperture")
    private final float lensAperture;
    @Field(value = 21, getterName = "getMeteringRectangles")
    @Nullable
    private final Rect[] meteringRectangles;
    @Field(value = 22, getterName = "getScalerCropRegion")
    @Nullable
    private final Rect scalerCropRegion;

    @Constructor
    public FrameMetadata(@Param(1) long sensorTimestamp, @Param(2) int sensorSensitivity, @Param(3) long sensorExposureTime, @Param(4) int controlPostRawSensitivityBoost, @Nullable @Param(5) Rect[] faceBounds, @Nullable @Param(6) int[] faceScores, @Nullable @Param(11) Integer edgeMode, @Nullable @Param(12) Integer tonemapMode, @Nullable @Param(13) Float tonemapGamma, @Nullable @Param(14) float[] tonemapRed, @Nullable @Param(15) float[] tonemapGreen, @Nullable @Param(16) float[] tonemapBlue, @Nullable @Param(17) Float lensFocusDistance, @Nullable @Param(18) Float lensFocusRangeLower, @Nullable @Param(19) Float lensFocusRangeUpper, @Param(20) float lensAperture, @Nullable @Param(21) Rect[] meteringRectangles, @Nullable @Param(22) Rect scalerCropRegion) {
        this.sensorTimestamp = sensorTimestamp;
        this.sensorSensitivity = sensorSensitivity;
        this.sensorExposureTime = sensorExposureTime;
        this.controlPostRawSensitivityBoost = controlPostRawSensitivityBoost;
        this.faceBounds = faceBounds;
        this.faceScores = faceScores;
        this.edgeMode = edgeMode;
        this.tonemapMode = tonemapMode;
        this.tonemapGamma = tonemapGamma;
        this.tonemapRed = tonemapRed;
        this.tonemapGreen = tonemapGreen;
        this.tonemapBlue = tonemapBlue;
        this.lensFocusDistance = lensFocusDistance;
        this.lensFocusRangeLower = lensFocusRangeLower;
        this.lensFocusRangeUpper = lensFocusRangeUpper;
        this.lensAperture = lensAperture;
        this.meteringRectangles = meteringRectangles;
        this.scalerCropRegion = scalerCropRegion;
    }

    public long getSensorTimestamp() {
        return sensorTimestamp;
    }

    public int getSensorSensitivity() {
        return sensorSensitivity;
    }

    public long getSensorExposureTime() {
        return sensorExposureTime;
    }

    public int getControlPostRawSensitivityBoost() {
        return controlPostRawSensitivityBoost;
    }

    @Nullable
    public Rect[] getFaceBounds() {
        return faceBounds;
    }

    @Nullable
    public int[] getFaceScores() {
        return faceScores;
    }

    @Nullable
    public Integer getEdgeMode() {
        return edgeMode;
    }

    @Nullable
    public Integer getTonemapMode() {
        return tonemapMode;
    }

    @Nullable
    public Float getTonemapGamma() {
        return tonemapGamma;
    }

    @Nullable
    public float[] getTonemapRed() {
        return tonemapRed;
    }

    @Nullable
    public float[] getTonemapGreen() {
        return tonemapGreen;
    }

    @Nullable
    public float[] getTonemapBlue() {
        return tonemapBlue;
    }

    @Nullable
    public Float getLensFocusDistance() {
        return lensFocusDistance;
    }

    @Nullable
    public Float getLensFocusRangeLower() {
        return lensFocusRangeLower;
    }

    @Nullable
    public Float getLensFocusRangeUpper() {
        return lensFocusRangeUpper;
    }

    public float getLensAperture() {
        return lensAperture;
    }

    @Nullable
    public Rect[] getMeteringRectangles() {
        return meteringRectangles;
    }

    @Nullable
    public Rect getScalerCropRegion() {
        return scalerCropRegion;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("FrameMetadata")
                .field("sensorTimestamp", sensorTimestamp)
                .field("sensorSensitivity", sensorSensitivity)
                .field("sensorExposureTime", sensorExposureTime)
                .field("controlPostRawSensitivityBoost", controlPostRawSensitivityBoost)
                .field("faceBounds", faceBounds)
                .field("faceScores", faceScores)
                .field("edgeMode", edgeMode)
                .field("tonemapMode", tonemapMode)
                .field("tonemapGamma", tonemapGamma)
                .field("tonemapRed", tonemapRed)
                .field("tonemapGreen", tonemapGreen)
                .field("tonemapBlue", tonemapBlue)
                .field("lensFocusDistance", lensFocusDistance)
                .field("lensFocusRangeLower", lensFocusRangeLower)
                .field("lensFocusRangeUpper", lensFocusRangeUpper)
                .field("lensAperture", lensAperture)
                .field("meteringRectangles", meteringRectangles)
                .field("scalerCropRegion", scalerCropRegion)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FrameMetadata> CREATOR = findCreator(FrameMetadata.class);
}
