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

@SafeParcelable.Class
public class DetectionOptions extends AbstractSafeParcelable {

    @Field(2)
    public int mode;
    @Field(3)
    public int landmarkType;
    @Field(4)
    public int classificationType;
    @Field(5)
    public boolean prominentFaceOnly;
    @Field(6)
    public boolean trackingEnabled;
    @Field(7)
    public float minFaceSize;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DetectionOptions> CREATOR = findCreator(DetectionOptions.class);
}
