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
import com.google.android.gms.fido.common.Transport;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;
import java.util.List;

/**
 * This class contains the attributes that are specified by a caller when referring to a credential as an input
 * parameter to the registration or authentication method.
 */
@PublicApi
@SafeParcelable.Class
public class PublicKeyCredentialDescriptor extends AbstractSafeParcelable {
    @Field(value = 2, getterName = "getType")
    @NonNull
    private PublicKeyCredentialType type;
    @Field(value = 3, getterName = "getId")
    @NonNull
    private byte[] id;
    @Field(value = 4, getterName = "getTransports")
    @Nullable
    private List<Transport> transports;

    private PublicKeyCredentialDescriptor() {
    }

    public PublicKeyCredentialDescriptor(@NonNull String type, @NonNull byte[] id, @Nullable List<Transport> transports) {
        try {
            this.type = PublicKeyCredentialType.fromString(type);
        } catch (PublicKeyCredentialType.UnsupportedPublicKeyCredTypeException e) {
            throw new IllegalArgumentException(e);
        }
        this.id = id;
        this.transports = transports;
    }

    @Constructor
    PublicKeyCredentialDescriptor(@Param(2) @NonNull PublicKeyCredentialType type, @Param(3) @NonNull byte[] id, @Param(4) @Nullable List<Transport> transports) {
        this.type = type;
        this.id = id;
        this.transports = transports;
    }

    @NonNull
    public byte[] getId() {
        return id;
    }

    @Nullable
    public List<Transport> getTransports() {
        return transports;
    }

    @NonNull
    public PublicKeyCredentialType getType() {
        return type;
    }

    @NonNull
    public String getTypeAsString() {
        return type.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicKeyCredentialDescriptor)) return false;

        PublicKeyCredentialDescriptor that = (PublicKeyCredentialDescriptor) o;

        if (type != that.type) return false;
        if (!Arrays.equals(id, that.id)) return false;
        return transports != null ? transports.equals(that.transports) : that.transports == null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{type, Arrays.hashCode(id), transports});
    }

    @Override
    @NonNull
    public String toString() {
        return ToStringHelper.name("PublicKeyCredentialDescriptor")
                .value(id)
                .field("type", type)
                .field("transports", transports)
                .end();
    }

    /**
     * Exception thrown when an unsupported or unrecognized public key credential descriptor is encountered.
     */
    public static class UnsupportedPubKeyCredDescriptorException extends Exception {
        public UnsupportedPubKeyCredDescriptorException(String message) {
            super(message);
        }

        public UnsupportedPubKeyCredDescriptorException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PublicKeyCredentialDescriptor> CREATOR = findCreator(PublicKeyCredentialDescriptor.class);
}
