/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.Manifest;
import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Location granularity levels to be used with APIs within FusedLocationProviderClient.
 */
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.SOURCE)
@IntDef({Granularity.GRANULARITY_PERMISSION_LEVEL, Granularity.GRANULARITY_COARSE, Granularity.GRANULARITY_FINE})
public @interface Granularity {
    /**
     * The desired location granularity should correspond to the client permission level. The client will be delivered fine
     * locations while it has the {@link Manifest.permission#ACCESS_FINE_LOCATION} permission, coarse locations while it has
     * only the {@link Manifest.permission#ACCESS_COARSE_LOCATION} permission, and no location if it lacks either.
     */
    int GRANULARITY_PERMISSION_LEVEL = 0;
    /**
     * The desired location granularity is always coarse, regardless of the client permission level. The client will be delivered
     * coarse locations while it has the {@link Manifest.permission#ACCESS_FINE_LOCATION} or
     * {@link Manifest.permission#ACCESS_COARSE_LOCATION} permission, and no location if it lacks either.
     */
    int GRANULARITY_COARSE = 1;
    /**
     * The desired location granularity is always fine, regardless of the client permission level. The client will be delivered fine
     * locations while it has the {@link Manifest.permission#ACCESS_FINE_LOCATION}, and no location if it lacks that permission.
     */
    int GRANULARITY_FINE = 2;
}
