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
 * Request object for getting Asterism consent state.
 * Asterism consent controls whether RCS phone number verification
 * features are enabled for the current device and account.
 */
public class GetAsterismConsentRequest implements Parcelable {

    private final int requestId;
    @Nullable
    private final String packageName;
    private final long timestamp;

    public GetAsterismConsentRequest(int requestId, @Nullable String packageName, long timestamp) {
        this.requestId = requestId;
        this.packageName = packageName;
        this.timestamp = timestamp;
    }

    protected GetAsterismConsentRequest(Parcel in) {
        requestId = in.readInt();
        packageName = in.readString();
        timestamp = in.readLong();
    }

    public int getRequestId() {
        return requestId;
    }

    @Nullable
    public String getPackageName() {
        return packageName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(requestId);
        dest.writeString(packageName);
        dest.writeLong(timestamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GetAsterismConsentRequest> CREATOR = new Creator<GetAsterismConsentRequest>() {
        @Override
        public GetAsterismConsentRequest createFromParcel(Parcel in) {
            return new GetAsterismConsentRequest(in);
        }

        @Override
        public GetAsterismConsentRequest[] newArray(int size) {
            return new GetAsterismConsentRequest[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "GetAsterismConsentRequest{requestId=" + requestId
                + ", packageName='" + packageName + '\''
                + ", timestamp=" + timestamp + '}';
    }
}
