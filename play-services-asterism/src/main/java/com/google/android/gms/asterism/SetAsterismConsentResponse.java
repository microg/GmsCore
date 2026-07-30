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
 * Response object for Asterism consent modification requests.
 * Indicates whether the consent operation was successful and
 * provides the updated consent state.
 */
public class SetAsterismConsentResponse implements Parcelable {

    public static final int RESULT_OK = 0;
    public static final int RESULT_ERROR_UNKNOWN = 1;
    public static final int RESULT_ERROR_INVALID_ARGUMENT = 2;
    public static final int RESULT_ERROR_NOT_AUTHORIZED = 3;
    public static final int RESULT_ERROR_NETWORK = 4;
    public static final int RESULT_ERROR_INTEGRITY_CHECK_FAILED = 5;
    public static final int RESULT_ERROR_CONSENT_EXPIRED = 6;

    private final int resultCode;
    private final int consentState;
    @Nullable
    private final String errorMessage;
    @Nullable
    private final String updatedToken;
    private final long updatedExpiryTimestamp;

    public SetAsterismConsentResponse(int resultCode, int consentState,
                                       @Nullable String errorMessage, @Nullable String updatedToken,
                                       long updatedExpiryTimestamp) {
        this.resultCode = resultCode;
        this.consentState = consentState;
        this.errorMessage = errorMessage;
        this.updatedToken = updatedToken;
        this.updatedExpiryTimestamp = updatedExpiryTimestamp;
    }

    protected SetAsterismConsentResponse(Parcel in) {
        resultCode = in.readInt();
        consentState = in.readInt();
        errorMessage = in.readString();
        updatedToken = in.readString();
        updatedExpiryTimestamp = in.readLong();
    }

    public int getResultCode() {
        return resultCode;
    }

    public int getConsentState() {
        return consentState;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Nullable
    public String getUpdatedToken() {
        return updatedToken;
    }

    public long getUpdatedExpiryTimestamp() {
        return updatedExpiryTimestamp;
    }

    public boolean isSuccess() {
        return resultCode == RESULT_OK;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(resultCode);
        dest.writeInt(consentState);
        dest.writeString(errorMessage);
        dest.writeString(updatedToken);
        dest.writeLong(updatedExpiryTimestamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SetAsterismConsentResponse> CREATOR = new Creator<SetAsterismConsentResponse>() {
        @Override
        public SetAsterismConsentResponse createFromParcel(Parcel in) {
            return new SetAsterismConsentResponse(in);
        }

        @Override
        public SetAsterismConsentResponse[] newArray(int size) {
            return new SetAsterismConsentResponse[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "SetAsterismConsentResponse{resultCode=" + resultCode
                + ", consentState=" + consentState
                + ", errorMessage='" + errorMessage + '\''
                + ", updatedToken='" + updatedToken + '\''
                + ", updatedExpiryTimestamp=" + updatedExpiryTimestamp + '}';
    }
}
