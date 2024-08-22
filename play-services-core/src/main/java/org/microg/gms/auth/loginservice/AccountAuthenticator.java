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

package org.microg.gms.auth.loginservice;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.R;

import com.google.android.gms.common.internal.CertData;
import org.microg.gms.auth.*;
import org.microg.gms.auth.login.LoginActivity;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.utils.ExtendedPackageInfo;
import org.microg.gms.utils.PackageManagerUtilsKt;

import java.util.Arrays;
import java.util.List;

import static android.accounts.AccountManager.*;

public class AccountAuthenticator extends AbstractAccountAuthenticator {
    private static final String TAG = "GmsAuthenticator";
    public static final String KEY_OVERRIDE_PACKAGE = "overridePackage";
    public static final String KEY_OVERRIDE_CERTIFICATE = "overrideCertificate";
    private final Context context;
    private final String accountType;

    public AccountAuthenticator(Context context) {
        super(context);
        this.context = context;
        this.accountType = AuthConstants.DEFAULT_ACCOUNT_TYPE;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        Log.d(TAG, "editProperties: " + accountType);
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        if (accountType.equals(this.accountType)) {
            final Intent i = new Intent(context, LoginActivity.class);
            i.putExtras(options);
            i.putExtra(LoginActivity.EXTRA_TMPL, LoginActivity.TMPL_NEW_ACCOUNT);
            i.putExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            final Bundle result = new Bundle();
            result.putParcelable(KEY_INTENT, i);
            return result;
        }
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "confirmCredentials: " + account + ", " + options);
        return null;
    }

    public boolean isPackageOverrideAllowed(Account account, String requestingPackage, String overridePackage, CertData overrideCertificate) {
        // Always allow for self package
        if (requestingPackage.equals(context.getPackageName())) return true;
//        if (requestingPackage.equals("org.microg.example.authwithoverride")) return true;
        String requestingDigestString = PackageManagerUtilsKt.toHexString(PackageManagerUtilsKt.digest(PackageManagerUtilsKt.getCertificates(context.getPackageManager(), requestingPackage).get(0), "SHA-256"), "");
        String overrideCertificateDigestString = PackageManagerUtilsKt.toHexString(PackageManagerUtilsKt.digest(overrideCertificate, "SHA-256"), "");
        String overrideUserDataKey = "override." + requestingPackage + ":" + requestingDigestString + ":" + overridePackage + ":" + overrideCertificateDigestString;
        String hasOverride = AccountManager.get(context).getUserData(account, overrideUserDataKey);
        return "1".equals(hasOverride);
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        options.keySet();
        Log.d(TAG, "getAuthToken: " + account + ", " + authTokenType + ", " + options);
        String app = options.getString(KEY_ANDROID_PACKAGE_NAME);
        app = PackageUtils.getAndCheckPackage(context, app, options.getInt(KEY_CALLER_UID), options.getInt(KEY_CALLER_PID));
        AuthManager authManager;
        if (app == null) {
            Bundle result = new Bundle();
            result.putInt(KEY_ERROR_CODE, ERROR_CODE_BAD_REQUEST);
            return result;
        }
        if (options.containsKey(KEY_OVERRIDE_PACKAGE) || options.containsKey(KEY_OVERRIDE_CERTIFICATE)) {
            String overridePackage = options.getString(KEY_OVERRIDE_PACKAGE, app);
            byte[] overrideCertificateBytes = options.getByteArray(KEY_OVERRIDE_CERTIFICATE);
            CertData overrideCert;
            if (overrideCertificateBytes != null) {
                overrideCert = new CertData(overrideCertificateBytes);
            } else {
                overrideCert = PackageManagerUtilsKt.getCertificates(context.getPackageManager(), app).get(0);
            }
            if (isPackageOverrideAllowed(account, app, overridePackage, overrideCert)) {
                authManager = new AuthManager(context, account.name, overridePackage, authTokenType);
                authManager.setPackageSignature(PackageManagerUtilsKt.toHexString(PackageManagerUtilsKt.digest(overrideCert, "SHA1"), ""));
            } else {
                Bundle result = new Bundle();
                Intent i = new Intent(context, AskPackageOverrideActivity.class);
                i.putExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
                i.putExtra(KEY_ANDROID_PACKAGE_NAME, app);
                i.putExtra(KEY_ACCOUNT_TYPE, account.type);
                i.putExtra(KEY_ACCOUNT_NAME, account.name);
                i.putExtra(KEY_OVERRIDE_PACKAGE, overridePackage);
                i.putExtra(KEY_OVERRIDE_CERTIFICATE, overrideCert.getBytes());
                result.putParcelable(KEY_INTENT, i);
                return result;
            }
        } else {
            authManager = new AuthManager(context, account.name, app, authTokenType);
        }
        try {
            AuthResponse res = authManager.requestAuth(true);
            if (res.auth != null) {
                Log.d(TAG, "getAuthToken: " + res.auth);
                Bundle result = new Bundle();
                result.putString(KEY_ACCOUNT_TYPE, account.type);
                result.putString(KEY_ACCOUNT_NAME, account.name);
                result.putString(KEY_AUTHTOKEN, res.auth);
                return result;
            } else {
                Bundle result = new Bundle();
                Intent i = new Intent(context, AskPermissionActivity.class);
                i.putExtras(options);
                i.putExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
                i.putExtra(KEY_ANDROID_PACKAGE_NAME, app);
                i.putExtra(KEY_ACCOUNT_TYPE, account.type);
                i.putExtra(KEY_ACCOUNT_NAME, account.name);
                i.putExtra(KEY_AUTHTOKEN, authTokenType);
                try {
                    if (res.consentDataBase64 != null)
                        i.putExtra(AskPermissionActivity.EXTRA_CONSENT_DATA, Base64.decode(res.consentDataBase64, Base64.URL_SAFE));
                } catch (Exception e) {
                    Log.w(TAG, "Can't decode consent data: ", e);
                }
                result.putParcelable(KEY_INTENT, i);
                return result;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        Log.d(TAG, "getAuthTokenLabel: " + authTokenType);
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "updateCredentials: " + account + ", " + authTokenType + ", " + options);
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        AccountManager accountManager = AccountManager.get(context);
        String services = accountManager.getUserData(account, "services");
        boolean res = true;
        if (services != null) {
            List<String> servicesList = Arrays.asList(services.split(","));
            for (String feature : features) {
                if (feature.startsWith("service_") && !servicesList.contains(feature.substring(8))) {
                    Log.d(TAG, "Feature " + feature + " not supported");
                    res = false;
                }
            }
        } else {
            res = false;
        }
        Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, res);
        return result;
    }
}
