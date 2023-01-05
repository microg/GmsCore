/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import android.os.Parcelable;

import org.microg.gms.common.PublicApi;

/**
 * Clients may communicate with authenticators using a variety of mechanisms. We define authenticators that are
 * part of the client's platform as having a platform attachment, and refer to them as platform authenticators.
 * While those that are reachable via cross-platform transport protocols are defined as having cross-platform
 * attachment, and refer to them as roaming authenticators.
 */
public enum Attachment implements Parcelable {
    PLATFORM("platform"),
    CROSS_PLATFORM("cross-platform");

    private final String value;

    Attachment(String value) {
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
    public static Attachment fromString(String attachment) throws UnsupportedAttachmentException {
        for (Attachment value : values()) {
            if (value.value.equals(attachment)) return value;
        }
        throw new UnsupportedAttachmentException("Attachment " + attachment + " not supported");
    }

    public static Creator<Attachment> CREATOR = new Creator<Attachment>() {
        @Override
        public Attachment createFromParcel(Parcel source) {
            try {
                return Attachment.fromString(source.readString());
            } catch (Attachment.UnsupportedAttachmentException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };

    /**
     * Exception thrown when an unsupported or unrecognized attachment is encountered.
     */
    public static class UnsupportedAttachmentException extends Exception {
        public UnsupportedAttachmentException(String message) {
            super(message);
        }
    }
}
