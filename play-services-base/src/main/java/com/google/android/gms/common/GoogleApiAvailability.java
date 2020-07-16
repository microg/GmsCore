/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.common;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.microg.gms.common.Constants;
import org.microg.gms.common.PublicApi;

import static com.google.android.gms.common.ConnectionResult.INTERNAL_ERROR;
import static com.google.android.gms.common.ConnectionResult.INVALID_ACCOUNT;
import static com.google.android.gms.common.ConnectionResult.NETWORK_ERROR;
import static com.google.android.gms.common.ConnectionResult.RESOLUTION_REQUIRED;
import static com.google.android.gms.common.ConnectionResult.SERVICE_DISABLED;
import static com.google.android.gms.common.ConnectionResult.SERVICE_INVALID;
import static com.google.android.gms.common.ConnectionResult.SERVICE_MISSING;
import static com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;
import static com.google.android.gms.common.ConnectionResult.SIGN_IN_REQUIRED;
import static com.google.android.gms.common.ConnectionResult.SUCCESS;

@PublicApi
public class GoogleApiAvailability {
    private static final String TAG = "GmsApiAvailability";

    /**
     * Package name for Google Play services.
     */
    public static final String GOOGLE_PLAY_SERVICES_PACKAGE = Constants.GMS_PACKAGE_NAME;

    /**
     * Google Play services client library version (declared in library's AndroidManifest.xml android:versionCode).
     */
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = Constants.MAX_REFERENCE_VERSION;

    private static GoogleApiAvailability instance;

    private GoogleApiAvailability() {
    }

    /**
     * Returns the singleton instance of GoogleApiAvailability.
     */
    public static GoogleApiAvailability getInstance() {
        if (instance == null) {
            synchronized (GoogleApiAvailability.class) {
                if (instance == null) {
                    instance = new GoogleApiAvailability();
                }
            }
        }
        return instance;
    }


    /**
     * Returns a dialog to address the provided errorCode. The returned dialog displays a localized
     * message about the error and upon user confirmation (by tapping on dialog) will direct them
     * to the Play Store if Google Play services is out of date or missing, or to system settings
     * if Google Play services is disabled on the device.
     *
     * @param activity    parent activity for creating the dialog, also used for identifying language to display dialog in.
     * @param errorCode   error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                    If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param requestCode The requestCode given when calling startActivityForResult.
     */
    public Dialog getErrorDialog(Activity activity, int errorCode, int requestCode) {
        return getErrorDialog(activity, errorCode, requestCode, null);
    }

    /**
     * Returns a dialog to address the provided errorCode. The returned dialog displays a localized
     * message about the error and upon user confirmation (by tapping on dialog) will direct them
     * to the Play Store if Google Play services is out of date or missing, or to system settings
     * if Google Play services is disabled on the device.
     *
     * @param activity       parent activity for creating the dialog, also used for identifying language to display dialog in.
     * @param errorCode      error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                       If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param requestCode    The requestCode given when calling startActivityForResult.
     * @param cancelListener The {@link DialogInterface.OnCancelListener} to invoke if the dialog is canceled.
     */
    public Dialog getErrorDialog(Activity activity, int errorCode, int requestCode, DialogInterface.OnCancelListener cancelListener) {
        // TODO
        return null;
    }

    /**
     * Returns a PendingIntent to address the provided connection failure.
     * <p/>
     * If {@link ConnectionResult#hasResolution()} is true, then {@link ConnectionResult#getResolution()}
     * will be returned. Otherwise, the returned PendingIntent will direct the user to either the
     * Play Store if Google Play services is out of date or missing, or system settings if Google
     * Play services is disabled on the device.
     *
     * @param context parent context for creating the PendingIntent.
     * @param result  the connection failure. If successful or the error is not resolvable by the user, null is returned.
     */
    public PendingIntent getErrorResolutionPendingIntent(Context context, ConnectionResult result) {
        if (result.hasResolution()) {
            return result.getResolution();
        }
        return getErrorResolutionPendingIntent(context, result.getErrorCode(), 0);
    }

    /**
     * Returns a PendingIntent to address the provided errorCode. It will direct the user to either
     * the Play Store if Google Play services is out of date or missing, or system settings if
     * Google Play services is disabled on the device.
     *
     * @param context     parent context for creating the PendingIntent.
     * @param errorCode   error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                    If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param requestCode The requestCode given when calling startActivityForResult.
     */
    public PendingIntent getErrorResolutionPendingIntent(Context context, int errorCode, int requestCode) {
        // TODO
        return null;
    }

    /**
     * Returns a human-readable string of the error code returned from {@link #isGooglePlayServicesAvailable(Context)}.
     */
    public final String getErrorString(int errorCode) {
        return ConnectionResult.getStatusString(errorCode);
    }

    /**
     * Verifies that Google Play services is installed and enabled on this device, and that the
     * version installed on this device is no older than the one required by this client.
     *
     * @return status code indicating whether there was an error. Can be one of following in
     * {@link ConnectionResult}: SUCCESS, SERVICE_MISSING, SERVICE_UPDATING,
     * SERVICE_VERSION_UPDATE_REQUIRED, SERVICE_DISABLED, SERVICE_INVALID
     */
    public int isGooglePlayServicesAvailable(Context context) {
        Log.d(TAG, "As we can't know right now if the later desired feature is available, " +
                "we just pretend it to be.");
        return SUCCESS;
    }

    /**
     * Determines whether an error can be resolved via user action. If true, proceed by calling
     * {@link #getErrorDialog(Activity, int, int)} and showing the dialog.
     *
     * @param errorCode error code returned by {@link #isGooglePlayServicesAvailable(Context)}, or
     *                  returned to your application via {@link OnConnectionFailedListener#onConnectionFailed(ConnectionResult)}
     * @return true if the error is resolvable with {@link #getErrorDialog(Activity, int, int)}
     */
    public final boolean isUserResolvableError(int errorCode) {
        switch (errorCode) {
            case SERVICE_MISSING:
            case SERVICE_VERSION_UPDATE_REQUIRED:
            case SERVICE_DISABLED:
            case SERVICE_INVALID:
                return true;
            case SIGN_IN_REQUIRED:
            case INVALID_ACCOUNT:
            case RESOLUTION_REQUIRED:
            case NETWORK_ERROR:
            case INTERNAL_ERROR:
            default:
                return false;
        }
    }

    /**
     * Attempts to make Google Play services available on this device. If Play Services is already
     * available, the returned {@link Task} may complete immediately.
     * <p/>
     * If it is necessary to display UI in order to complete this request (e.g. sending the user
     * to the Google Play store) the passed {@link Activity} will be used to display this UI.
     * <p/>
     * It is recommended to call this method from {@link Activity#onCreate(Bundle)}.
     * If the passed {@link Activity} completes before the returned {@link Task} completes, the
     * Task will fail with a {@link java.util.concurrent.CancellationException}.
     * <p/>
     * This method must be called from the main thread.
     *
     * @return A {@link Task}. If this Task completes without throwing an exception, Play Services
     * is available on this device.
     */
    public Task<Void> makeGooglePlayServicesAvailable(Activity activity) {
        int status = isGooglePlayServicesAvailable(activity);
        if (status == SUCCESS) {
            return Tasks.forResult(null);
        }
        // TODO
        return Tasks.forResult(null);
    }

    /**
     * Displays a DialogFragment for an error code returned by {@link #isGooglePlayServicesAvailable(Context)}.
     *
     * @param activity    parent activity for creating the dialog, also used for identifying language to display dialog in.
     * @param errorCode   error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                    If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param requestCode The requestCode given when calling startActivityForResult.
     * @return true if the dialog is shown, false otherwise
     * @throws RuntimeException if API level is below 11 and activity is not a {@link FragmentActivity}.
     * @see ErrorDialogFragment
     * @see SupportErrorDialogFragmet
     */
    public boolean showErrorDialogFragment(Activity activity, int errorCode, int requestCode) {
        return showErrorDialogFragment(activity, errorCode, requestCode, null);
    }

    /**
     * Displays a DialogFragment for an error code returned by {@link #isGooglePlayServicesAvailable(Context)}.
     *
     * @param activity       parent activity for creating the dialog, also used for identifying language to display dialog in.
     * @param errorCode      error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                       If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param requestCode    The requestCode given when calling startActivityForResult.
     * @param cancelListener The {@link DialogInterface.OnCancelListener} to invoke if the dialog is canceled.
     * @return true if the dialog is shown, false otherwise
     * @throws RuntimeException if API level is below 11 and activity is not a {@link FragmentActivity}.
     * @see ErrorDialogFragment
     * @see SupportErrorDialogFragmet
     */
    public boolean showErrorDialogFragment(Activity activity, int errorCode, int requestCode, DialogInterface.OnCancelListener cancelListener) {
        Dialog dialog = getErrorDialog(activity, errorCode, requestCode, cancelListener);
        if (dialog == null) {
            return false;
        } else {
            // TODO
            return false;
        }
    }

    /**
     * Displays a notification for an error code returned from
     * {@link #isGooglePlayServicesAvailable(Context)}, if it is resolvable by the user.
     * <p/>
     * This method is similar to {@link #getErrorDialog(int, android.app.Activity, int)}, but is
     * provided for background tasks that cannot or should not display dialogs.
     *
     * @param errorCode error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                  If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param context   used for identifying language to display dialog in as well as accessing the
     *                  {@link android.app.NotificationManager}.
     */
    public void showErrorNotification(Context context, int errorCode) {
        if (errorCode == RESOLUTION_REQUIRED) {
            Log.e(TAG, "showErrorNotification(context, errorCode) is called for RESOLUTION_REQUIRED when showErrorNotification(context, result) should be called");
        }

        if (isUserResolvableError(errorCode)) {
            GooglePlayServicesUtil.showErrorNotification(errorCode, context);
        }
    }

    /**
     * Displays a notification for a connection failure, if it is resolvable by the user.
     *
     * @param context The calling context used to display the notification.
     * @param result  The connection failure. If successful or the error is not resolvable by the
     *                user, no notification is shown.
     */
    public void showErrorNotification(Context context, ConnectionResult result) {
        PendingIntent pendingIntent = getErrorResolutionPendingIntent(context, result);
        if (pendingIntent != null) {
            // TODO
        }
    }
}
