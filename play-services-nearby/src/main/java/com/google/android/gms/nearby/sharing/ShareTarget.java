/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.sharing;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

public class ShareTarget implements Parcelable {
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_PHONE = 1;
    public static final int TYPE_TABLET = 2;
    public static final int TYPE_COMPUTER = 3;

    private final long id;
    private final String name;
    private final int deviceType;
    private final String imageUrl;
    private final boolean isIncoming;

    public ShareTarget(long id, String name, int deviceType, String imageUrl, boolean isIncoming) {
        this.id = id;
        this.name = name;
        this.deviceType = deviceType;
        this.imageUrl = imageUrl;
        this.isIncoming = isIncoming;
    }

    protected ShareTarget(Parcel in) {
        id = in.readLong();
        name = in.readString();
        deviceType = in.readInt();
        imageUrl = in.readString();
        isIncoming = in.readByte() != 0;
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

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeInt(deviceType);
        dest.writeString(imageUrl);
        dest.writeByte((byte) (isIncoming ? 1 : 0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShareTarget that = (ShareTarget) o;
        return id == that.id && deviceType == that.deviceType && isIncoming == that.isIncoming && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, deviceType, isIncoming);
    }
}
