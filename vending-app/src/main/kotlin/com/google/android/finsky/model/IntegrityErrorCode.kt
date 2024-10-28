/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.model

import org.microg.gms.common.PublicApi

@PublicApi
annotation class IntegrityErrorCode {
    companion object {
        /**
         * Integrity API is not available.
         *
         * Integrity API is not enabled, or the Play Store version might be old.
         *
         * Recommended actions:
         * Make sure that Integrity API is enabled in Google Play Console.
         * Ask the user to update Play Store.
         */
        const val API_NOT_AVAILABLE = -1

        /**
         * PackageManager could not find this app.
         *
         * Something is wrong (possibly an attack). Non-actionable.
         */
        const val APP_NOT_INSTALLED = -5

        /**
         * The calling app UID (user id) does not match the one from Package Manager.
         *
         * Something is wrong (possibly an attack). Non-actionable.
         */
        const val APP_UID_MISMATCH = -7

        /**
         * Binding to the service in the Play Store has failed.
         * This can be due to having an old Play Store version installed on the device.
         *
         * Ask the user to update Play Store.
         */
        const val CANNOT_BIND_TO_SERVICE = -9

        /**
         * The provided request hash is too long.
         *
         * The request hash length must be less than 500 bytes.
         * Retry with a shorter request hash.
         * */
        const val REQUEST_HASH_TOO_LONG = -17

        /**
         * There is a transient error on the calling device.
         *
         * Retry with an exponential backoff.
         *
         * Introduced in Integrity Play Core version 1.1.0 (prior versions returned a token with empty Device Integrity Verdict).
         * If the error persists after a few retries, you should assume that the device has failed integrity checks and act accordingly.
         */
        const val CLIENT_TRANSIENT_ERROR = -18

        /**
         * The StandardIntegrityTokenProvider is invalid (e.g. it is outdated).
         *
         * Request a new integrity token provider by calling StandardIntegrityManager#prepareIntegrityToken.
         * */
        const val INTEGRITY_TOKEN_PROVIDER_INVALID = -19

        /**
         * The provided cloud project number is invalid.
         *
         * Use the cloud project number which can be found in Project info in
         * your Google Cloud Console for the cloud project where Play Integrity API is enabled.
         */
        const val CLOUD_PROJECT_NUMBER_IS_INVALID = -16

        /**
         * Unknown internal Google server error.
         *
         * Retry with an exponential backoff. Consider filing a bug if fails consistently.
         */
        const val GOOGLE_SERVER_UNAVAILABLE = -12

        /**
         * Unknown error processing integrity request.
         *
         * Retry with an exponential backoff. Consider filing a bug if fails consistently.
         */
        const val INTERNAL_ERROR = -100

        /**
         * Network error: unable to obtain integrity details.
         *
         * Ask the user to check for a connection.
         */
        const val NETWORK_ERROR = -3

        /**
         * Nonce is not encoded as a base64 web-safe no-wrap string.
         *
         * Retry with correct nonce format.
         */
        const val NONCE_IS_NOT_BASE64 = -13

        /**
         * Nonce length is too long. The nonce must be less than 500 bytes before base64 encoding.
         *
         * Retry with a shorter nonce.
         */
        const val NONCE_TOO_LONG = -11

        /**
         * Nonce length is too short. The nonce must be a minimum of 16 bytes (before base64 encoding) to allow for a better security.
         *
         * Retry with a longer nonce.
         */
        const val NONCE_TOO_SHORT = -10

        /**
         * No error.
         *
         * This is the default value.
         */
        const val NO_ERROR = 0

        /**
         * Google Play Services is not available or version is too old.
         *
         * Ask the user to Install or Update Play Services.
         */
        const val PLAY_SERVICES_NOT_FOUND = -6

        /**
         * The Play Services needs to be updated.
         *
         * Ask the user to update Google Play Services.
         */
        const val PLAY_SERVICES_VERSION_OUTDATED = -15

        /**
         * No active account found in the Play Store app.
         * Note that the Play Integrity API now supports unauthenticated requests.
         * This error code is used only for older Play Store versions that lack support.
         *
         * Ask the user to authenticate in Play Store.
         */
        const val PLAY_STORE_ACCOUNT_NOT_FOUND = -4

        /**
         * The Play Store app is either not installed or not the official version.
         *
         * Ask the user to install an official and recent version of Play Store.
         */
        const val PLAY_STORE_NOT_FOUND = -2

        /**
         * The Play Store needs to be updated.
         *
         * Ask the user to update the Google Play Store.
         */
        const val PLAY_STORE_VERSION_OUTDATED = -14

        /**
         * The calling app is making too many requests to the API and hence is throttled.
         *
         * Retry with an exponential backoff.
         */
        const val TOO_MANY_REQUESTS = -8
    }
}