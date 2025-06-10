package com.google.android.gms.common.internal.safeparcel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.CLASS)
public @interface Param {
    int value();
}
