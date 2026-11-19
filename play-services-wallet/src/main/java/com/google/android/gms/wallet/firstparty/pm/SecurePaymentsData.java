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

@SafeParcelable.Class
public class SecurePaymentsData extends AbstractSafeParcelable {
    @Field(2)
    public final int key;
    @Field(3)
    public final String value;

    @Constructor
    public SecurePaymentsData(@Param(2) int key, @Param(3) String value) {
        this.key = key;
        this.value = value;
        if (key <= 0) {
            throw new IllegalArgumentException("SecurePaymentsData.key must be > 0");
        }
        if (value == null) {
            throw new IllegalArgumentException("SecurePaymentsData.value must not be null");
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SecurePaymentsData> CREATOR = findCreator(SecurePaymentsData.class);
}
