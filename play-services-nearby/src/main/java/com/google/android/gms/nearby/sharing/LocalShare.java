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
 * The local endpoint that is advertised while receiving nearby shares.
 */
public class LocalShare implements Parcelable {
    private final String deviceName;
    private final byte[] shareTargetId;

    public LocalShare(String deviceName, byte[] shareTargetId) {
        this.deviceName = deviceName;
        this.shareTargetId = shareTargetId == null ? new byte[0] : Arrays.copyOf(shareTargetId, shareTargetId.length);
    }

    protected LocalShare(Parcel in) {
        this.deviceName = in.readString();
        this.shareTargetId = in.createByteArray();
    }

    public String getDeviceName() {
        return deviceName;
    }

    public byte[] getShareTargetId() {
        return Arrays.copyOf(shareTargetId, shareTargetId.length);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceName);
        dest.writeByteArray(shareTargetId);
    }

    public static final Creator<LocalShare> CREATOR = new Creator<LocalShare>() {
        @Override
        public LocalShare createFromParcel(Parcel in) {
            return new LocalShare(in);
        }

        @Override
        public LocalShare[] newArray(int size) {
            return new LocalShare[size];
        }
    };
}
