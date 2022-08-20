/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import android.os.Parcelable;

import org.microg.gms.common.PublicApi;

public enum UserVerificationRequirement implements Parcelable {
    REQUIRED("required"),
    PREFERRED("preferred"),
    DISCOURAGED("discouraged");

    private final String value;

    UserVerificationRequirement(String value) {
        this.value = value;
    }

    @Override
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

    @PublicApi(exclude = true)
    public static UserVerificationRequirement fromString(String attachment) throws UnsupportedUserVerificationRequirementException {
        for (UserVerificationRequirement value : values()) {
            if (value.value.equals(attachment)) return value;
        }
        throw new UnsupportedUserVerificationRequirementException("User verification requirement " + attachment + " not supported");
    }

    public static Creator<UserVerificationRequirement> CREATOR = new Creator<UserVerificationRequirement>() {
        @Override
        public UserVerificationRequirement createFromParcel(Parcel source) {
            try {
                return UserVerificationRequirement.fromString(source.readString());
            } catch (UnsupportedUserVerificationRequirementException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public UserVerificationRequirement[] newArray(int size) {
            return new UserVerificationRequirement[size];
        }
    };

    public static class UnsupportedUserVerificationRequirementException extends Exception {
        public UnsupportedUserVerificationRequirementException(String message) {
            super(message);
        }
    }
}
