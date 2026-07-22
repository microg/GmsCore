/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class AuthenticationExtensionsPrfOutputs extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "isEnabled")
    private final boolean enabled;

    @Field(value = 2, getterName = "getFirst")
    @Nullable
    private final byte[] first;

    @Field(value = 3, getterName = "getSecond")
    @Nullable
    private final byte[] second;

    @Constructor
    public AuthenticationExtensionsPrfOutputs(@Param(1) boolean enabled, @Param(2) @Nullable byte[] first, @Param(3) @Nullable byte[] second) {
        this.enabled = enabled;
        this.first = first;
        this.second = second;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    public byte[] getFirst() {
        return first;
    }

    @Nullable
    public byte[] getSecond() {
        return second;
    }

    private String b64url(byte[] v) {
        return Base64.encodeToString(v, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticationExtensionsPrfOutputs> CREATOR = findCreator(AuthenticationExtensionsPrfOutputs.class);

    @Override
    public String toString() {
        return ToStringHelper.name("AuthenticationExtensionsPrfOutputs").field("enabled", enabled).field("first", first).field("second", second).end();
    }
}
