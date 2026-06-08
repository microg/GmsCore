/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Ad size (AdMob {@code AdSize}) passed to {@code IAdLoaderBuilder.forPublisherAdView} and used by the
 * banner ad manager.
 */
@SafeParcelable.Class
public class AdSizeParcel extends AbstractSafeParcelable {
    @Field(2)
    public String formatString;
    @Field(3)
    public int height;
    @Field(4)
    public int heightPixels;
    @Field(5)
    public boolean autoHeight;
    @Field(6)
    public int width;
    @Field(7)
    public int widthPixels;
    @Field(8)
    public AdSizeParcel[] shortcuts;
    @Field(9)
    public boolean isFluid;
    @Field(10)
    public boolean isAutoHeightSmartBanner;
    @Field(11)
    public boolean isDynamicBanner;
    @Field(12)
    public boolean isFullWidth;
    @Field(13)
    public boolean isAutoMediation;
    @Field(14)
    public boolean isParcelStateValid;
    @Field(15)
    public boolean isAdaptiveBanner;
    @Field(16)
    public boolean isInlineAdaptiveBanner;
    @Field(17)
    public boolean isCollapsible;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AdSizeParcel> CREATOR = findCreator(AdSizeParcel.class);
}
