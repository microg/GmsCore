/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel;

import android.os.Parcelable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface SafeParcelable extends Parcelable {
    @Deprecated
    String NULL = "SAFE_PARCELABLE_NULL_STRING";
    int SAFE_PARCEL_OBJECT_MAGIC = 0x4F45;
    @Deprecated
    int SAFE_PARCEL_MAGIC = SAFE_PARCEL_OBJECT_MAGIC;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Field {
        int value();

        boolean mayNull() default false;

        Class subClass() default SafeParcelable.class;

        boolean useValueParcel() default false;

        boolean useDirectList() default false;

        long versionCode() default -1;
    }
}
