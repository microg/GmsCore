/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.PublicApi;

import java.util.Arrays;

/**
 * This container class represents client output for extensions that can be passed into FIDO2 APIs.
 */
@PublicApi
@SafeParcelable.Class
public class AuthenticationExtensionsClientOutputs extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getUvmEntries")
    @Nullable
    private UvmEntries uvmEntries;

    @Constructor
    AuthenticationExtensionsClientOutputs(@Param(1)@Nullable UvmEntries uvmEntries) {
        this.uvmEntries = uvmEntries;
    }

    @Nullable
    public UvmEntries getUvmEntries() {
        return uvmEntries;
    }

    /**
     * Serializes the {@link AuthenticationExtensionsClientOutputs} to bytes.
     * Use {@link #deserializeFromBytes(byte[])} to deserialize.
     */
    @NonNull
    public byte[] serializeToBytes() {
        return SafeParcelableSerializer.serializeToBytes(this);
    }

    /**
     * De-serializes the {@link AuthenticationExtensionsClientOutputs} from bytes, reversing {@link #serializeToBytes()}.
     *
     * @return The deserialized {@link AuthenticationExtensionsClientOutputs}
     */
    @NonNull
    public static AuthenticationExtensionsClientOutputs deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticationExtensionsClientOutputs)) return false;

        AuthenticationExtensionsClientOutputs that = (AuthenticationExtensionsClientOutputs) o;

        return uvmEntries != null ? uvmEntries.equals(that.uvmEntries) : that.uvmEntries == null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{uvmEntries});
    }

    /**
     * Builder for {@link AuthenticationExtensionsClientOutputs}.
     */
    public static class Builder {
        @Nullable
        private UvmEntries uvmEntries;

        /**
         * The constructor of {@link AuthenticationExtensionsClientOutputs.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets the User Verification Method extension, which allows the relying party to ascertain up to three
         * authentication methods that were used.
         */
        public Builder setUserVerificationMethodEntries(@Nullable UvmEntries uvmEntries) {
            this.uvmEntries = uvmEntries;
            return this;
        }

        /**
         * Builds the {@link AuthenticationExtensionsClientOutputs} object.
         */
        @NonNull
        public AuthenticationExtensionsClientOutputs build() {
            return new AuthenticationExtensionsClientOutputs(uvmEntries);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticationExtensionsClientOutputs> CREATOR = findCreator(AuthenticationExtensionsClientOutputs.class);
}
