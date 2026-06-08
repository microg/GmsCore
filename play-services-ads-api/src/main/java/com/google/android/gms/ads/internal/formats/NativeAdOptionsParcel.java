/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.formats;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.internal.client.VideoOptionsParcel;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Native ad options (AdMob {@code NativeAdOptions}) passed to {@code IAdLoaderBuilder.setNativeAdOptions}.
 */
@SafeParcelable.Class
public class NativeAdOptionsParcel extends AbstractSafeParcelable {
    @Field(1)
    public int versionCode = 6;
    @Field(2)
    public boolean returnUrlsForImageAssets;
    @Field(3)
    public int imageOrientation;
    @Field(4)
    public boolean requestMultipleImages;
    @Field(5)
    public int adChoicesPlacement;
    @Field(6)
    public VideoOptionsParcel videoOptions;
    @Field(7)
    public boolean requestCustomMuteThisAd;
    @Field(8)
    public int mediaAspectRatio;
    @Field(9)
    public int swipeGestureDirection;
    @Field(10)
    public boolean customClickGestureEnabled;
    @Field(11)
    public int customClickGestureDirection;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<NativeAdOptionsParcel> CREATOR = findCreator(NativeAdOptionsParcel.class);
}
