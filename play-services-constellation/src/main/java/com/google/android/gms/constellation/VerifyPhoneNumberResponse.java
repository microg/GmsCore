/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Response from phone number verification via Constellation service.
 * Contains verification status, token, and RCS capability information.
 */
public class VerifyPhoneNumberResponse implements Parcelable {

    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_VERIFIED = 1;
    public static final int STATUS_PENDING = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_TIMEOUT = 4;
    public static final int STATUS_RETRY = 5;
    public static final int STATUS_NETWORK_ERROR = 6;

    private final int status;
    @NonNull
    private final String phoneNumber;
    @Nullable
    private final String verificationToken;
    @Nullable
    private final String rcsConfigurationToken;
    private final long verifiedTimestamp;
    private final long expiryTimestamp;
    private final int retryAfterSeconds;
    @Nullable
    private final String errorMessage;
    private final int errorCode;
    private final boolean rcsEnabled;
    private final boolean rcsGroupChatEnabled;
    private final boolean rcsFileTransferEnabled;

    public VerifyPhoneNumberResponse(int status, @NonNull String phoneNumber,
                                      @Nullable String verificationToken,
                                      @Nullable String rcsConfigurationToken,
                                      long verifiedTimestamp, long expiryTimestamp,
                                      int retryAfterSeconds, @Nullable String errorMessage,
                                      int errorCode, boolean rcsEnabled,
                                      boolean rcsGroupChatEnabled, boolean rcsFileTransferEnabled) {
        this.status = status;
        this.phoneNumber = phoneNumber;
        this.verificationToken = verificationToken;
        this.rcsConfigurationToken = rcsConfigurationToken;
        this.verifiedTimestamp = verifiedTimestamp;
        this.expiryTimestamp = expiryTimestamp;
        this.retryAfterSeconds = retryAfterSeconds;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.rcsEnabled = rcsEnabled;
        this.rcsGroupChatEnabled = rcsGroupChatEnabled;
        this.rcsFileTransferEnabled = rcsFileTransferEnabled;
    }

    protected VerifyPhoneNumberResponse(Parcel in) {
        status = in.readInt();
        phoneNumber = in.readString();
        verificationToken = in.readString();
        rcsConfigurationToken = in.readString();
        verifiedTimestamp = in.readLong();
        expiryTimestamp = in.readLong();
        retryAfterSeconds = in.readInt();
        errorMessage = in.readString();
        errorCode = in.readInt();
        rcsEnabled = in.readByte() != 0;
        rcsGroupChatEnabled = in.readByte() != 0;
        rcsFileTransferEnabled = in.readByte() != 0;
    }

    public int getStatus() { return status; }
    @NonNull
    public String getPhoneNumber() { return phoneNumber; }
    @Nullable
    public String getVerificationToken() { return verificationToken; }
    @Nullable
    public String getRcsConfigurationToken() { return rcsConfigurationToken; }
    public long getVerifiedTimestamp() { return verifiedTimestamp; }
    public long getExpiryTimestamp() { return expiryTimestamp; }
    public int getRetryAfterSeconds() { return retryAfterSeconds; }
    @Nullable
    public String getErrorMessage() { return errorMessage; }
    public int getErrorCode() { return errorCode; }
    public boolean isRcsEnabled() { return rcsEnabled; }
    public boolean isRcsGroupChatEnabled() { return rcsGroupChatEnabled; }
    public boolean isRcsFileTransferEnabled() { return rcsFileTransferEnabled; }
    public boolean isVerified() { return status == STATUS_VERIFIED; }
    public boolean isPending() { return status == STATUS_PENDING; }
    public boolean isExpired() { return expiryTimestamp > 0 && System.currentTimeMillis() > expiryTimestamp; }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(status);
        dest.writeString(phoneNumber);
        dest.writeString(verificationToken);
        dest.writeString(rcsConfigurationToken);
        dest.writeLong(verifiedTimestamp);
        dest.writeLong(expiryTimestamp);
        dest.writeInt(retryAfterSeconds);
        dest.writeString(errorMessage);
        dest.writeInt(errorCode);
        dest.writeByte((byte) (rcsEnabled ? 1 : 0));
        dest.writeByte((byte) (rcsGroupChatEnabled ? 1 : 0));
        dest.writeByte((byte) (rcsFileTransferEnabled ? 1 : 0));
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<VerifyPhoneNumberResponse> CREATOR = new Creator<VerifyPhoneNumberResponse>() {
        @Override
        public VerifyPhoneNumberResponse createFromParcel(Parcel in) { return new VerifyPhoneNumberResponse(in); }
        @Override
        public VerifyPhoneNumberResponse[] newArray(int size) { return new VerifyPhoneNumberResponse[size]; }
    };

    @NonNull
    @Override
    public String toString() {
        return "VerifyPhoneNumberResponse{status=" + status + ", phoneNumber='" + phoneNumber + '\''
                + ", rcsEnabled=" + rcsEnabled + ", expiryTimestamp=" + expiryTimestamp + '}';
    }
}
