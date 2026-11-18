/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.internal.AdRequestParcel;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public final class PreloadConfigurationParcel extends AbstractSafeParcelable {
    @Field(1)
    public String adUnitId;
    @Field(2)
    public int adFormat;
    @Field(3)
    public AdRequestParcel adRequest;
    @Field(4)
    public int bufferSize;
    @Field(5)
    public boolean usesDefaultBufferSize;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PreloadConfigurationParcel> CREATOR = findCreator(PreloadConfigurationParcel.class);
}
