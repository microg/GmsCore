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

package org.microg.gms.auth.loginservice;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.R;

import org.microg.gms.auth.AskPermissionActivity;
import org.microg.gms.auth.AuthManager;
import org.microg.gms.auth.AuthRequest;
import org.microg.gms.auth.AuthResponse;
import org.microg.gms.auth.login.LoginActivity;
import org.microg.gms.common.Utils;

import java.io.IOException;
import java.util.Arrays;

public class GoogleLoginService extends Service {
    private static final String TAG = "GmsAuthLoginSvc";

    private String accountType;

    @Override
    public void onCreate() {
        super.onCreate();
        accountType = getString(R.string.google_account_type);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            return new AbstractAccountAuthenticator(this) {
                @Override
                public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
                    Log.d(TAG, "editProperties: " + accountType);
                    return null;
                }

                @Override
                public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
                    if (accountType.equals(GoogleLoginService.this.accountType)) {
                        return GoogleLoginService.this.addAccount(response, authTokenType, requiredFeatures, options);
                    }
                    return null;
                }

                @Override
                public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
                    Log.d(TAG, "confirmCredentials: " + account + ", " + options);
                    return null;
                }

                @Override
                public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
                    return GoogleLoginService.this.getAuthToken(response, account, authTokenType, options);
                }

                @Override
                public String getAuthTokenLabel(String authTokenType) {
                    Log.d(TAG, "getAuthTokenLabel: " + authTokenType);
                    return null;
                }

                @Override
                public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
                    return null;
                }

                @Override
                public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
                    Log.d(TAG, "hasFeatures: " + account + ", " + Arrays.toString(features));
                    Bundle result = new Bundle();
                    result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
                    return result;
                }
            }.getIBinder();
        }
        return null;
    }

    private Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
        options.keySet();
        Log.d(TAG, "getAuthToken: " + account + ", " + authTokenType + ", " + options);
        String app = options.getString(AccountManager.KEY_ANDROID_PACKAGE_NAME);
        Utils.checkPackage(this, app, options.getInt(AccountManager.KEY_CALLER_UID), options.getInt(AccountManager.KEY_CALLER_UID));
        String appSignature = Utils.getFirstPackageSignatureDigest(this, app);
        if (!AuthManager.isPermitted(this, account, app, appSignature, authTokenType)) {
            Bundle result = new Bundle();
            Intent i = new Intent(this, AskPermissionActivity.class);
            i.putExtras(options);
            i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            i.putExtra(AccountManager.KEY_ANDROID_PACKAGE_NAME, app);
            i.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            i.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
            i.putExtra(AccountManager.KEY_AUTHTOKEN, authTokenType);
            result.putParcelable(AccountManager.KEY_INTENT, i);
            return result;
        }
        String token = AuthManager.getToken(this, account, app, appSignature, authTokenType);
        if (token == null) {
            try {
                AuthRequest request = new AuthRequest().fromContext(this)
                        .email(account.name)
                        .token(AccountManager.get(this).getPassword(account))
                        .service(authTokenType)
                        .app(app, appSignature)
                        .callerIsGms()
                        .calledFromAccountManager()
                        .hasPermission();
                AuthResponse r = request.getResponse();
                AuthManager.storeResponse(this, account, app, appSignature, authTokenType, r);
                token = r.auth;
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_AUTHTOKEN, token);
        return result;
    }

    private Bundle addAccount(AccountAuthenticatorResponse response, String authTokenType, String[] requiredFeatures, Bundle options) {
        final Intent i = new Intent(GoogleLoginService.this, LoginActivity.class);
        i.putExtras(options);
        i.putExtra(LoginActivity.EXTRA_TMPL, LoginActivity.TMPL_NEW_ACCOUNT);
        i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle result = new Bundle();
        result.putParcelable(AccountManager.KEY_INTENT, i);
        return result;
    }
}
