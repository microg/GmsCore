/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel;

import android.os.Parcel;

import java.lang.reflect.Array;

public abstract class AutoSafeParcelable implements SafeParcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        SafeParcelUtil.writeObject(this, out, flags);
    }

    public static class AutoCreator<T extends SafeParcelable> implements Creator<T> {

        private final Class<T> tClass;

        public AutoCreator(Class<T> tClass) {
            this.tClass = tClass;
        }

        @Override
        public T createFromParcel(Parcel parcel) {
            return SafeParcelUtil.createObject(tClass, parcel);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T[] newArray(int i) {
            return (T[]) Array.newInstance(tClass, i);
        }
    }
}
