/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.setup.accounts;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class ScreenlockState extends AbstractSafeParcelable {
    @Field(value = 2)
    public boolean isSecureScreenlock;
    @Field(value = 3)
    public int screenlockType;
    @Field(value = 4)
    public long screenlockSettingsAgeMillis;
    @Field(value = 5)
    public long elapsedTimeSinceUnlockMillis;
    @Field(value = 6)
    public boolean isForcedScreenlock;

    public ScreenlockState() {
    }

    public ScreenlockState(boolean isSecureScreenlock, int screenlockType,
                           long screenlockSettingsAgeMillis, long elapsedTimeSinceUnlockMillis) {
        this(isSecureScreenlock, screenlockType, screenlockSettingsAgeMillis,
                elapsedTimeSinceUnlockMillis, false);
    }

    @Constructor
    public ScreenlockState(@Param(2) boolean isSecureScreenlock,
                           @Param(3) int screenlockType,
                           @Param(4) long screenlockSettingsAgeMillis,
                           @Param(5) long elapsedTimeSinceUnlockMillis,
                           @Param(6) boolean isForcedScreenlock) {
        this.isSecureScreenlock = isSecureScreenlock;
        this.screenlockType = screenlockType;
        this.screenlockSettingsAgeMillis = screenlockSettingsAgeMillis;
        this.elapsedTimeSinceUnlockMillis = elapsedTimeSinceUnlockMillis;
        this.isForcedScreenlock = isForcedScreenlock;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ScreenlockState> CREATOR =
            findCreator(ScreenlockState.class);
}
