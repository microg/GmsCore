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
public class AuthenticationExtensionsPrfOutputs extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "isEnabled")
    private final boolean enabled;

    @Field(value = 2, getterName = "getResults")
    @Nullable
    private final AuthenticationExtensionsPrfValues results;

    @Constructor
    public AuthenticationExtensionsPrfOutputs(@Param(1) boolean enabled, @Param(2) @Nullable AuthenticationExtensionsPrfValues results) {
        this.enabled = enabled;
        this.results = results;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    public AuthenticationExtensionsPrfValues getResults() {
        return results;
    }

    @Nullable
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticationExtensionsPrfOutputs> CREATOR = findCreator(AuthenticationExtensionsPrfOutputs.class);

    @Override
    public String toString() {
        return ToStringHelper.name("AuthenticationExtensionsPrfOutputs").field("enabled", enabled).field("results", results).end();
    }
}
