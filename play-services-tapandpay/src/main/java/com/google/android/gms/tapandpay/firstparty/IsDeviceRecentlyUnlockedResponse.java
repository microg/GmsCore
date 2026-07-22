/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class IsDeviceRecentlyUnlockedResponse extends AbstractSafeParcelable {
    @Field(1)
    public final boolean isDeviceRecentlyUnlocked;

    @Constructor
    public IsDeviceRecentlyUnlockedResponse(@Param(1) boolean isDeviceRecentlyUnlocked) {
        this.isDeviceRecentlyUnlocked = isDeviceRecentlyUnlocked;
    }

    public static final SafeParcelableCreatorAndWriter<IsDeviceRecentlyUnlockedResponse> CREATOR = findCreator(IsDeviceRecentlyUnlockedResponse.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
