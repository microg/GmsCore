/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common;

import android.app.Dialog;
import android.content.Intent;

/**
 * {@code GooglePlayServicesRepairableException}s are special instances of {@link UserRecoverableException}s which are
 * thrown when Google Play services is not installed, up-to-date, or enabled. In these cases, client code can use
 * {@link #getConnectionStatusCode()} in conjunction with {@link GoogleApiAvailability#getErrorDialog(android.app.Activity, int, int)}
 * to provide users with a localized {@link Dialog} that will allow users to install, update, or otherwise enable Google Play services.
 */
public class GooglePlayServicesRepairableException extends UserRecoverableException {
    private final int connectionStatusCode;

    /**
     * Creates a {@link GooglePlayServicesRepairableException}.
     *
     * @param connectionStatusCode a code for the {@link ConnectionResult} {@code statusCode} of the exception
     * @param message              a string message for the exception
     * @param intent               an intent that may be started to resolve the connection issue with Google Play services
     */
    public GooglePlayServicesRepairableException(int connectionStatusCode, String message, Intent intent) {
        super(message, intent);
        this.connectionStatusCode = connectionStatusCode;
    }

    /**
     * Returns the {@link ConnectionResult} {@code statusCode} of the exception.
     * <p>
     * This value may be passed in to {@link GoogleApiAvailability#getErrorDialog(android.app.Activity, int, int)} to
     * provide users with a localized {@link Dialog} that will allow users to install, update, or otherwise enable Google Play services.
     */
    public int getConnectionStatusCode() {
        return connectionStatusCode;
    }
}
