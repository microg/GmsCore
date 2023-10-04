/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.licensing;

import static com.android.vending.licensing.LicenseChecker.LICENSED;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.microg.gms.auth.AuthConstants;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import kotlin.Unit;

public class LicensingService extends Service {
    private static final String TAG = "FakeLicenseService";
    private RequestQueue queue;
    private AccountManager accountManager;
    private LicenseServiceNotificationRunnable notificationRunnable;

    private static final String KEY_V2_RESULT_JWT = "LICENSE_DATA";


    private final ILicensingService.Stub mLicenseService = new ILicensingService.Stub() {


        @Override
        public void checkLicense(long nonce, String packageName, ILicenseResultListener listener) throws RemoteException {
            Log.v(TAG, "checkLicense(" + nonce + ", " + packageName + ")");

            Account[] accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE);
            PackageManager packageManager = getPackageManager();

            if (accounts.length == 0) {
                handleNoAccounts(packageName, packageManager);
            } else {
                checkLicense(nonce, packageName, packageManager, listener, new LinkedList<>(Arrays.asList(accounts)));
            }
        }

        private void checkLicense(long nonce, String packageName, PackageManager packageManager,
                                  ILicenseResultListener listener, Queue<Account> remainingAccounts) throws RemoteException {
            new LicenseChecker.V1().checkLicense(
                remainingAccounts.poll(), accountManager, packageName, packageManager,
                queue, nonce,
                (responseCode, stringTuple) -> {
                    if (responseCode != LICENSED && !remainingAccounts.isEmpty()) {
                        checkLicense(nonce, packageName, packageManager, listener, remainingAccounts);
                    } else {
                        listener.verifyLicense(responseCode, stringTuple.a, stringTuple.b);
                    }
                }
            );
        }

        @Override
        public void checkLicenseV2(String packageName, ILicenseV2ResultListener listener, Bundle extraParams) throws RemoteException {
            Log.v(TAG, "checkLicenseV2(" + packageName + ", " + extraParams + ")");

            Account[] accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE);
            PackageManager packageManager = getPackageManager();

            if (accounts.length == 0) {
                handleNoAccounts(packageName, packageManager);
            } else {
                checkLicenseV2(packageName, packageManager, listener, extraParams, new LinkedList<>(Arrays.asList(accounts)));
            }
        }

        private void checkLicenseV2(String packageName, PackageManager packageManager,
                                    ILicenseV2ResultListener listener, Bundle extraParams,
                                    Queue<Account> remainingAccounts) throws RemoteException {
            new LicenseChecker.V2().checkLicense(
                remainingAccounts.poll(), accountManager, packageName, packageManager, queue, Unit.INSTANCE,
                (responseCode, data) -> {
                    /*
                     * Suppress failures on V2. V2 is commonly used by free apps whose checker
                     * will not throw users out of the app if it never receives a response.
                     *
                     * This means that users who are signed in to a Google account will not
                     * get a worse experience in these apps than users that are not signed in.
                     */
                    if (responseCode == LICENSED) {
                        Bundle bundle = new Bundle();
                        bundle.putString(KEY_V2_RESULT_JWT, data);

                        listener.verifyLicense(responseCode, bundle);
                    } else if (!remainingAccounts.isEmpty()) {
                        checkLicenseV2(packageName, packageManager, listener, extraParams, remainingAccounts);
                    } else {
                        Log.i(TAG, "Suppressed negative license result for package " + packageName);
                    }
                }
            );

        }

        private void handleNoAccounts(String packageName, PackageManager packageManager) {
            try {
                Log.e(TAG, "not checking license, as user is not signed in");
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                notificationRunnable.callerUid = packageInfo.applicationInfo.uid;
                notificationRunnable.callerAppName = packageManager.getApplicationLabel(packageInfo.applicationInfo);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "ignored license request, but package name " + packageName + " was not known!");
                notificationRunnable.callerAppName = packageName;
            }
            notificationRunnable.run();
        }
    };

    public IBinder onBind(Intent intent) {
        queue = Volley.newRequestQueue(this);
        accountManager = AccountManager.get(this);
        notificationRunnable = new LicenseServiceNotificationRunnable(this);

        return mLicenseService;
    }


}
