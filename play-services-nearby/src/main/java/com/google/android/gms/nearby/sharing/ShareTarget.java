/*
 * SPDX-FileCopyrightText: 2023, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.sharing;

import android.os.Parcel;
import android.os.Parcelable;

import org.microg.gms.common.PublicApi;

@PublicApi
public class ShareTarget implements Parcelable {
    public static final int DEVICE_TYPE_UNKNOWN = 0;
    public static final int DEVICE_TYPE_PHONE = 1;
    public static final int DEVICE_TYPE_TABLET = 2;
    public static final int DEVICE_TYPE_LAPTOP = 3;

    private final long id;
    private final String deviceName;
    private final String endpointId;
    private final int deviceType;
    private final boolean isIncoming;
    private final boolean isKnown;

    public ShareTarget(long id, String deviceName, String endpointId, int deviceType, boolean isIncoming, boolean isKnown) {
        this.id = id;
        this.deviceName = deviceName != null ? deviceName : "Nearby Device";
        this.endpointId = endpointId != null ? endpointId : "";
        this.deviceType = deviceType;
        this.isIncoming = isIncoming;
        this.isKnown = isKnown;
    }

    protected ShareTarget(Parcel in) {
        this.id = in.readLong();
        this.deviceName = in.readString();
        this.endpointId = in.readString();
        this.deviceType = in.readInt();
        this.isIncoming = in.readByte() != 0;
        this.isKnown = in.readByte() != 0;
    }

    public long getId() {
        return id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    public boolean isKnown() {
        return isKnown;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(deviceName);
        dest.writeString(endpointId);
        dest.writeInt(deviceType);
        dest.writeByte((byte) (isIncoming ? 1 : 0));
        dest.writeByte((byte) (isKnown ? 1 : 0));
    }

    public static final Creator<ShareTarget> CREATOR = new Creator<ShareTarget>() {
        @Override
        public ShareTarget createFromParcel(Parcel in) {
            return new ShareTarget(in);
        }

        @Override
        public ShareTarget[] newArray(int size) {
            return new ShareTarget[size];
        }
    };
}
