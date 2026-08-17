/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

/**
 * This enumeration defines the valid credential types.
 */
public enum PublicKeyCredentialType implements Parcelable {
    PUBLIC_KEY("public-key");

    private final String value;

    PublicKeyCredentialType(String value) {
        this.value = value;
    }

    @Override
    @NonNull
    public String toString() {
        return value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(toString());
    }

    @Hide
    public static PublicKeyCredentialType fromString(String type) throws UnsupportedPublicKeyCredTypeException {
        for (PublicKeyCredentialType value : values()) {
            if (value.value.equals(type)) return value;
        }
        throw new UnsupportedPublicKeyCredTypeException("PublicKeyCredentialType " + type + " not supported");
    }

    public static Creator<PublicKeyCredentialType> CREATOR = new Creator<PublicKeyCredentialType>() {
        @Override
        public PublicKeyCredentialType createFromParcel(Parcel source) {
            try {
                return PublicKeyCredentialType.fromString(source.readString());
            } catch (UnsupportedPublicKeyCredTypeException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public PublicKeyCredentialType[] newArray(int size) {
            return new PublicKeyCredentialType[size];
        }
    };

    /**
     * Exception thrown when an unsupported or unrecognized transport is encountered.
     */
    public static class UnsupportedPublicKeyCredTypeException extends Exception {
        public UnsupportedPublicKeyCredTypeException(String message) {
            super(message);
        }
    }
}
