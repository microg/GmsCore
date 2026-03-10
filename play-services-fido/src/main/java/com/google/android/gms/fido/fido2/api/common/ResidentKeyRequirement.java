/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

/**
 * An enum that describes the Resident Key (Discoverable Credential) requirements.
 * <p>
 * According to WebAuthn, this structure describes the Relying Party's requirements for client-side discoverable credentials
 * (formerly known as resident credentials or resident keys):
 * <p>
 * If the resident key requirement is set to "required", then the Relying Party requires a client-side discoverable credential
 * and is prepared to receive an error if it can't be created. If the resident key requirement is set to "preferred", the Relying
 * party strongly prefers a client-side discoverable credential but will accept a server-side credential. If the resident key
 * requirement is set to "discouraged" then a server-side credential is preferable, but will accept a client-side discoverable
 * credential.
 */
public enum ResidentKeyRequirement implements Parcelable {
    RESIDENT_KEY_DISCOURAGED("discouraged"),
    RESIDENT_KEY_PREFERRED("preferred"),
    RESIDENT_KEY_REQUIRED("required");

    private final String requirement;

    ResidentKeyRequirement(String requirement) {
        this.requirement = requirement;
    }

    @NonNull
    public static ResidentKeyRequirement fromString(@NonNull String requirement) throws UnsupportedResidentKeyRequirementException {
        for (ResidentKeyRequirement value : values()) {
            if (requirement.equals(value.requirement)) return value;
        }
        throw new UnsupportedResidentKeyRequirementException(requirement);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return requirement;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(requirement);
    }

    public static Creator<ResidentKeyRequirement> CREATOR = new Creator<ResidentKeyRequirement>() {
        @Override
        public ResidentKeyRequirement createFromParcel(Parcel source) {
            try {
                return ResidentKeyRequirement.fromString(source.readString());
            } catch (UnsupportedResidentKeyRequirementException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ResidentKeyRequirement[] newArray(int size) {
            return new ResidentKeyRequirement[size];
        }
    };

    /**
     * Exception thrown when an unsupported or unrecognized resident key requirement is encountered.
     */
    public static class UnsupportedResidentKeyRequirementException extends Exception {
        public UnsupportedResidentKeyRequirementException(@NonNull String requirement) {
            super("Resident key requirement " + requirement + " not supported");
        }
    }
}
