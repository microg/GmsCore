/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal.safeparcel;

import android.os.Parcelable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface SafeParcelable extends Parcelable {
    int SAFE_PARCEL_OBJECT_MAGIC = 0x4F45;

    @Target(ElementType.TYPE)
    @interface Class {
    }

    @Target(ElementType.CONSTRUCTOR)
    @interface Constructor {

    }

    @Target(ElementType.PARAMETER)
    @interface Param {
        int value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Field {
        int value();

        boolean mayNull() default false;

        java.lang.Class<?> subClass() default SafeParcelable.class;

        boolean useValueParcel() default false;

        boolean useDirectList() default false;

        long versionCode() default -1;

        String defaultValue() default "";

        String type() default "";

        String getterName() default "";

        String getter() default "";
    }
}
