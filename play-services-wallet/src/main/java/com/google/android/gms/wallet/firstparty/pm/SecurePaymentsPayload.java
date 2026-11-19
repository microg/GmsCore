/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet.firstparty.pm;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Arrays;

@SafeParcelable.Class
public class SecurePaymentsPayload extends AbstractSafeParcelable {
    @Field(2)
    public final byte[] securePayload;
    @Field(3)
    public final SecurePaymentsData[] securePayments;

    @Constructor
    public SecurePaymentsPayload(@Param(2) byte[] securePayload, @Param(3) SecurePaymentsData[] securePayments) {
        this.securePayload = securePayload;
        this.securePayments = securePayments;
    }

    @Override
    public String toString() {
        return "SecurePaymentsPayload{" +
                "payload=" + Arrays.toString(securePayload) +
                ", securePayments=" + Arrays.toString(securePayments) +
                '}';
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SecurePaymentsPayload> CREATOR = findCreator(SecurePaymentsPayload.class);
}
