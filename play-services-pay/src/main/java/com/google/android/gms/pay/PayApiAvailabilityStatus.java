/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.pay;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pay API availability status on the device.
 */
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.SOURCE)
@IntDef({PayApiAvailabilityStatus.AVAILABLE, PayApiAvailabilityStatus.NOT_ELIGIBLE})
public @interface PayApiAvailabilityStatus {
    /**
     * Indicates that the Pay API requested is available and ready to be used.
     */
    int AVAILABLE = 0;
    /**
     * Indicates that the user is currently not eligible for using the Pay API requested. The user may become eligible in the future.
     */
    int NOT_ELIGIBLE = 2;
}
