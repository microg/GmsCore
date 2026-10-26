/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class AuthenticationExtensionsPrfValues extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getFirst")
    @Nullable
    private final byte[] first;

    @Field(value = 2, getterName = "getSecond")
    @Nullable
    private final byte[] second;

    @Constructor
    public AuthenticationExtensionsPrfValues(@Param(1) @Nullable byte[] first, @Param(2) @Nullable byte[] second) {
        this.first = first;
        this.second = second;
    }

    @Nullable
    public byte[] getFirst() {
        return first;
    }

    @Nullable
    public byte[] getSecond() {
        return second;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticationExtensionsPrfValues> CREATOR = findCreator(AuthenticationExtensionsPrfValues.class);

    @Override
    public String toString() {
        return ToStringHelper.name("AuthenticationExtensionsPrfValues").field("first", first).field("second", second).end();
    }
}
