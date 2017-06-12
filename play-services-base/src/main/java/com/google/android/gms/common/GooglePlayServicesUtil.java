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
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import org.microg.gms.common.Constants;
import org.microg.gms.common.PublicApi;

/**
 * Utility class for verifying that the Google Play services APK is available and up-to-date on
 * this device. The same checks are performed if one uses {@link AdvertisingIdClient} or
 * {@link GoogleAuthUtil} to connect to the service.
 * <p/>
 * TODO: methods :)
 */
@PublicApi
public class GooglePlayServicesUtil {
    private static final String TAG = "GooglePlayServicesUtil";

    public static final String GMS_ERROR_DIALOG = "GooglePlayServicesErrorDialog";

    /**
     * Package name for Google Play services.
     */
    @Deprecated
    public static final String GOOGLE_PLAY_SERVICES_PACKAGE = Constants.GMS_PACKAGE_NAME;

    /**
     * Google Play services client library version (declared in library's AndroidManifest.xml android:versionCode).
     */
    @Deprecated
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = Constants.MAX_REFERENCE_VERSION;

    /**
     * Package name for Google Play Store.
     */
    public static final String GOOGLE_PLAY_STORE_PACKAGE = "com.android.vending";

    /**
     * Returns a dialog to address the provided errorCode. The returned dialog displays a localized
     * message about the error and upon user confirmation (by tapping on dialog) will direct them
     * to the Play Store if Google Play services is out of date or missing, or to system settings
     * if Google Play services is disabled on the device.
     *
     * @param errorCode   error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                    If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param activity    parent activity for creating the dialog, also used for identifying
     *                    language to display dialog in.
     * @param requestCode The requestCode given when calling startActivityForResult.
     */
    @Deprecated
    public static Dialog getErrorDialog(int errorCode, Activity activity, int requestCode) {
        return getErrorDialog(errorCode, activity, requestCode, null);
    }

    /**
     * Returns a dialog to address the provided errorCode. The returned dialog displays a localized
     * message about the error and upon user confirmation (by tapping on dialog) will direct them
     * to the Play Store if Google Play services is out of date or missing, or to system settings
     * if Google Play services is disabled on the device.
     *
     * @param errorCode      error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                       If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param activity       parent activity for creating the dialog, also used for identifying
     *                       language to display dialog in.
     * @param requestCode    The requestCode given when calling startActivityForResult.
     * @param cancelListener The {@link DialogInterface.OnCancelListener} to invoke if the dialog
     *                       is canceled.
     */
    @Deprecated
    public static Dialog getErrorDialog(int errorCode, Activity activity, int requestCode, DialogInterface.OnCancelListener cancelListener) {
        return GoogleApiAvailability.getInstance().getErrorDialog(activity, errorCode, requestCode, cancelListener);
    }

    /**
     * Returns a PendingIntent to address the provided errorCode. It will direct them to one of the
     * following places to either the Play Store if Google Play services is out of date or missing,
     * or system settings if Google Play services is disabled on the device.
     *
     * @param errorCode   error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                    If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param activity    parent context for creating the PendingIntent.
     * @param requestCode The requestCode given when calling startActivityForResult.
     */
    @Deprecated
    public static PendingIntent getErrorPendingIntent(int errorCode, Activity activity,
                                                      int requestCode) {
        return null; // TODO
    }

    /**
     * Returns a human-readable string of the error code returned from {@link #isGooglePlayServicesAvailable(Context)}.
     */
    @Deprecated
    public static String getErrorString(int errorCode) {
        return null; // TODO
    }

    /**
     * Returns the open source software license information for the Google Play services
     * application, or null if Google Play services is not available on this device.
     */
    @Deprecated
    public static String getOpenSourceSoftwareLicenseInfo(Context context) {
        return null; // TODO
    }

    /**
     * This gets the Context object of the Buddy APK. This loads the Buddy APK code from the Buddy
     * APK into memory. This returned context can be used to create classes and obtain resources
     * defined in the Buddy APK.
     *
     * @return The Context object of the Buddy APK or null if the Buddy APK is not installed on the device.
     */
    public static Context getRemoteContext(Context context) {
        return null; // TODO
    }

    /**
     * This gets the Resources object of the Buddy APK.
     *
     * @return The Resources object of the Buddy APK or null if the Buddy APK is not installed on the device.
     */
    public static Resources getRemoteResources(Context context) {
        return null; // TODO
    }

    /**
     * Verifies that Google Play services is installed and enabled on this device, and that the
     * version installed on this device is no older than the one required by this client.
     *
     * @return status code indicating whether there was an error. Can be one of following in
     * {@link ConnectionResult}: SUCCESS, SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED,
     * SERVICE_DISABLED, SERVICE_INVALID
     */
    @Deprecated
    public static int isGooglePlayServicesAvailable(Context context) {
        Log.d(TAG, "As we can't know right now if the later desired feature is available, " +
                "we just pretend it to be.");
        return ConnectionResult.SUCCESS;
    }

    @Deprecated
    public static boolean isGoogleSignedUid(PackageManager packageManager, int uid) {
        return false; // TODO
    }

    /**
     * Determines whether an error is user-recoverable. If true, proceed by calling
     * {@link #getErrorDialog(int, Activity, int)} and showing the dialog.
     *
     * @param errorCode error code returned by {@link #isGooglePlayServicesAvailable(Context)}, or
     *                  returned to your application via {@link com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener#onConnectionFailed(ConnectionResult)}
     * @return true if the error is recoverable with {@link #getErrorDialog(int, Activity, int)}
     */
    @Deprecated
    public static boolean isUserRecoverableError(int errorCode) {
        return false; // TODO
    }

    /**
     * Display a DialogFragment for an error code returned by {@link #isGooglePlayServicesAvailable(Context)}.
     *
     * @param errorCode   error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                    If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param activity    parent activity for creating the dialog, also used for identifying
     *                    language to display dialog in.
     * @param requestCode The requestCode given when calling startActivityForResult.
     * @return true if the dialog is shown, false otherwise
     * @throws RuntimeException if API level is below 11 and activity is not a {@link android.support.v4.app.FragmentActivity}.
     */
    @Deprecated
    public static boolean showErrorDialogFragment(int errorCode, Activity activity, int requestCode) {
        return showErrorDialogFragment(errorCode, activity, requestCode, null);
    }

    @Deprecated
    public static boolean showErrorDialogFragment(int errorCode, Activity activity, Fragment fragment, int requestCode, DialogInterface.OnCancelListener cancelListener) {
        return false; // TODO
    }

    /**
     * @param errorCode      error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                       If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param activity       parent activity for creating the dialog, also used for identifying
     *                       language to display dialog in.
     * @param requestCode    The requestCode given when calling startActivityForResult.
     * @param cancelListener The {@link DialogInterface.OnCancelListener} to invoke if the dialog
     *                       is canceled.
     * @return true if the dialog is shown, false otherwise.
     * @throws RuntimeException if API level is below 11 and activity is not a {@link android.support.v4.app.FragmentActivity}.
     */
    @Deprecated
    public static boolean showErrorDialogFragment(int errorCode, Activity activity, int requestCode, DialogInterface.OnCancelListener cancelListener) {
        return showErrorDialogFragment(errorCode, activity, null, requestCode, cancelListener);
    }

    /**
     * Displays a notification relevant to the provided error code. This method is similar to
     * {@link #getErrorDialog(int, android.app.Activity, int)}, but is provided for background
     * tasks that cannot or shouldn't display dialogs.
     *
     * @param errorCode error code returned by {@link #isGooglePlayServicesAvailable(Context)} call.
     *                  If errorCode is {@link ConnectionResult#SUCCESS} then null is returned.
     * @param context   used for identifying language to display dialog in as well as accessing the
     *                  {@link android.app.NotificationManager}.
     */
    @Deprecated
    public static void showErrorNotification(int errorCode, Context context) {
        // TODO
    }
}
