/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel;

import android.os.Parcel;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.lang.reflect.Array;

public class ReflectedSafeParcelableCreatorAndWriter<T extends AutoSafeParcelable> implements SafeParcelableCreatorAndWriter<T> {

    private final SafeParcelReflectionUtil.ClassDescriptor<T> descriptor;

    public ReflectedSafeParcelableCreatorAndWriter(Class<T> tClass) {
        this.descriptor = new SafeParcelReflectionUtil.ClassDescriptor<>(tClass);
    }

    @Override
    public T createFromParcel(Parcel parcel) {
        return SafeParcelReflectionUtil.createObject(parcel, descriptor);
    }

    @Override
    public void writeToParcel(T object, Parcel parcel, int flags) {
        SafeParcelReflectionUtil.writeObject(object, parcel, flags, descriptor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T[] newArray(int i) {
        return (T[]) Array.newInstance(descriptor.tClass, i);
    }
}
