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
    @Field(1)
    public Surface target;
    @Field(2)
    public String cameraId;
    @Field(3)
    public int captureWidth;
    @Field(4)
    public int captureHeight;
    @Field(5)
    public int enableLowLightBoost;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("LowLightBoostOptionsParcelable")
                .field("target", target)
                .field("cameraId", cameraId)
                .field("captureWidth", captureWidth)
                .field("captureHeight", captureHeight)
                .field("enableLowLightBoost", enableLowLightBoost)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LowLightBoostOptionsParcelable> CREATOR = findCreator(LowLightBoostOptionsParcelable.class);
}
