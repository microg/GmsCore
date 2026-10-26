/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class PhoneNumberVerificationRequest extends AbstractSafeParcelable {
    @Field(1)
    public String phoneNumber;

    @Field(2)
    public String simOperator;

    @Field(3)
    public int verificationMethod;

    @Field(4)
    public byte[] verificationToken;

    public PhoneNumberVerificationRequest() {
    }

    public PhoneNumberVerificationRequest(String phoneNumber, String simOperator, int verificationMethod, byte[] verificationToken) {
        this.phoneNumber = phoneNumber;
        this.simOperator = simOperator;
        this.verificationMethod = verificationMethod;
        this.verificationToken = verificationToken;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("PhoneNumberVerificationRequest")
                .field("phoneNumber", phoneNumber)
                .field("simOperator", simOperator)
                .field("verificationMethod", verificationMethod)
                .field("verificationToken", verificationToken)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PhoneNumberVerificationRequest> CREATOR = findCreator(PhoneNumberVerificationRequest.class);
}
