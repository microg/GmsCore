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
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;

import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

@PublicApi
@SafeParcelable.Class
public class AuthenticationExtensionsDevicePublicKeyOutputs extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getDevicePublicKey")
    @Nullable
    private final byte[] devicePublicKey;

    @Field(value = 2, getterName = "getSignature")
    @Nullable
    private final byte[] signature;

    @Constructor
    public AuthenticationExtensionsDevicePublicKeyOutputs(@Param(1) byte[] devicePublicKey, @Param(2) byte[] signature) {
        this.devicePublicKey = devicePublicKey;
        this.signature = signature;
    }

    @Nullable
    public byte[] getDevicePublicKey() {
        return devicePublicKey;
    }

    @Nullable
    public byte[] getSignature() {
        return signature;
    }

    @PublicApi
    public static class Builder {
        @Nullable
        private byte[] devicePublicKey;
        @Nullable
        private byte[] signature;

        public Builder() {
        }

        public Builder setDevicePublicKey(@Nullable byte[] devicePublicKey) {
            this.devicePublicKey = devicePublicKey;
            return this;
        }

        public Builder setSignature(@Nullable byte[] signature) {
            this.signature = signature;
            return this;
        }

        public AuthenticationExtensionsDevicePublicKeyOutputs build() {
            return new AuthenticationExtensionsDevicePublicKeyOutputs(devicePublicKey, signature);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @PublicApi
    @NonNull
    public byte[] serializeToBytes() {
        return SafeParcelableSerializer.serializeToBytes(this);
    }

    @PublicApi
    @NonNull
    public static AuthenticationExtensionsDevicePublicKeyOutputs deserializeFromBytes(@NonNull byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthenticationExtensionsDevicePublicKeyOutputs)) return false;
        AuthenticationExtensionsDevicePublicKeyOutputs that = (AuthenticationExtensionsDevicePublicKeyOutputs) o;
        return Arrays.equals(devicePublicKey, that.devicePublicKey) && Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{Arrays.hashCode(devicePublicKey), Arrays.hashCode(signature)});
    }

    @Override
    public String toString() {
        return ToStringHelper.name("AuthenticationExtensionsDevicePublicKeyOutputs").field("devicePublicKey", devicePublicKey != null ? devicePublicKey.length : null).field("signature", signature != null ? signature.length : null).end();
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticationExtensionsDevicePublicKeyOutputs> CREATOR = findCreator(AuthenticationExtensionsDevicePublicKeyOutputs.class);
}
