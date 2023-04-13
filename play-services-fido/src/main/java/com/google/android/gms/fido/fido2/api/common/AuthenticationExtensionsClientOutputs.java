/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParcelUtil;

import java.util.Arrays;

/**
 * This container class represents client output for extensions that can be passed into FIDO2 APIs.
 */
@PublicApi
public class AuthenticationExtensionsClientOutputs extends AutoSafeParcelable {
    @Field(1)
    private UvmEntries uvmEntries;

    public UvmEntries getUvmEntries() {
        return uvmEntries;
    }

    /**
     * Serializes the {@link AuthenticationExtensionsClientOutputs} to bytes.
     * Use {@link #deserializeFromBytes(byte[])} to deserialize.
     */
    public byte[] serializeToBytes() {
        return SafeParcelUtil.asByteArray(this);
    }

    /**
     * De-serializes the {@link AuthenticationExtensionsClientOutputs} from bytes, reversing {@link #serializeToBytes()}.
     *
     * @return The deserialized {@link AuthenticationExtensionsClientOutputs}
     */
    public static AuthenticationExtensionsClientOutputs deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelUtil.fromByteArray(serializedBytes, CREATOR);
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
        public Builder setUserVerificationMethodEntries(UvmEntries uvmEntries) {
            this.uvmEntries = uvmEntries;
            return this;
        }

        /**
         * Builds the {@link AuthenticationExtensionsClientOutputs} object.
         */
        public AuthenticationExtensionsClientOutputs build() {
            AuthenticationExtensionsClientOutputs extensions = new AuthenticationExtensionsClientOutputs();
            extensions.uvmEntries = uvmEntries;
            return extensions;
        }
    }

    public static final Creator<AuthenticationExtensionsClientOutputs> CREATOR = new AutoCreator<>(AuthenticationExtensionsClientOutputs.class);
}
