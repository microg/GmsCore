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
public class UserPresence extends AbstractSafeParcelable {
    @Field(value = 2)
    public boolean lockScreenSecure;
    @Field(value = 3)
    public boolean shownLockScreen;
    @Field(value = 4)
    public int lockScreenQuality;
    @Field(value = 5)
    public long lastUnlockDurationInSeconds;
    @Field(value = 6)
    public long lockScreenSetupDurationInSeconds;

    public UserPresence() {
    }

    @Constructor
    public UserPresence(@Param(2) boolean lockScreenSecure,
                        @Param(3) boolean shownLockScreen,
                        @Param(4) int lockScreenQuality,
                        @Param(5) long lastUnlockDurationInSeconds,
                        @Param(6) long lockScreenSetupDurationInSeconds) {
        this.lockScreenSecure = lockScreenSecure;
        this.shownLockScreen = shownLockScreen;
        this.lockScreenQuality = lockScreenQuality;
        this.lastUnlockDurationInSeconds = lastUnlockDurationInSeconds;
        this.lockScreenSetupDurationInSeconds = lockScreenSetupDurationInSeconds;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<UserPresence> CREATOR = findCreator(UserPresence.class);
}
