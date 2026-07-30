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
 * Request to verify a phone number via RCS Constellation service.
 * Supports multiple verification methods including SMS, TS43, and carrier-based verification.
 */
public class VerifyPhoneNumberRequest implements Parcelable {

    public static final int VERIFICATION_METHOD_SMS = 1;
    public static final int VERIFICATION_METHOD_TS43 = 2;
    public static final int VERIFICATION_METHOD_CARRIER = 3;
    public static final int VERIFICATION_METHOD_AUTO = 4;

    @NonNull
    private final String phoneNumber;
    private final int verificationMethod;
    @Nullable
    private final String simCountryIso;
    @Nullable
    private final String networkCountryIso;
    private final int subId;
    private final long requestTimestamp;
    private final boolean forceRefresh;
    @Nullable
    private final String clientPackageName;
    private final int clientVersionCode;
    @Nullable
    private final String deviceId;

    public VerifyPhoneNumberRequest(@NonNull String phoneNumber, int verificationMethod,
                                     @Nullable String simCountryIso, @Nullable String networkCountryIso,
                                     int subId, long requestTimestamp, boolean forceRefresh,
                                     @Nullable String clientPackageName, int clientVersionCode,
                                     @Nullable String deviceId) {
        this.phoneNumber = phoneNumber;
        this.verificationMethod = verificationMethod;
        this.simCountryIso = simCountryIso;
        this.networkCountryIso = networkCountryIso;
        this.subId = subId;
        this.requestTimestamp = requestTimestamp;
        this.forceRefresh = forceRefresh;
        this.clientPackageName = clientPackageName;
        this.clientVersionCode = clientVersionCode;
        this.deviceId = deviceId;
    }

    protected VerifyPhoneNumberRequest(Parcel in) {
        phoneNumber = in.readString();
        verificationMethod = in.readInt();
        simCountryIso = in.readString();
        networkCountryIso = in.readString();
        subId = in.readInt();
        requestTimestamp = in.readLong();
        forceRefresh = in.readByte() != 0;
        clientPackageName = in.readString();
        clientVersionCode = in.readInt();
        deviceId = in.readString();
    }

    @NonNull
    public String getPhoneNumber() { return phoneNumber; }
    public int getVerificationMethod() { return verificationMethod; }
    @Nullable
    public String getSimCountryIso() { return simCountryIso; }
    @Nullable
    public String getNetworkCountryIso() { return networkCountryIso; }
    public int getSubId() { return subId; }
    public long getRequestTimestamp() { return requestTimestamp; }
    public boolean isForceRefresh() { return forceRefresh; }
    @Nullable
    public String getClientPackageName() { return clientPackageName; }
    public int getClientVersionCode() { return clientVersionCode; }
    @Nullable
    public String getDeviceId() { return deviceId; }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(phoneNumber);
        dest.writeInt(verificationMethod);
        dest.writeString(simCountryIso);
        dest.writeString(networkCountryIso);
        dest.writeInt(subId);
        dest.writeLong(requestTimestamp);
        dest.writeByte((byte) (forceRefresh ? 1 : 0));
        dest.writeString(clientPackageName);
        dest.writeInt(clientVersionCode);
        dest.writeString(deviceId);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<VerifyPhoneNumberRequest> CREATOR = new Creator<VerifyPhoneNumberRequest>() {
        @Override
        public VerifyPhoneNumberRequest createFromParcel(Parcel in) {
            return new VerifyPhoneNumberRequest(in);
        }
        @Override
        public VerifyPhoneNumberRequest[] newArray(int size) {
            return new VerifyPhoneNumberRequest[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "VerifyPhoneNumberRequest{phoneNumber='" + phoneNumber + '\''
                + ", verificationMethod=" + verificationMethod
                + ", simCountryIso='" + simCountryIso + '\''
                + ", subId=" + subId + ", forceRefresh=" + forceRefresh + '}';
    }
}
