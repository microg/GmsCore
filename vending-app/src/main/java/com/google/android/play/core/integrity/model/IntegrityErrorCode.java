package com.google.android.play.core.integrity.model;

import androidx.annotation.IntDef;

import org.microg.gms.common.PublicApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
        IntegrityErrorCode.NO_ERROR,
        IntegrityErrorCode.API_NOT_AVAILABLE,
        IntegrityErrorCode.PLAY_STORE_NOT_FOUND,
        IntegrityErrorCode.NETWORK_ERROR,
        IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND,
        IntegrityErrorCode.APP_NOT_INSTALLED,
        IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND,
        IntegrityErrorCode.APP_UID_MISMATCH,
        IntegrityErrorCode.TOO_MANY_REQUESTS,
        IntegrityErrorCode.CANNOT_BIND_TO_SERVICE,
        IntegrityErrorCode.NONCE_TOO_SHORT,
        IntegrityErrorCode.NONCE_TOO_LONG,
        IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE,
        IntegrityErrorCode.NONCE_IS_NOT_BASE64,
        IntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED,
        IntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED,
        IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID,
        IntegrityErrorCode.CLIENT_TRANSIENT_ERROR,
        IntegrityErrorCode.INTERNAL_ERROR,
})
@PublicApi
public @interface IntegrityErrorCode {
    /**
     * Integrity API is not available.
     * Integrity API is not enabled, or the Play Store version might be old.
     * <ul>
     *   Recommended actions:
     *  <li>Make sure that Integrity API is enabled in Google Play Console. </li>
     *  <li>Ask the user to update Play Store. </li>
     * </ul>
     */
    int API_NOT_AVAILABLE = -1;

    /**
     * The calling app is not installed.
     * Something is wrong (possibly an attack). Non-actionable.
     */
    int APP_NOT_INSTALLED = -5;

    /**
     * The calling app UID (user id) does not match the one from Package Manager.
     * Something is wrong (possibly an attack). Non-actionable.
     */
    int APP_UID_MISMATCH = -7;

    /**
     * Binding to the service in the Play Store has failed. This can be due to having an old Play Store version installed on the device.
     * Ask the user to update Play Store.
     */
    int CANNOT_BIND_TO_SERVICE = -9;

    /**
     * There was a transient error in the client device.
     * Retry with an exponential backoff.
     * Introduced in Integrity Play Core version 1.1.0 (prior versions returned a token with empty Device Integrity Verdict). If the error persists after a few retries, you should assume that the device has failed integrity checks and act accordingly.
     */
    int CLIENT_TRANSIENT_ERROR = -17;

    /**
     * The provided cloud project number is invalid.
     * Use the cloud project number which can be found in Project info in your Google Cloud Console for the cloud project where Play Integrity API is enabled.
     */
    int CLOUD_PROJECT_NUMBER_IS_INVALID = -16;

    /**
     * Unknown internal Google server error.
     * Retry with an exponential backoff. Consider filing a bug if fails consistently.
     */
    int GOOGLE_SERVER_UNAVAILABLE = -12;

    /**
     * Unknown internal error.
     * Retry with an exponential backoff. Consider filing a bug if fails consistently.
     */
    int INTERNAL_ERROR = -100;

    /**
     * No available network is found.
     * Ask the user to check for a connection.
     */
    int NETWORK_ERROR = -3;

    /**
     * Nonce is not encoded as a base64 web-safe no-wrap string.
     * Retry with correct nonce format.
     */
    int NONCE_IS_NOT_BASE64 = -13;

    /**
     * Nonce length is too long. The nonce must be less than 500 bytes before base64 encoding.
     * Retry with a shorter nonce.
     */
    int NONCE_TOO_LONG = -11;

    /**
     * Nonce length is too short. The nonce must be a minimum of 16 bytes (before base64 encoding) to allow for a better security.
     * Retry with a longer nonce.
     */
    int NONCE_TOO_SHORT = -10;

    int NO_ERROR = 0;

    /**
     * Play Services is not available or version is too old.
     * Ask the user to Install or Update Play Services.
     */
    int PLAY_SERVICES_NOT_FOUND = -6;

    /**
     * Play Services needs to be updated.
     * Ask the user to update Google Play Services.
     */
    int PLAY_SERVICES_VERSION_OUTDATED = -15;

    /**
     * No Play Store account is found on device. Note that the Play Integrity API now supports unauthenticated requests. This error code is used only for older Play Store versions that lack support.
     * Ask the user to authenticate in Play Store.
     */
    int PLAY_STORE_ACCOUNT_NOT_FOUND = -4;

    /**
     * No Play Store app is found on device or not official version is installed.
     * Ask the user to install an official and recent version of Play Store.
     */
    int PLAY_STORE_NOT_FOUND = -2;

    /**
     * The Play Store needs to be updated.
     * Ask the user to update the Google Play Store.
     */
    int PLAY_STORE_VERSION_OUTDATED = -14;

    /**
     * The calling app is making too many requests to the API and hence is throttled.
     * Retry with an exponential backoff.
     */
    int TOO_MANY_REQUESTS = -8;
}
