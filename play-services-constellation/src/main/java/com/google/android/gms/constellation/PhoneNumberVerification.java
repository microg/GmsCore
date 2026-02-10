/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class PhoneNumberVerification extends AbstractSafeParcelable {

    @Field(1)
    @Nullable
    public String phoneNumber;

    @Field(2)
    public long timestampMillis;

    @Field(3)
    public int verificationMethod;

    @Field(4)
    public int unknownInt;

    @Field(5)
    @Nullable
    public String msisdnToken;

    @Field(6)
    @Nullable
    public Bundle extras;

    @Field(7)
    public int verificationStatus;

    @Field(8)
    public long retryAfterSeconds;

    private PhoneNumberVerification() {
    }

    @Constructor
    public PhoneNumberVerification(
            @Param(1) @Nullable String phoneNumber,
            @Param(2) long timestampMillis,
            @Param(3) int verificationMethod,
            @Param(4) int unknownInt,
            @Param(5) @Nullable String msisdnToken,
            @Param(6) @Nullable Bundle extras,
            @Param(7) int verificationStatus,
            @Param(8) long retryAfterSeconds) {
        this.phoneNumber = phoneNumber;
        this.timestampMillis = timestampMillis;
        this.verificationMethod = verificationMethod;
        this.unknownInt = unknownInt;
        this.msisdnToken = msisdnToken;
        this.extras = extras;
        this.verificationStatus = verificationStatus;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PhoneNumberVerification> CREATOR = findCreator(PhoneNumberVerification.class);
}
