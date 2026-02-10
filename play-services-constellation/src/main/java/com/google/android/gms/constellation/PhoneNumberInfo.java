/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class PhoneNumberInfo extends AbstractSafeParcelable {

    @Field(1)
    @Nullable
    public String phoneNumber;

    @Field(2)
    public long timestamp;

    @Field(3)
    public int verificationMethod;

    private PhoneNumberInfo() {
    }

    @Constructor
    public PhoneNumberInfo(
            @Param(1) @Nullable String phoneNumber,
            @Param(2) long timestamp,
            @Param(3) int verificationMethod) {
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
        this.verificationMethod = verificationMethod;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PhoneNumberInfo> CREATOR = findCreator(PhoneNumberInfo.class);
}
