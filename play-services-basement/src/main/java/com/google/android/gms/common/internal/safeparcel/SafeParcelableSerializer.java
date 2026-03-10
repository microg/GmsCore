/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal.safeparcel;

import android.os.Parcel;
import android.os.Parcelable;

public class SafeParcelableSerializer {
    public static <T extends SafeParcelable> T deserializeFromBytes(byte[] bytes, Parcelable.Creator<T> tCreator) {
        if (bytes == null) return null;
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        T parcelable = tCreator.createFromParcel(parcel);
        parcel.recycle();
        return parcelable;
    }

    public static <T extends SafeParcelable> byte[] serializeToBytes(T parcelable) {
        if (parcelable == null) return null;
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }
}
