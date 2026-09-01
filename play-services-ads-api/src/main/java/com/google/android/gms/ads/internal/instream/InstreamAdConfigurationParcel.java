/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.instream;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Instream ad configuration passed to {@code IAdLoaderBuilder.forInstreamAd}.
 */
@SafeParcelable.Class
public class InstreamAdConfigurationParcel extends AbstractSafeParcelable {
    @Field(1000)
    public int versionCode;
    @Field(1)
    public int instreamAdType;
    @Field(2)
    public String adTagUrl;
    @Field(3)
    public int timeoutMillis;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<InstreamAdConfigurationParcel> CREATOR = findCreator(InstreamAdConfigurationParcel.class);
}
