/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.formats;

import android.os.IBinder;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Ad Manager publisher ad view options passed to {@code IAdLoaderBuilder.setPublisherAdViewOptions}.
 */
@Deprecated
@SafeParcelable.Class
public final class PublisherAdViewOptions extends AbstractSafeParcelable {
    @Field(1)
    public boolean manualImpressionsEnabled;
    @Field(2)
    public IBinder appEventListener;
    @Field(3)
    public IBinder delayedBannerAdListener;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PublisherAdViewOptions> CREATOR = findCreator(PublisherAdViewOptions.class);
}
