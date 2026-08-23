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
public class VerificationResult extends AbstractSafeParcelable {
    @Field(1)
    public int statusCode;

    @Field(2)
    public String phoneNumber;

    @Field(3)
    public byte[] responseToken;

    @Field(4)
    public long validUntilTimestamp;

    public VerificationResult() {
    }

    public VerificationResult(int statusCode, String phoneNumber, byte[] responseToken, long validUntilTimestamp) {
        this.statusCode = statusCode;
        this.phoneNumber = phoneNumber;
        this.responseToken = responseToken;
        this.validUntilTimestamp = validUntilTimestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("VerificationResult")
                .field("statusCode", statusCode)
                .field("phoneNumber", phoneNumber)
                .field("responseToken", responseToken)
                .field("validUntilTimestamp", validUntilTimestamp)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<VerificationResult> CREATOR = findCreator(VerificationResult.class);
}
