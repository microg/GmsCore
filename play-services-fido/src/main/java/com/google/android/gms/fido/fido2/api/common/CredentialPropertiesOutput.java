/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.PublicApi;

import java.util.Arrays;

@PublicApi
@SafeParcelable.Class
public class CredentialPropertiesOutput extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "isResidentKey")
    private final boolean residentKey;

    @Constructor
    public CredentialPropertiesOutput(@Param(1) boolean residentKey) {
        this.residentKey = residentKey;
    }

    public boolean isResidentKey() {
        return residentKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CredentialPropertiesOutput)) return false;
        CredentialPropertiesOutput that = (CredentialPropertiesOutput) o;
        return residentKey == that.residentKey;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{residentKey});
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CredentialPropertiesOutput> CREATOR = findCreator(CredentialPropertiesOutput.class);
}
