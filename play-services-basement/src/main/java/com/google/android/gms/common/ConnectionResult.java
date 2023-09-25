/*
 * SPDX-FileCopyrightText: 2016 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Parcel;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Arrays;

/**
 * Contains all possible error codes for when a client fails to connect to Google Play services.
 * These error codes are used by {@link GoogleApiClient.OnConnectionFailedListener}.
 */
@SafeParcelable.Class
public class ConnectionResult extends AbstractSafeParcelable {
    /**
     * The connection was successful.
     */
    public static final int SUCCESS = 0;
    /**
     * Google Play services is missing on this device. The calling activity should pass this error
     * code to {@link GooglePlayServicesUtil#getErrorDialog(int, Activity, int)} to get a localized
     * error dialog that will resolve the error when shown.
     */
    public static final int SERVICE_MISSING = 1;
    /**
     * The installed version of Google Play services is out of date. The calling activity should
     * pass this error code to {@link GooglePlayServicesUtil#getErrorDialog(int, Activity, int)} to
     * get a localized error dialog that will resolve the error when shown.
     */
    public static final int SERVICE_VERSION_UPDATE_REQUIRED = 2;
    /**
     * The installed version of Google Play services has been disabled on this device. The calling
     * activity should pass this error code to
     * {@link GooglePlayServicesUtil#getErrorDialog(int, Activity, int)} to get a localized error
     * dialog that will resolve the error when shown.
     */
    public static final int SERVICE_DISABLED = 3;
    /**
     * The client attempted to connect to the service but the user is not signed in. The client may
     * choose to continue without using the API or it may call
     * {@link #startResolutionForResult(Activity, int)} to prompt the user to sign in. After the
     * sign in activity returns with {@link Activity#RESULT_OK} further attempts to connect should
     * succeed.
     */
    public static final int SIGN_IN_REQUIRED = 4;
    /**
     * The client attempted to connect to the service with an invalid account name specified.
     */
    public static final int INVALID_ACCOUNT = 5;
    /**
     * Completing the connection requires some form of resolution. A resolution will be available
     * to be started with {@link #startResolutionForResult(Activity, int)}. If the result returned
     * is {@link Activity#RESULT_OK}, then further attempts to connect should either complete or
     * continue on to the next issue that needs to be resolved.
     */
    public static final int RESOLUTION_REQUIRED = 6;
    /**
     * A network error occurred. Retrying should resolve the problem.
     */
    public static final int NETWORK_ERROR = 7;
    /**
     * An internal error occurred. Retrying should resolve the problem.
     */
    public static final int INTERNAL_ERROR = 8;
    /**
     * The version of the Google Play services installed on this device is not authentic.
     */
    public static final int SERVICE_INVALID = 9;
    /**
     * The application is misconfigured. This error is not recoverable and will be treated as
     * fatal. The developer should look at the logs after this to determine more actionable
     * information.
     */
    public static final int DEVELOPER_ERROR = 10;
    /**
     * The application is not licensed to the user. This error is not recoverable and will be
     * treated as fatal.
     */
    public static final int LICENSE_CHECK_FAILED = 11;
    /**
     * The client canceled the connection by calling {@link GoogleApiClient#disconnect()}.
     * Only returned by {@link GoogleApiClient#blockingConnect()}.
     */
    public static final int CANCELED = 13;
    /**
     * The timeout was exceeded while waiting for the connection to complete. Only returned by
     * {@link GoogleApiClient#blockingConnect()}.
     */
    public static final int TIMEOUT = 14;
    /**
     * An interrupt occurred while waiting for the connection complete. Only returned by
     * {@link GoogleApiClient#blockingConnect()}.
     */
    public static final int INTERRUPTED = 15;
    /**
     * One of the API components you attempted to connect to is not available. The API will not
     * work on this device, and updating Google Play services will not likely solve the problem.
     * Using the API on the device should be avoided.
     */
    public static final int API_UNAVAILABLE = 16;
    /**
     * The client attempted to connect to the service but the user is not signed in. An error may have occurred when signing in the user and the error can not
     * be recovered with any user interaction. Alternately, the API may have been requested with {@link GoogleApiClient.Builder#addApiIfAvailable(Api, Scope...)}
     * and it may be the case that no required APIs needed authentication, so authentication did not occur.
     * <p>
     * When seeing this error code, there is no resolution for the sign-in failure.
     */
    public static final int SIGN_IN_FAILED = 17;
    /**
     * Google Play service is currently being updated on this device.
     */
    public static final int SERVICE_UPDATING = 18;

    /**
     * Service doesn't have one or more required permissions.
     */
    public static final int SERVICE_MISSING_PERMISSION = 19;
    /**
     * The current user profile is restricted and cannot use authenticated features. (Jelly Bean MR2+ Restricted Profiles for Android tablets)
     */
    public static final int RESTRICTED_PROFILE = 20;
    /**
     * There was a user-resolvable issue connecting to Google Play services, but when attempting to start the resolution, the activity was not found.
     * <p>
     * This can occur when attempting to resolve issues connecting to Google Play services on emulators with Google APIs but not Google Play Store.
     */
    public static final int RESOLUTION_ACTIVITY_NOT_FOUND = 22;
    /**
     * The API being requested is disabled on this device for this application. Trying again at a later time may succeed.
     */
    public static final int API_DISABLED = 23;
    /**
     * The API being requested is disabled for this connection attempt, but may work for other connections.
     */
    public static final int API_DISABLED_FOR_CONNECTION = 24;

    /**
     * The Drive API requires external storage (such as an SD card), but no external storage is
     * mounted. This error is recoverable if the user installs external storage (if none is
     * present) and ensures that it is mounted (which may involve disabling USB storage mode,
     * formatting the storage, or other initialization as required by the device).
     * <p/>
     * This error should never be returned on a device with emulated external storage. On devices
     * with emulated external storage, the emulated "external storage" is always present regardless
     * of whether the device also has removable storage.
     */
    @Deprecated
    public static final int DRIVE_EXTERNAL_STORAGE_REQUIRED = 1500;

    @Field(1)
    int versionCode = 1;
    @Field(value = 2, getterName = "getErrorCode")
    private int statusCode;
    @Field(value = 3, getterName = "getResolution")
    private PendingIntent resolution;
    @Field(value = 4, getterName = "getErrorMessage")
    private String message;

    private ConnectionResult() {
    }

    /**
     * Creates a connection result.
     *
     * @param statusCode The status code.
     */
    public ConnectionResult(int statusCode) {
        this(statusCode, null);
    }

    /**
     * Creates a connection result.
     *
     * @param statusCode The status code.
     * @param resolution A pending intent that will resolve the issue when started, or null.
     */
    public ConnectionResult(int statusCode, PendingIntent resolution) {
        this(statusCode, resolution, getStatusString(statusCode));
    }

    /**
     * Creates a connection result.
     *
     * @param statusCode The status code.
     * @param resolution A pending intent that will resolve the issue when started, or null.
     * @param message    An additional error message for the connection result, or null.
     */
    @Constructor
    public ConnectionResult(@Param(2) int statusCode, @Param(3) PendingIntent resolution, @Param(4) String message) {
        this.statusCode = statusCode;
        this.resolution = resolution;
        this.message = message;
    }

    static String getStatusString(int statusCode) {
        switch (statusCode) {
            case -1:
                return "UNKNOWN";
            case 0:
                return "SUCCESS";
            case 1:
                return "SERVICE_MISSING";
            case 2:
                return "SERVICE_VERSION_UPDATE_REQUIRED";
            case 3:
                return "SERVICE_DISABLED";
            case 4:
                return "SIGN_IN_REQUIRED";
            case 5:
                return "INVALID_ACCOUNT";
            case 6:
                return "RESOLUTION_REQUIRED";
            case 7:
                return "NETWORK_ERROR";
            case 8:
                return "INTERNAL_ERROR";
            case 9:
                return "SERVICE_INVALID";
            case 10:
                return "DEVELOPER_ERROR";
            case 11:
                return "LICENSE_CHECK_FAILED";
            case 13:
                return "CANCELED";
            case 14:
                return "TIMEOUT";
            case 15:
                return "INTERRUPTED";
            case 16:
                return "API_UNAVAILABLE";
            case 17:
                return "SIGN_IN_FAILED";
            case 18:
                return "SERVICE_UPDATING";
            case 19:
                return "SERVICE_MISSING_PERMISSION";
            case 20:
                return "RESTRICTED_PROFILE";
            case 21:
                return "API_VERSION_UPDATE_REQUIRED";
            case 42:
                return "UPDATE_ANDROID_WEAR";
            case 99:
                return "UNFINISHED";
            case 1500:
                return "DRIVE_EXTERNAL_STORAGE_REQUIRED";
            default:
                return "UNKNOWN_ERROR_CODE(" + statusCode + ")";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ConnectionResult)) {
            return false;
        } else {
            ConnectionResult r = (ConnectionResult) o;
            return statusCode == r.statusCode && resolution == null ? r.resolution == null : resolution.equals(r.resolution) && TextUtils.equals(message, r.message);
        }
    }

    /**
     * Indicates the type of error that interrupted connection.
     *
     * @return the error code, or {@link #SUCCESS} if no error occurred.
     */
    public int getErrorCode() {
        return statusCode;
    }

    /**
     * Returns an error message for connection result.
     *
     * @return the message
     */
    public String getErrorMessage() {
        return message;
    }

    /**
     * A pending intent to resolve the connection failure. This intent can be started with
     * {@link Activity#startIntentSenderForResult(IntentSender, int, Intent, int, int, int)} to
     * present UI to solve the issue.
     *
     * @return The pending intent to resolve the connection failure.
     */
    public PendingIntent getResolution() {
        return resolution;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{statusCode, resolution, message});
    }

    /**
     * Returns {@code true} if calling {@link #startResolutionForResult(Activity, int)} will start
     * any intents requiring user interaction.
     *
     * @return {@code true} if there is a resolution that can be started.
     */
    public boolean hasResolution() {
        return statusCode != 0 && resolution != null;
    }

    /**
     * Returns {@code true} if the connection was successful.
     *
     * @return {@code true} if the connection was successful, {@code false} if there was an error.
     */
    public boolean isSuccess() {
        return statusCode == 0;
    }

    /**
     * Resolves an error by starting any intents requiring user interaction. See
     * {@link #SIGN_IN_REQUIRED}, and {@link #RESOLUTION_REQUIRED}.
     *
     * @param activity    An Activity context to use to resolve the issue. The activity's
     *                    {@link Activity#onActivityResult} method will be invoked after the user
     *                    is done. If the resultCode is {@link Activity#RESULT_OK}, the application
     *                    should try to connect again.
     * @param requestCode The request code to pass to {@link Activity#onActivityResult}.
     * @throws IntentSender.SendIntentException If the resolution intent has been canceled or is no
     *                                          longer able to execute the request.
     */
    public void startResolutionForResult(Activity activity, int requestCode) throws
            IntentSender.SendIntentException {
        if (hasResolution()) {
            activity.startIntentSenderForResult(resolution.getIntentSender(), requestCode, null, 0, 0, 0);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ConnectionResult> CREATOR = findCreator(ConnectionResult.class);
}
