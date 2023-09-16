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

/**
 * An enum describing the relying party's preference for attestation conveyance.
 */
public enum AttestationConveyancePreference implements Parcelable {
    NONE("none"),
    INDIRECT("indirect"),
    DIRECT("direct");

    private final String value;

    AttestationConveyancePreference(String value) {
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
    @NonNull
    public static AttestationConveyancePreference fromString(String attachment) throws UnsupportedAttestationConveyancePreferenceException {
        for (AttestationConveyancePreference value : values()) {
            if (value.value.equals(attachment)) return value;
        }
        throw new UnsupportedAttestationConveyancePreferenceException("Attestation conveyance preference " + attachment + " not supported");
    }

    public static Creator<AttestationConveyancePreference> CREATOR = new Creator<AttestationConveyancePreference>() {
        @Override
        public AttestationConveyancePreference createFromParcel(Parcel source) {
            try {
                return AttestationConveyancePreference.fromString(source.readString());
            } catch (UnsupportedAttestationConveyancePreferenceException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public AttestationConveyancePreference[] newArray(int size) {
            return new AttestationConveyancePreference[size];
        }
    };

    /**
     * Exception thrown when an unsupported or unrecognized attestation conveyance preference is encountered.
     */
    public static class UnsupportedAttestationConveyancePreferenceException extends Exception {
        public UnsupportedAttestationConveyancePreferenceException(String message) {
            super(message);
        }
    }
}
