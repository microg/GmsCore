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
 * Video options (AdMob {@code VideoOptions}) nested inside {@link NativeAdOptionsParcel}.
 */
@SafeParcelable.Class
public class VideoOptionsParcel extends AbstractSafeParcelable {
    @Field(2)
    public boolean startMuted;
    @Field(3)
    public boolean customControlsRequested;
    @Field(4)
    public boolean clickToExpandRequested;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<VideoOptionsParcel> CREATOR = findCreator(VideoOptionsParcel.class);
}
