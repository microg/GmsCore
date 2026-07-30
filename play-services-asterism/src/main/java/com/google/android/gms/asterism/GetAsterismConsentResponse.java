/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Response object for Asterism consent queries.
 * Contains the current consent state and related metadata
 * used by RCS services to determine feature availability.
 */
public class GetAsterismConsentResponse implements Parcelable {

    public static final int CONSENT_UNKNOWN = 0;
    public static final int CONSENT_GRANTED = 1;
    public static final int CONSENT_DENIED = 2;
    public static final int CONSENT_PENDING = 3;
    public static final int CONSENT_EXPIRED = 4;

    private final int consentState;
    private final long consentTimestamp;
    private final long expiryTimestamp;
    @Nullable
    private final String consentToken;
    private final int statusCode;
    @Nullable
    private final String statusMessage;

    public GetAsterismConsentResponse(int consentState, long consentTimestamp,
                                       long expiryTimestamp, @Nullable String consentToken,
                                       int statusCode, @Nullable String statusMessage) {
        this.consentState = consentState;
        this.consentTimestamp = consentTimestamp;
        this.expiryTimestamp = expiryTimestamp;
        this.consentToken = consentToken;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    protected GetAsterismConsentResponse(Parcel in) {
        consentState = in.readInt();
        consentTimestamp = in.readLong();
        expiryTimestamp = in.readLong();
        consentToken = in.readString();
        statusCode = in.readInt();
        statusMessage = in.readString();
    }

    public int getConsentState() {
        return consentState;
    }

    public long getConsentTimestamp() {
        return consentTimestamp;
    }

    public long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    @Nullable
    public String getConsentToken() {
        return consentToken;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Nullable
    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isConsentGranted() {
        return consentState == CONSENT_GRANTED;
    }

    public boolean isExpired() {
        return consentState == CONSENT_EXPIRED
                || (expiryTimestamp > 0 && System.currentTimeMillis() > expiryTimestamp);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(consentState);
        dest.writeLong(consentTimestamp);
        dest.writeLong(expiryTimestamp);
        dest.writeString(consentToken);
        dest.writeInt(statusCode);
        dest.writeString(statusMessage);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GetAsterismConsentResponse> CREATOR = new Creator<GetAsterismConsentResponse>() {
        @Override
        public GetAsterismConsentResponse createFromParcel(Parcel in) {
            return new GetAsterismConsentResponse(in);
        }

        @Override
        public GetAsterismConsentResponse[] newArray(int size) {
            return new GetAsterismConsentResponse[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "GetAsterismConsentResponse{consentState=" + consentState
                + ", consentTimestamp=" + consentTimestamp
                + ", expiryTimestamp=" + expiryTimestamp
                + ", consentToken='" + consentToken + '\''
                + ", statusCode=" + statusCode
                + ", statusMessage='" + statusMessage + '\'' + '}';
    }
}
