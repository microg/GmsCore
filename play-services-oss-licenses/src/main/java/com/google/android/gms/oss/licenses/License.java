/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.oss.licenses;

import android.os.Parcel;
import android.os.Parcelable;

public class License implements Parcelable, Comparable<License> {
    public static final Creator<License> CREATOR = new Creator<License>() {
        @Override
        public License createFromParcel(Parcel source) {
            return new License(source);
        }

        @Override
        public License[] newArray(int size) {
            return new License[size];
        }
    };

    private final String name;
    private final long offset;
    private final int length;
    private final String path;

    public License(String name, long offset, int length, String path) {
        this.name = name;
        this.offset = offset;
        this.length = length;
        this.path = path;
    }

    public License(Parcel parcel) {
        this.name = parcel.readString();
        this.offset = parcel.readLong();
        this.length = parcel.readInt();
        this.path = parcel.readString();
    }

    public String getName() {
        return name;
    }

    public long getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int compareTo(License other) {
        return name.compareToIgnoreCase(other.name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeLong(offset);
        dest.writeInt(length);
        dest.writeString(path);
    }

    @Override
    public String toString() {
        return name;
    }
}
