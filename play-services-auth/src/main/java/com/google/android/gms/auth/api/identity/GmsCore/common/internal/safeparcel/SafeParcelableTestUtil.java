package com.google.android.gms.common.internal.safeparcel;

import android.os.Parcel;
import android.os.Parcelable;

import static com.google.common.truth.Truth.assertThat;

public class SafeParcelableTestUtil {
    public static <T extends Parcelable> void assertSafeParcelableRoundTrip(Parcelable.Creator<T> creator, T original) {
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        T created = creator.createFromParcel(parcel);
        parcel.recycle();

        assertThat(created).isEqualTo(original);
    }
          }
