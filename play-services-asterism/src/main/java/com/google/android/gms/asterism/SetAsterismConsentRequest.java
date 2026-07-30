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
 * Request object for setting Asterism consent state.
 * Used by RCS-enabled applications to grant or revoke consent
 * for phone number verification and related features.
 */
public class SetAsterismConsentRequest implements Parcelable {

    public static final int ACTION_GRANT = 1;
    public static final int ACTION_REVOKE = 2;
    public static final int ACTION_REFRESH = 3;
    public static final int ACTION_CHECK_STATUS = 4;

    private final int action;
    private final int requestId;
    @Nullable
    private final String packageName;
    @Nullable
    private final String accountName;
    @Nullable
    private final String callingPackage;
    private final long timestamp;
    private final long ttlMillis;
    private final boolean requiresDeviceIntegrity;

    public SetAsterismConsentRequest(int action, int requestId, @Nullable String packageName,
                                      @Nullable String accountName, @Nullable String callingPackage,
                                      long timestamp, long ttlMillis, boolean requiresDeviceIntegrity) {
        this.action = action;
        this.requestId = requestId;
        this.packageName = packageName;
        this.accountName = accountName;
        this.callingPackage = callingPackage;
        this.timestamp = timestamp;
        this.ttlMillis = ttlMillis;
        this.requiresDeviceIntegrity = requiresDeviceIntegrity;
    }

    protected SetAsterismConsentRequest(Parcel in) {
        action = in.readInt();
        requestId = in.readInt();
        packageName = in.readString();
        accountName = in.readString();
        callingPackage = in.readString();
        timestamp = in.readLong();
        ttlMillis = in.readLong();
        requiresDeviceIntegrity = in.readByte() != 0;
    }

    public int getAction() {
        return action;
    }

    public int getRequestId() {
        return requestId;
    }

    @Nullable
    public String getPackageName() {
        return packageName;
    }

    @Nullable
    public String getAccountName() {
        return accountName;
    }

    @Nullable
    public String getCallingPackage() {
        return callingPackage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    public boolean isRequiresDeviceIntegrity() {
        return requiresDeviceIntegrity;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(action);
        dest.writeInt(requestId);
        dest.writeString(packageName);
        dest.writeString(accountName);
        dest.writeString(callingPackage);
        dest.writeLong(timestamp);
        dest.writeLong(ttlMillis);
        dest.writeByte((byte) (requiresDeviceIntegrity ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SetAsterismConsentRequest> CREATOR = new Creator<SetAsterismConsentRequest>() {
        @Override
        public SetAsterismConsentRequest createFromParcel(Parcel in) {
            return new SetAsterismConsentRequest(in);
        }

        @Override
        public SetAsterismConsentRequest[] newArray(int size) {
            return new SetAsterismConsentRequest[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "SetAsterismConsentRequest{action=" + action
                + ", requestId=" + requestId
                + ", packageName='" + packageName + '\''
                + ", accountName='" + accountName + '\''
                + ", callingPackage='" + callingPackage + '\''
                + ", timestamp=" + timestamp
                + ", ttlMillis=" + ttlMillis
                + ", requiresDeviceIntegrity=" + requiresDeviceIntegrity + '}';
    }
}
