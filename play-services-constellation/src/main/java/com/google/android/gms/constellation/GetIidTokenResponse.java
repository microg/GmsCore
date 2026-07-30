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
 * Response containing the IID token for RCS registration.
 */
public class GetIidTokenResponse implements Parcelable {

    @Nullable
    private final String token;
    private final long tokenTimestamp;
    private final long expiryTimestamp;
    private final int statusCode;
    @Nullable
    private final String statusMessage;
    @NonNull
    private final String senderId;

    public GetIidTokenResponse(@Nullable String token, long tokenTimestamp,
                                long expiryTimestamp, int statusCode,
                                @Nullable String statusMessage, @NonNull String senderId) {
        this.token = token;
        this.tokenTimestamp = tokenTimestamp;
        this.expiryTimestamp = expiryTimestamp;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.senderId = senderId;
    }

    protected GetIidTokenResponse(Parcel in) {
        token = in.readString();
        tokenTimestamp = in.readLong();
        expiryTimestamp = in.readLong();
        statusCode = in.readInt();
        statusMessage = in.readString();
        senderId = in.readString();
    }

    @Nullable
    public String getToken() { return token; }
    public long getTokenTimestamp() { return tokenTimestamp; }
    public long getExpiryTimestamp() { return expiryTimestamp; }
    public int getStatusCode() { return statusCode; }
    @Nullable
    public String getStatusMessage() { return statusMessage; }
    @NonNull
    public String getSenderId() { return senderId; }
    public boolean isSuccess() { return token != null && statusCode == 0; }
    public boolean isExpired() { return expiryTimestamp > 0 && System.currentTimeMillis() > expiryTimestamp; }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(token);
        dest.writeLong(tokenTimestamp);
        dest.writeLong(expiryTimestamp);
        dest.writeInt(statusCode);
        dest.writeString(statusMessage);
        dest.writeString(senderId);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<GetIidTokenResponse> CREATOR = new Creator<GetIidTokenResponse>() {
        @Override
        public GetIidTokenResponse createFromParcel(Parcel in) { return new GetIidTokenResponse(in); }
        @Override
        public GetIidTokenResponse[] newArray(int size) { return new GetIidTokenResponse[size]; }
    };

    @NonNull
    @Override
    public String toString() {
        return "GetIidTokenResponse{senderId='" + senderId + '\'' + ", hasToken=" + (token != null)
                + ", statusCode=" + statusCode + '}';
    }
}
