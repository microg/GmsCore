/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.sharing;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/**
 * A device that was discovered nearby and can receive a share.
 */
public class ShareTarget implements Parcelable {
    private final String deviceName;
    private final int deviceType;
    private final byte[] targetId;

    public ShareTarget(String deviceName, int deviceType, byte[] targetId) {
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.targetId = targetId == null ? new byte[0] : Arrays.copyOf(targetId, targetId.length);
    }

    protected ShareTarget(Parcel in) {
        this.deviceName = in.readString();
        this.deviceType = in.readInt();
        this.targetId = in.createByteArray();
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public byte[] getTargetId() {
        return Arrays.copyOf(targetId, targetId.length);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceName);
        dest.writeInt(deviceType);
        dest.writeByteArray(targetId);
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
