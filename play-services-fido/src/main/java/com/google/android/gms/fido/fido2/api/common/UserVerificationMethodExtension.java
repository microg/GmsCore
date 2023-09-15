/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.NonNull;
import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

/**
 * Extension for FIDO User Verification Method.
 * <p>
 * This authentication extension allows Relying Parties to ascertain the method(s) used by the user to authorize the
 * operation.
 * <p>
 * Note that this extension can be used in only sign calls.
 */
@PublicApi
public class UserVerificationMethodExtension extends AutoSafeParcelable {
    @Field(1)
    @NonNull
    private boolean uvm;

    @NonNull
    public boolean getUvm() {
        return uvm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserVerificationMethodExtension)) return false;

        UserVerificationMethodExtension that = (UserVerificationMethodExtension) o;

        return uvm == that.uvm;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{uvm});
    }

    public static final Creator<UserVerificationMethodExtension> CREATOR = new AutoCreator<>(UserVerificationMethodExtension.class);
}
