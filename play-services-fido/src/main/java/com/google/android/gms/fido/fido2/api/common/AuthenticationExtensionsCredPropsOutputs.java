/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Objects;

/**
 * Class that holds the result of the credProps extension.
 * <p>
 * Since this extension only reports information, it is always included in registration responses and does not need to be requested.
 */
@SafeParcelable.Class
public class AuthenticationExtensionsCredPropsOutputs extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getIsDiscoverableCredential")
    private boolean rk;

    @Constructor
    public AuthenticationExtensionsCredPropsOutputs(@Param(1) boolean rk) {
        this.rk = rk;
    }

    public boolean equals(Object other) {
        return (other instanceof AuthenticationExtensionsCredPropsOutputs) && this.rk == ((AuthenticationExtensionsCredPropsOutputs) other).rk;
    }

    /**
     * This value reflects the "rk" flag of the WebAuthn extension.
     */
    public boolean getIsDiscoverableCredential() {
        return rk;
    }

    public int hashCode() {
        return Objects.hashCode(new Object[]{this.rk});
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticationExtensionsCredPropsOutputs> CREATOR = findCreator(AuthenticationExtensionsCredPropsOutputs.class);
}
