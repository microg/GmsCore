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
import java.util.Objects;

/**
 * This container class represents client output for extensions that can be passed into FIDO2 APIs.
 */
@PublicApi
@SafeParcelable.Class
public class AuthenticationExtensionsClientOutputs extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getUvmEntries")
    @Nullable
    private UvmEntries uvmEntries;

    @Field(value = 2, getterName = "getDevicePublicKeyOutputs")
    @Nullable
    private AuthenticationExtensionsDevicePublicKeyOutputs devicePublicKeyOutputs;

    @Field(value = 3, getterName = "getCredProps")
    @Nullable
    private AuthenticationExtensionsCredPropsOutputs credProps;

    @Field(value = 4, getterName = "getPrfOutputs")
    @Nullable
    private AuthenticationExtensionsPrfOutputs prfOutputs;

    @Field(value = 5, getterName = "getTxAuthSimple")
    @Nullable
    private String txAuthSimple;

    @Constructor
    public AuthenticationExtensionsClientOutputs(@Param(1) @Nullable UvmEntries uvmEntries, @Param(2) @Nullable AuthenticationExtensionsDevicePublicKeyOutputs devicePublicKeyOutputs, @Param(3) @Nullable AuthenticationExtensionsCredPropsOutputs credProps, @Param(4) @Nullable AuthenticationExtensionsPrfOutputs prfOutputs, @Param(5) @Nullable String txAuthSimple) {
        this.uvmEntries = uvmEntries;
        this.devicePublicKeyOutputs = devicePublicKeyOutputs;
        this.credProps = credProps;
        this.prfOutputs = prfOutputs;
        this.txAuthSimple = txAuthSimple;
    }

    @Nullable
    public UvmEntries getUvmEntries() {
        return uvmEntries;
    }

    @Nullable
    public AuthenticationExtensionsDevicePublicKeyOutputs getDevicePublicKeyOutputs() {
        return devicePublicKeyOutputs;
    }

    @Nullable
    public AuthenticationExtensionsCredPropsOutputs getCredProps() {
        return credProps;
    }

    @Nullable
    public AuthenticationExtensionsPrfOutputs getPrfOutputs() {
        return prfOutputs;
    }

    @Nullable
    public String getTxAuthSimple() {
        return txAuthSimple;
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
        return (Objects.equals(uvmEntries, that.uvmEntries)) && (Objects.equals(devicePublicKeyOutputs, that.devicePublicKeyOutputs)) && (Objects.equals(credProps, that.credProps)) && (Objects.equals(prfOutputs, that.prfOutputs)) && (Objects.equals(txAuthSimple, that.txAuthSimple));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{uvmEntries, devicePublicKeyOutputs, credProps, prfOutputs, txAuthSimple});
    }

    /**
     * Builder for {@link AuthenticationExtensionsClientOutputs}.
     */
    public static class Builder {
        @Nullable
        private UvmEntries uvmEntries;
        @Nullable
        private AuthenticationExtensionsDevicePublicKeyOutputs devicePublicKeyOutputs;
        @Nullable
        private AuthenticationExtensionsCredPropsOutputs credProps;
        @Nullable
        private AuthenticationExtensionsPrfOutputs prfOutputs;
        @Nullable
        private String txAuthSimple;

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
         * Set Device Public Key client outputs
         */
        public Builder setDevicePublicKeyOutputs(@Nullable AuthenticationExtensionsDevicePublicKeyOutputs dpkOutputs) {
            this.devicePublicKeyOutputs = dpkOutputs;
            return this;
        }

        /**
         * Set Credential Properties client outputs (e.g., rk=true)
         */
        public Builder setCredProps(@Nullable AuthenticationExtensionsCredPropsOutputs credProps) {
            this.credProps = credProps;
            return this;
        }

        /**
         * Set PRF client outputs
         */
        public Builder setPrfOutputs(@Nullable AuthenticationExtensionsPrfOutputs prfOutputs) {
            this.prfOutputs = prfOutputs;
            return this;
        }

        /**
         * Set txAuthSimple string
         */
        public Builder setTxAuthSimple(@Nullable String txAuthSimple) {
            this.txAuthSimple = txAuthSimple;
            return this;
        }

        /**
         * Builds the {@link AuthenticationExtensionsClientOutputs} object.
         */
        public AuthenticationExtensionsClientOutputs build() {
            return new AuthenticationExtensionsClientOutputs(uvmEntries, devicePublicKeyOutputs, credProps, prfOutputs, txAuthSimple);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticationExtensionsClientOutputs> CREATOR = findCreator(AuthenticationExtensionsClientOutputs.class);
}
