/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import org.microg.gms.common.Constants;
import org.microg.gms.common.Utils;

public class AuthManager {

    private static final String TAG = "GmsAuthManager";
    public static final String PERMISSION_TREE_BASE = "com.google.android.googleapps.permission.GOOGLE_AUTH.";

    public static void storeResponse(Context context, Account account, String packageName,
                                     String sig, String service, AuthResponse response) {
        if (service.startsWith("weblogin:")) return;
        AccountManager accountManager = AccountManager.get(context);
        if (response.accountId != null)
            accountManager.setUserData(account, "GoogleUserId", response.accountId);
        if (response.Sid != null)
            accountManager.setAuthToken(account, buildTokenKey(packageName, sig, "SID"), response.Sid);
        if (response.LSid != null)
            accountManager.setAuthToken(account, buildTokenKey(packageName, sig, "LSID"), response.LSid);
        if (response.expiry > 0)
            accountManager.setUserData(account, buildExpireKey(packageName, sig, service), Long.toString(response.expiry));
        if (response.auth != null && response.expiry != 0 && response.storeConsentRemotely) {
            accountManager.setAuthToken(account, buildTokenKey(packageName, sig, service), response.auth);
            accountManager.setUserData(account, buildPermKey(packageName, sig, service), "1");
        }
    }

    public static String getToken(Context context, Account account, String packageName,
                                  String sig, String service) {
        if (service.startsWith("weblogin:")) return null;
        AccountManager accountManager = AccountManager.get(context);
        return accountManager.peekAuthToken(account, buildTokenKey(packageName, sig, service));
    }

    public static boolean isPermitted(Context context, Account account, String packageName,
                                      String sig, String service) {
        if (service.startsWith("audience:server:client_id:")) {
            // https://developers.google.com/accounts/docs/CrossClientAuth
            Log.d(TAG, "Always permitting scope: " + service);
            return true;
        } else if (service.startsWith("weblogin:")) {
            if (Constants.GMS_PACKAGE_SIGNATURE_SHA1.equals(sig)) {
                Log.d(TAG, "Permitting weblogin, is Google singed app!");
                return true;
            }
            return false;
        } else if (!service.startsWith("oauth:") && !service.startsWith("oauth2:")) {
            if (context.getPackageManager().checkPermission(PERMISSION_TREE_BASE + service, packageName) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permitting, permission is present");
                return true;
            }
        }
        AccountManager accountManager = AccountManager.get(context);
        String perm = accountManager.getUserData(account, buildPermKey(packageName, sig, service));
        if (!"1".equals(perm)) {
            Log.d(TAG, "Not permitting, permission not stored for " + packageName + ": " + service);
            return false;
        }
        String exp = accountManager.getUserData(account, buildExpireKey(packageName, sig, service));
        if (exp != null) {
            long expLong = Long.parseLong(exp);
            if (expLong < System.currentTimeMillis() / 1000L) {
                Log.d(TAG, "Permission for " + packageName + " / " + service + " present, but expired");
                return false;
            }
        }
        return true;
    }

    public static void storePermission(Context context, Account account, String packageName, String service) {
        storePermission(context, account, packageName, Utils.getFirstPackageSignatureDigest(context, packageName), service);
    }

    public static void storePermission(Context context, Account account, String packageName,
                                       String sig, String service) {
        AccountManager accountManager = AccountManager.get(context);
        accountManager.setUserData(account, buildPermKey(packageName, sig, service), "1");
    }

    public static String buildTokenKey(String packageName, String sig, String service) {
        if (service.startsWith("weblogin:")) return null;
        return packageName + ":" + sig + ":" + service;
    }

    private static String buildPermKey(String packageName, String sig, String service) {
        return "perm." + packageName + ":" + sig + ":" + service;
    }

    private static String buildExpireKey(String packageName, String sig, String service) {
        return "EXP." + packageName + ":" + sig + ":" + service;
    }
}
