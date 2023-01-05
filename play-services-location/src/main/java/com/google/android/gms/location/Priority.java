/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Location power vs accuracy priority levels to be used with APIs within {@link FusedLocationProviderClient}.
 * <p>
 * Priority values have been intentionally chosen to match the framework QUALITY constants, and the values are specified
 * such that higher priorities should always have lower values and vice versa.
 */
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.SOURCE)
@IntDef({Priority.PRIORITY_HIGH_ACCURACY, Priority.PRIORITY_BALANCED_POWER_ACCURACY, Priority.PRIORITY_LOW_POWER, Priority.PRIORITY_PASSIVE})
public @interface Priority {
    /**
     * Requests a tradeoff that favors highly accurate locations at the possible expense of additional power usage.
     */
    int PRIORITY_HIGH_ACCURACY = 100;

    /**
     * Requests a tradeoff that is balanced between location accuracy and power usage.
     */
    int PRIORITY_BALANCED_POWER_ACCURACY = 102;

    /**
     * Requests a tradeoff that favors low power usage at the possible expense of location accuracy.
     */
    int PRIORITY_LOW_POWER = 104;

    /**
     * Ensures that no extra power will be used to derive locations. This enforces that the request will act as a passive listener
     * that will only receive "free" locations calculated on behalf of other clients, and no locations will be calculated on behalf of
     * only this request.
     */
    int PRIORITY_PASSIVE = 105;
}
