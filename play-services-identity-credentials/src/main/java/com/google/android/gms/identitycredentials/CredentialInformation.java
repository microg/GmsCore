/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.identitycredentials;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class CredentialInformation extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getPackageName")
    @Nullable
    private final String packageName;
    @Field(value = 2, getterName = "getNumPasswordCredentials")
    private final int numPasswordCredentials;
    @Field(value = 3, getterName = "getNumPasskeyCredentials")
    private final int numPasskeyCredentials;
    @Field(value = 4, getterName = "getNumGoogleIdCredentials")
    private final int numGoogleIdCredentials;
    @Field(value = 5, getterName = "getNumCustomCredentials")
    private final int numCustomCredentials;

    @Constructor
    public CredentialInformation(@Param(1) @Nullable String packageName, @Param(2) int numPasswordCredentials, @Param(3) int numPasskeyCredentials, @Param(4) int numGoogleIdCredentials, @Param(5) int numCustomCredentials) {
        this.packageName = packageName;
        this.numPasswordCredentials = numPasswordCredentials;
        this.numPasskeyCredentials = numPasskeyCredentials;
        this.numGoogleIdCredentials = numGoogleIdCredentials;
        this.numCustomCredentials = numCustomCredentials;
    }

    @Nullable
    public String getPackageName() {
        return packageName;
    }

    public int getNumPasswordCredentials() {
        return numPasswordCredentials;
    }

    public int getNumPasskeyCredentials() {
        return numPasskeyCredentials;
    }

    public int getNumGoogleIdCredentials() {
        return numGoogleIdCredentials;
    }

    public int getNumCustomCredentials() {
        return numCustomCredentials;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CredentialInformation> CREATOR = findCreator(CredentialInformation.class);
}
