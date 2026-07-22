/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BulletPoint implements Parcelable {

    public HashMap<Integer, String> contentMap = new HashMap<>();

    public BulletPoint() {
    }

    public BulletPoint(HashMap<Integer, String> contentMap) {
        this.contentMap = contentMap;
    }

    public BulletPoint(Parcel parcel) {
        int readInt = parcel.readInt();
        for (int i = 0; i < readInt; i++) {
            this.contentMap.put(parcel.readInt(), parcel.readString());
        }
    }

    public final boolean equals(Object obj) {
        return (obj instanceof BulletPoint) && ((BulletPoint) obj).contentMap.equals(this.contentMap);
    }

    public final int hashCode() {
        return Arrays.hashCode(new Object[]{this.contentMap});
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(this.contentMap.size());
        for (Map.Entry entry : this.contentMap.entrySet()) {
            dest.writeInt((Integer) entry.getKey());
            dest.writeString((String) entry.getValue());
        }
    }

    public static final Creator<BulletPoint> CREATOR = new Creator<BulletPoint>() {
        @Override
        public BulletPoint createFromParcel(Parcel source) {
            return new BulletPoint(source);
        }

        @Override
        public BulletPoint[] newArray(int size) {
            return new BulletPoint[size];
        }
    };
}
