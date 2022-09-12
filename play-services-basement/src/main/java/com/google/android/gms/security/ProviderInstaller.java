/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.security;

import android.content.Context;
import android.content.Intent;

import java.security.Provider;

/**
 * A utility class for installing a dynamically updatable {@link Provider} to replace the platform default provider.
 */
public class ProviderInstaller {
    public static final String PROVIDER_NAME = "GmsCore_OpenSSL";

    /**
     * Installs the dynamically updatable security provider, if it's not already installed.
     *
     * @throws GooglePlayServicesRepairableException
     * @throws GooglePlayServicesNotAvailableException
     */
    public static void installIfNeeded(Context context) {

    }

    /**
     * Asynchronously installs the dynamically updatable security provider, if it's not already installed. This method must be called on the UI thread.
     *
     * @param context
     * @param listener called when the installation completes
     */
    public static void installIfNeededAsync(Context context, ProviderInstallListener listener) {
        if (listener != null) listener.onProviderInstalled();
    }

    /**
     * Callback for notification of the result of provider installation.
     */
    public interface ProviderInstallListener {
        /**
         * Called when installing the provider fails. This method is always called on the UI thread.
         * <p>
         * Implementers may use {@code errorCode} with the standard UI elements provided by {@link GoogleApiAvailability}; or {@code recoveryIntent} to implement custom UI.
         *
         * @param errorCode      error code for the failure, for use with {@link GoogleApiAvailability#showErrorDialogFragment(Activity, int, int)} or {@link GoogleApiAvailability#showErrorNotification(Context, ConnectionResult)}
         * @param recoveryIntent if non-null, an intent that can be used to install or update Google Play Services such that the provider can be installed
         */
        void onProviderInstallFailed(int errorCode, Intent recoveryIntent);

        /**
         * Called when installing the provider succeeds. This method is always called on the UI thread.
         */
        void onProviderInstalled();
    }
}
