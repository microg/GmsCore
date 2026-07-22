/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Deprecated
public @interface SafeParceled {
    int value();

    boolean mayNull() default false;

    @Deprecated String subType() default "undefined";

    Class subClass() default SafeParceled.class;

    boolean useClassLoader() default false;
}