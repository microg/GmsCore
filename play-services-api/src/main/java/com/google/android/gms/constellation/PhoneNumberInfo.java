/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class PhoneNumberInfo extends AbstractSafeParcelable {
    @Field(1)
    public int version;
    @Field(2)
    public String phoneNumber;
    @Field(3)
    public Long timestamp;
    @Field(4)
    public Bundle extras;

    public PhoneNumberInfo(String phoneNumber, Long timestamp, Bundle extras) {
        this.version = 1;
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
        this.extras = extras;
    }

    @Constructor
    public PhoneNumberInfo(@Param(1) int version, @Param(2) String phoneNumber, @Param(3) Long timestamp, @Param(4) Bundle extras) {
        this.version = version;
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
        this.extras = extras;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PhoneNumberInfo> CREATOR = findCreator(PhoneNumberInfo.class);
}
