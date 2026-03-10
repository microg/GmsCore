/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common;

/**
 * Indicates Google Play services is not available.
 */
public class GooglePlayServicesNotAvailableException extends Exception {
    /**
     * The error code returned by {@link GoogleApiAvailability#isGooglePlayServicesAvailable(Context)} call.
     */
    public final int errorCode;

    public GooglePlayServicesNotAvailableException(int errorCode) {
        this.errorCode = errorCode;
    }
}
