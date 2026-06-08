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
 * Ad value (AdMob {@code AdValue}) delivered to {@code IOnPaidEventListener.onPaidEvent}.
 */
@SafeParcelable.Class
public class AdValueParcel extends AbstractSafeParcelable {
    @Field(1)
    public int adType;
    @Field(2)
    public int precisionType;
    @Field(3)
    public String currencyCode;
    @Field(4)
    public long valueMicros;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AdValueParcel> CREATOR = findCreator(AdValueParcel.class);
}
