package com.google.android.play.core.integrity.model;

import androidx.annotation.IntDef;

import org.microg.gms.common.PublicApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
        StandardIntegrityErrorCode.NO_ERROR,
        StandardIntegrityErrorCode.API_NOT_AVAILABLE,
        StandardIntegrityErrorCode.PLAY_STORE_NOT_FOUND,
        StandardIntegrityErrorCode.NETWORK_ERROR,
        StandardIntegrityErrorCode.APP_NOT_INSTALLED,
        StandardIntegrityErrorCode.PLAY_SERVICES_NOT_FOUND,
        StandardIntegrityErrorCode.APP_UID_MISMATCH,
        StandardIntegrityErrorCode.TOO_MANY_REQUESTS,
        StandardIntegrityErrorCode.CANNOT_BIND_TO_SERVICE,
        StandardIntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE,
        StandardIntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED,
        StandardIntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED,
        StandardIntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID,
        StandardIntegrityErrorCode.REQUEST_HASH_TOO_LONG,
        StandardIntegrityErrorCode.INTERNAL_ERROR,
})
@PublicApi
public @interface StandardIntegrityErrorCode {
    /**
     * Standard Integrity API is not available. 
     * Standard Integrity API is not yet available due to the Play Store version being too old. 
     * <ul>
     *   Recommended actions: 
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
     * Binding to the service in the Play Store has failed. This can be due to having an old Play Store version installed on the device or device memory is overloaded. 
     * Ask the user to update Play Store. 
     * Retry with an exponential backoff.
     */
    int CANNOT_BIND_TO_SERVICE = -9;

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
     * The provided request hash is too long. The request hash length must be less than 500 characters. 
     * Retry with a shorter request hash.
     */
    int REQUEST_HASH_TOO_LONG = -17;

    /**
     * The calling app is making too many requests to the API and hence is throttled. 
     * Retry with an exponential backoff.
     */
    int TOO_MANY_REQUESTS = -8;
}
