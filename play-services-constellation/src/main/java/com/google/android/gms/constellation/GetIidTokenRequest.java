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
 * Request for an IID (Instance ID) token used in RCS registration.
 * The IID token is required for Google Messages to authenticate with RCS servers.
 */
public class GetIidTokenRequest implements Parcelable {

    @NonNull
    private final String senderId;
    @Nullable
    private final String scope;
    @Nullable
    private final String authorizedEntity;
    private final long requestTimestamp;
    private final boolean forceRefresh;

    public GetIidTokenRequest(@NonNull String senderId, @Nullable String scope,
                               @Nullable String authorizedEntity, long requestTimestamp,
                               boolean forceRefresh) {
        this.senderId = senderId;
        this.scope = scope;
        this.authorizedEntity = authorizedEntity;
        this.requestTimestamp = requestTimestamp;
        this.forceRefresh = forceRefresh;
    }

    protected GetIidTokenRequest(Parcel in) {
        senderId = in.readString();
        scope = in.readString();
        authorizedEntity = in.readString();
        requestTimestamp = in.readLong();
        forceRefresh = in.readByte() != 0;
    }

    @NonNull
    public String getSenderId() { return senderId; }
    @Nullable
    public String getScope() { return scope; }
    @Nullable
    public String getAuthorizedEntity() { return authorizedEntity; }
    public long getRequestTimestamp() { return requestTimestamp; }
    public boolean isForceRefresh() { return forceRefresh; }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(senderId);
        dest.writeString(scope);
        dest.writeString(authorizedEntity);
        dest.writeLong(requestTimestamp);
        dest.writeByte((byte) (forceRefresh ? 1 : 0));
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<GetIidTokenRequest> CREATOR = new Creator<GetIidTokenRequest>() {
        @Override
        public GetIidTokenRequest createFromParcel(Parcel in) { return new GetIidTokenRequest(in); }
        @Override
        public GetIidTokenRequest[] newArray(int size) { return new GetIidTokenRequest[size]; }
    };

    @NonNull
    @Override
    public String toString() {
        return "GetIidTokenRequest{senderId='" + senderId + '\'' + ", scope='" + scope + '\''
                + ", forceRefresh=" + forceRefresh + '}';
    }
}
