/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.libraries.camera.capture.lowlightboost.internal;

import android.graphics.Rect;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

/**
 * Subset of a camera2 {@code TotalCaptureResult}, forwarded by the client for each frame while a
 * low light boost session is active.
 */
@SafeParcelable.Class
@Hide
public class CaptureResultParcelable extends AbstractSafeParcelable {
    @Field(1)
    public long sensorTimestamp;
    @Field(2)
    public int sensorSensitivity;
    @Field(3)
    public long sensorExposureTime;
    @Field(4)
    public int postRawSensitivityBoost;
    @Field(5)
    public Rect[] faceRects;
    @Field(6)
    public int[] faceScores;
    @Field(11)
    public Integer edgeMode;
    @Field(12)
    public Integer tonemapMode;
    @Field(13)
    public Float tonemapGamma;
    @Field(14)
    public float[] tonemapCurveRed;
    @Field(15)
    public float[] tonemapCurveGreen;
    @Field(16)
    public float[] tonemapCurveBlue;
    @Field(17)
    public Float focusDistance;
    @Field(18)
    public Float focusRangeNear;
    @Field(19)
    public Float focusRangeFar;
    @Field(20)
    public float aperture;
    @Field(21)
    public Rect[] aeRegions;
    @Field(22)
    public Rect scalerCropRegion;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("CaptureResultParcelable")
                .field("sensorTimestamp", sensorTimestamp)
                .field("sensorSensitivity", sensorSensitivity)
                .field("sensorExposureTime", sensorExposureTime)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CaptureResultParcelable> CREATOR = findCreator(CaptureResultParcelable.class);
}
