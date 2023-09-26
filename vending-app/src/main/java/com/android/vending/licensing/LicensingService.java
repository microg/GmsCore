/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.licensing;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class LicensingService extends Service {
    private static final String TAG = "FakeLicenseService";
    private RequestQueue queue;

    private static final String KEY_V2_RESULT_JWT = "LICENSE_DATA";

    private final ILicensingService.Stub mLicenseService = new ILicensingService.Stub() {




        /* Possible response codes for checkLicense v1, from
         * https://developer.android.com/google/play/licensing/licensing-reference#server-response-codes and
         * the LVL library.
         */

        /**
         * The application is licensed to the user. The user has purchased the application, or is authorized to
         * download and install the alpha or beta version of the application.
         */
        private static final int LICENSED = 0x0;
        /**
         * The application is licensed to the user, but there is an updated application version available that is
         * signed with a different key.
         */
        private static final int NOT_LICENSED = 0x1;
        /**
         * The application is not licensed to the user.
         */
        private static final int LICENSED_OLD_KEY = 0x2;
        /**
         * Server error — the application (package name) was not recognized by Google Play.
         */
        private static final int ERROR_NOT_MARKET_MANAGED = 0x3;
        /**
         * Server error — the server could not load the application's key pair for licensing.
         */
        private static final int ERROR_SERVER_FAILURE = 0x4;
        private static final int ERROR_OVER_QUOTA = 0x5;

        /**
         * Local error — the Google Play application was not able to reach the licensing server, possibly because
         * of network availability problems.
         */
        private static final int ERROR_CONTACTING_SERVER = 0x101;
        /**
         * Local error — the application requested a license check for a package that is not installed on the device.
         */
        private static final int ERROR_INVALID_PACKAGE_NAME = 0x102;
        /**
         * Local error — the application requested a license check for a package whose UID (package, user ID pair)
         * does not match that of the requesting application.
         */
        private static final int ERROR_NON_MATCHING_UID = 0x103;

        @Override
        public void checkLicense(long nonce, String packageName, ILicenseResultListener listener) throws RemoteException {
            Log.v(TAG, "checkLicense(" + nonce + ", " + packageName + ")");
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
                int versionCode = packageInfo.versionCode;

                // Verify caller identity
                if (packageInfo.applicationInfo.uid != getCallingUid()) {
                    Log.e(TAG, "an app illegally tried to request v1 licenses for another app (caller: " + getCallingUid() + ")");
                    listener.verifyLicense(ERROR_NON_MATCHING_UID, null, null);
                } else {
                    Request request =
                        new LicenseRequest.V1(packageName, versionCode, nonce, data -> {
                            try {
                                if (data != null) {
                                    Log.v(TAG, "licenseV1 result was " + data.result + "with signed data " + data.signedData);

                                    if (data.result != null) {
                                        listener.verifyLicense(data.result, data.signedData, data.signature);
                                    } else {
                                        listener.verifyLicense(LICENSED, data.signedData, data.signature);
                                    }
                                } else {
                                    Log.v(TAG, "licenseV1 result was that user has no license");
                                    listener.verifyLicense(NOT_LICENSED, null, null);
                                }
                            } catch (RemoteException e) {
                                Log.e(TAG, "After returning licenseV1 result, remote threw an Exception.");
                                e.printStackTrace();
                            }
                        }, error -> {
                            Log.e(TAG, "licenseV1 request failed with " + error.toString());
                            try {
                                listener.verifyLicense(ERROR_CONTACTING_SERVER, null, null);
                            } catch (RemoteException e) {
                                Log.e(TAG, "After telling it that licenseV1 had an error when contacting server, remote threw an Exception.");
                                e.printStackTrace();
                                Log.e(TAG, "Caused after network error:");
                                error.printStackTrace();
                            }
                        });

                    request.setShouldCache(false);
                    queue.add(request);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "an app tried to request v1 licenses for package " + packageName + ", which does not exist");
                listener.verifyLicense(ERROR_INVALID_PACKAGE_NAME, null, null);
            }
        }

        @Override
        public void checkLicenseV2(String packageName, ILicenseV2ResultListener listener, Bundle extraParams) throws RemoteException {
            Log.v(TAG, "checkLicenseV2(" + packageName + ", " + extraParams + ")");

            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
                int versionCode = packageInfo.versionCode;

                // Verify caller identity
                if (packageInfo.applicationInfo.uid != getCallingUid()) {
                    Log.e(TAG, "an app illegally tried to request v2 licenses for another app (caller: " + getCallingUid() + ")");
                    listener.verifyLicense(ERROR_NON_MATCHING_UID, new Bundle());
                } else {
                    Request request =
                        new LicenseRequest.V2(packageName, versionCode, jwt -> {
                            Log.v(TAG, "LicenseV2 returned JWT license value " + jwt);
                            Bundle bundle = new Bundle();
                            bundle.putString(KEY_V2_RESULT_JWT, jwt);
                            try {
                                listener.verifyLicense(jwt == null? NOT_LICENSED : LICENSED, bundle);
                            } catch (RemoteException e) {
                                Log.e(TAG, "After returning licenseV2 result, remote threw an Exception.");
                                e.printStackTrace();
                            }
                        }, error -> {
                            Log.e(TAG, "licenseV2 request failed with " + error.toString());
                            try {
                                listener.verifyLicense(ERROR_CONTACTING_SERVER, new Bundle());
                            } catch (RemoteException e) {
                                Log.e(TAG, "After telling it that licenseV2 had an error when contacting server, remote threw an Exception.");
                                e.printStackTrace();
                                Log.e(TAG, "Caused after network error:");
                                error.printStackTrace();
                            }
                        });

                    request.setShouldCache(false);
                    queue.add(request);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "an app tried to request v1 licenses for package " + packageName + ", which does not exist");
                listener.verifyLicense(ERROR_INVALID_PACKAGE_NAME, new Bundle());
            }

        }
    };

    public IBinder onBind(Intent intent) {
        queue = Volley.newRequestQueue(this);
        return mLicenseService;
    }


}
