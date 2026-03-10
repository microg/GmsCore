/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal.safeparcel;

public abstract class AbstractSafeParcelable implements SafeParcelable {

    @SuppressWarnings("unchecked")
    public static <T extends AbstractSafeParcelable> SafeParcelableCreatorAndWriter<T> findCreator(java.lang.Class<T> tClass) {
        java.lang.Class<?> upmostClass = tClass;
        while (upmostClass.getEnclosingClass() != null) upmostClass = upmostClass.getEnclosingClass();
        String upmostClassName = upmostClass.getName();
        int idx = upmostClassName.lastIndexOf('.');
        String packagePrefix = idx > 0 ? upmostClassName.substring(0, idx + 1) : "";
        String creatorClassName = packagePrefix + tClass.getSimpleName() + "$000Creator";
        try {
            return (SafeParcelableCreatorAndWriter<T>) java.lang.Class.forName(creatorClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No Creator found for " + tClass.getName(), e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
