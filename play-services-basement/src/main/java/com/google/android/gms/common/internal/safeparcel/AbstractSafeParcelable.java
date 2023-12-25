/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal.safeparcel;

public abstract class AbstractSafeParcelable implements SafeParcelable {

    @SuppressWarnings("unchecked")
    public static <T extends AbstractSafeParcelable> SafeParcelableCreatorAndWriter<T> findCreator(java.lang.Class<T> tClass) {
        String creatorClassName = tClass.getName() + "$000Creator";
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
