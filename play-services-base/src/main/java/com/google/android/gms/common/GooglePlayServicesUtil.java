/*
 * Copyright 2013-2015 microG Project Team
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
public class GooglePlayServicesUtil {
    private static final String TAG = "GooglePlayServicesUtil";

    public static final String GMS_ERROR_DIALOG = "GooglePlayServicesErrorDialog";
    public static final String GOOGLE_PLAY_SERVICES_PACKAGE = Constants.GMS_PACKAGE_NAME;
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = Constants.MAX_REFERENCE_VERSION;
    public static final String GOOGLE_PLAY_STORE_PACKAGE = "com.android.vending";

    public static Dialog getErrorDialog(int errorCode, Activity activity, int requestCode) {
        return null; // TODO
    }

    public static Dialog getErrorDialog(int errorCode, Activity activity, int requestCode,
            DialogInterface.OnCancelListener cancelListener) {
        return null; // TODO
    }

    public static PendingIntent getErrorPendingIntent(int errorCode, Activity activity,
            int requestCode) {
        return null; // TODO
    }

    public static String getErrorString(int errorCode) {
        return null; // TODO
    }

    public static String getOpenSourceSoftwareLicenseInfo(Context context) {
        return null; // TODO
    }

    public static Context getRemoteContext(Context context) {
        return null; // TODO
    }

    public static int isGooglePlayServicesAvailable(Context context) {
        Log.d(TAG, "As we can't know right now if the later desired feature is available, " +
                "we just pretend it to be.");
        return ConnectionResult.SUCCESS;
    }

    public static boolean isGoogleSignedUid(PackageManager packageManager, int uid) {
        return false; // TODO
    }

    public static boolean isUserRecoverableError(int errorCode) {
        return false; // TODO
    }

    public static boolean showErrorDialogFragment(int errorCode, Activity activity,
            int requestCode) {
        return false; // TODO
    }

    public static boolean showErrorDialogFragment(int errorCode, Activity activity,
            Fragment fragment, int requestCode, DialogInterface.OnCancelListener cancelListener) {
        return false; // TODO
    }

    public static boolean showErrorDialogFragment(int errorCode, Activity activity, int requestCode,
            DialogInterface.OnCancelListener cancelListener) {
        return false; // TODO
    }

    public static void showErrorNotification(int errorCode, Context context) {
        // TODO
    }
}
