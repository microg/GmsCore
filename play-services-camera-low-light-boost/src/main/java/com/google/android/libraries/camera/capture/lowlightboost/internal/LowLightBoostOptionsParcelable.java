/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.libraries.camera.capture.lowlightboost.internal;

import android.os.Parcel;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
@Hide
public class LowLightBoostOptionsParcelable extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getTarget")
    @NonNull
    private final Surface target;
    @Field(value = 2, getterName = "getCameraId")
    @NonNull
    private final String cameraId;
    @Field(value = 3, getterName = "getCaptureWidth")
    private final int captureWidth;
    @Field(value = 4, getterName = "getCaptureHeight")
    private final int captureHeight;
    @Field(value = 5, getterName = "getInitialBoostMode")
    private final int initialBoostMode;

    @Constructor
    public LowLightBoostOptionsParcelable(@NonNull @Param(1) Surface target, @NonNull @Param(2) String cameraId, @Param(3) int captureWidth, @Param(4) int captureHeight, @Param(5) int initialBoostMode) {
        this.target = target;
        this.cameraId = cameraId;
        this.captureWidth = captureWidth;
        this.captureHeight = captureHeight;
        this.initialBoostMode = initialBoostMode;
    }

    @NonNull
    public Surface getTarget() {
        return target;
    }

    @NonNull
    public String getCameraId() {
        return cameraId;
    }

    public int getCaptureWidth() {
        return captureWidth;
    }

    public int getCaptureHeight() {
        return captureHeight;
    }

    public int getInitialBoostMode() {
        return initialBoostMode;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("LowLightBoostOptionsParcelable")
                .field("target", target)
                .field("cameraId", cameraId)
                .field("captureWidth", captureWidth)
                .field("captureHeight", captureHeight)
                .field("initialBoostMode", initialBoostMode)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LowLightBoostOptionsParcelable> CREATOR = findCreator(LowLightBoostOptionsParcelable.class);
}
