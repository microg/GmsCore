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
 * Request for Phone Number Verification (PNV) capabilities.
 * Queries which verification methods are available for a given phone number.
 */
public class GetPnvCapabilitiesRequest implements Parcelable {

    @NonNull
    private final String phoneNumber;
    private final int subId;
    @Nullable
    private final String mccMnc;
    @Nullable
    private final String gid1;
    private final long requestTimestamp;
    private final boolean includeCarrierInfo;
    @Nullable
    private final String preferredMethod;

    public GetPnvCapabilitiesRequest(@NonNull String phoneNumber, int subId,
                                      @Nullable String mccMnc, @Nullable String gid1,
                                      long requestTimestamp, boolean includeCarrierInfo,
                                      @Nullable String preferredMethod) {
        this.phoneNumber = phoneNumber;
        this.subId = subId;
        this.mccMnc = mccMnc;
        this.gid1 = gid1;
        this.requestTimestamp = requestTimestamp;
        this.includeCarrierInfo = includeCarrierInfo;
        this.preferredMethod = preferredMethod;
    }

    protected GetPnvCapabilitiesRequest(Parcel in) {
        phoneNumber = in.readString();
        subId = in.readInt();
        mccMnc = in.readString();
        gid1 = in.readString();
        requestTimestamp = in.readLong();
        includeCarrierInfo = in.readByte() != 0;
        preferredMethod = in.readString();
    }

    @NonNull
    public String getPhoneNumber() { return phoneNumber; }
    public int getSubId() { return subId; }
    @Nullable
    public String getMccMnc() { return mccMnc; }
    @Nullable
    public String getGid1() { return gid1; }
    public long getRequestTimestamp() { return requestTimestamp; }
    public boolean isIncludeCarrierInfo() { return includeCarrierInfo; }
    @Nullable
    public String getPreferredMethod() { return preferredMethod; }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(phoneNumber);
        dest.writeInt(subId);
        dest.writeString(mccMnc);
        dest.writeString(gid1);
        dest.writeLong(requestTimestamp);
        dest.writeByte((byte) (includeCarrierInfo ? 1 : 0));
        dest.writeString(preferredMethod);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<GetPnvCapabilitiesRequest> CREATOR = new Creator<GetPnvCapabilitiesRequest>() {
        @Override
        public GetPnvCapabilitiesRequest createFromParcel(Parcel in) { return new GetPnvCapabilitiesRequest(in); }
        @Override
        public GetPnvCapabilitiesRequest[] newArray(int size) { return new GetPnvCapabilitiesRequest[size]; }
    };

    @NonNull
    @Override
    public String toString() {
        return "GetPnvCapabilitiesRequest{phoneNumber='" + phoneNumber + '\''
                + ", mccMnc='" + mccMnc + '\'' + ", subId=" + subId + '}';
    }
}
