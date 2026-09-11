/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.identitycredentials;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * The state of the primary provider's credentials
 */
@SafeParcelable.Class
public class CredentialTransferCapabilities extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getResponseBundle")
    private final Bundle responseBundle;

    @Constructor
    public CredentialTransferCapabilities(@Param(1) Bundle responseBundle) {
        this.responseBundle = responseBundle;
    }

    /**
     * Returns the number of requested credentials.
     */
    @Nullable
    public final Integer getNumCustomCredentials(@NonNull String key) {
        if (this.responseBundle.containsKey(key)) {
            return this.responseBundle.getInt(key);
        }
        return null;
    }

    /**
     * Returns the number of passkeys.
     */
    @Nullable
    public final Integer getNumPasskeys() {
        if (this.responseBundle.containsKey("NUM_PASSKEYS")) {
            return this.responseBundle.getInt("NUM_PASSKEYS");
        }
        return null;
    }

    /**
     * Returns the number of passwords.
     */
    @Nullable
    public final Integer getNumPasswords() {
        if (this.responseBundle.containsKey("NUM_PASSWORDS")) {
            return this.responseBundle.getInt("NUM_PASSWORDS");
        }
        return null;
    }

    @Nullable
    public final CallingAppInfoParcelable getProviderAppInfo() {
        return (CallingAppInfoParcelable) this.responseBundle.getParcelable("PROVIDER_APP_INFO");
    }

    /**
     * the bundle containing the credential transfer capabilities.
     */
    public Bundle getResponseBundle() {
        return responseBundle;
    }

    /**
     * Returns the total number of credentials.
     */
    @Nullable
    public final Integer getTotalNumCredentials() {
        if (this.responseBundle.containsKey("TOTAL_NUM_CREDENTIALS")) {
            return this.responseBundle.getInt("TOTAL_NUM_CREDENTIALS");
        }
        return null;
    }

    /**
     * Returns the total size of credentials in bytes.
     */
    @Nullable
    public final Long getTotalSizeBytes() {
        if (this.responseBundle.containsKey("TOTAL_SIZE_BYTES")) {
            return this.responseBundle.getLong("TOTAL_SIZE_BYTES");
        }
        return null;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CredentialTransferCapabilities> CREATOR = findCreator(CredentialTransferCapabilities.class);
}
