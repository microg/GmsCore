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

package org.microg.gms.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.PendingIntentCompat;

import com.google.android.auth.IAuthManagerService;
import com.google.android.gms.R;
import com.google.android.gms.auth.AccountChangeEventsRequest;
import com.google.android.gms.auth.AccountChangeEventsResponse;
import com.google.android.gms.auth.GetHubTokenInternalResponse;
import com.google.android.gms.auth.GetHubTokenRequest;
import com.google.android.gms.auth.HasCapabilitiesRequest;
import com.google.android.gms.auth.TokenData;
import com.google.android.gms.common.api.Scope;

import org.microg.gms.common.GooglePackagePermission;
import org.microg.gms.common.PackageUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static android.accounts.AccountManager.*;
import static android.os.Build.VERSION.SDK_INT;
import static org.microg.gms.auth.AskPermissionActivity.EXTRA_CONSENT_DATA;

public class AuthManagerServiceImpl extends IAuthManagerService.Stub {
    private static final String TAG = "GmsAuthManagerSvc";

    public static final String KEY_ACCOUNT_FEATURES = "account_features";
    public static final String KEY_AUTHORITY = "authority";
    public static final String KEY_CALLBACK_INTENT = "callback_intent";
    public static final String KEY_CALLER_UID = "callerUid";
    public static final String KEY_ANDROID_PACKAGE_NAME = "androidPackageName";
    public static final String KEY_CLIENT_PACKAGE_NAME = "clientPackageName";
    public static final String KEY_HANDLE_NOTIFICATION = "handle_notification";
    public static final String KEY_REQUEST_ACTIONS = "request_visible_actions";
    public static final String KEY_REQUEST_VISIBLE_ACTIVITIES = "request_visible_actions";
    public static final String KEY_SUPPRESS_PROGRESS_SCREEN = "suppressProgressScreen";
    public static final String KEY_SYNC_EXTRAS = "sync_extras";
    public static final String KEY_DELEGATION_TYPE = "delegation_type";
    public static final String KEY_DELEGATEE_USER_ID = "delegatee_user_id";

    public static final String KEY_ERROR = "Error";
    public static final String KEY_USER_RECOVERY_INTENT = "userRecoveryIntent";

    private final Context context;

    public AuthManagerServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public Bundle getToken(String accountName, String scope, Bundle extras) {
        return getTokenWithAccount(new Account(accountName, AuthConstants.DEFAULT_ACCOUNT_TYPE), scope, extras);
    }

    private List<Scope> getScopes(String scope) {
        if (!scope.startsWith("oauth2:")) return null;
        String[] strings = scope.substring(7).split(" ");
        List<Scope> res = new ArrayList<Scope>();
        for (String string : strings) {
            res.add(new Scope(string));
        }
        return res;
    }

    private static CharSequence getPackageLabel(String packageName, PackageManager pm) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    @Override
    public AccountChangeEventsResponse getChangeEvents(AccountChangeEventsRequest request) {
        return new AccountChangeEventsResponse();
    }

    @Override
    public Bundle getTokenWithAccount(Account account, String scope, Bundle extras) {
        String packageName = extras.getString(KEY_ANDROID_PACKAGE_NAME);
        if (packageName == null || packageName.isEmpty())
            packageName = extras.getString(KEY_CLIENT_PACKAGE_NAME);
        packageName = PackageUtils.getAndCheckCallingPackage(context, packageName, extras.getInt(KEY_CALLER_UID, 0), extras.getInt(KEY_CALLER_PID, 0));
        boolean notify = extras.getBoolean(KEY_HANDLE_NOTIFICATION, false);

        scope = Objects.equals(AuthConstants.SCOPE_OAUTH2, scope) ? AuthConstants.SCOPE_EM_OP_PRO : scope;

        if (!AuthConstants.SCOPE_GET_ACCOUNT_ID.equals(scope))
            Log.d(TAG, "getToken: account:" + account.name + " scope:" + scope + " extras:" + extras + ", notify: " + notify);

        scope = Objects.equals(AuthConstants.SCOPE_OAUTH2, scope) ? AuthConstants.SCOPE_EM_OP_PRO : scope;

        /*
         * TODO: This scope seems to be invalid (according to https://developers.google.com/oauthplayground/),
         * but is used in some applications anyway. Removing it is unlikely a good solution, but works for now.
         */
        scope = scope.replace("https://www.googleapis.com/auth/identity.plus.page.impersonation ", "");

        AuthManager authManager = new AuthManager(context, account.name, packageName, scope);
        if (extras.containsKey(KEY_DELEGATION_TYPE) && extras.getInt(KEY_DELEGATION_TYPE) != 0 ) {
            authManager.setDelegation(extras.getInt(KEY_DELEGATION_TYPE), extras.getString("delegatee_user_id"));
        }
        authManager.setOauth2Foreground(notify ? "0" : "1");
        Bundle result = new Bundle();
        result.putString(KEY_ACCOUNT_NAME, account.name);
        result.putString(KEY_ACCOUNT_TYPE, authManager.getAccountType());
        if (!authManager.accountExists()) {
            result.putString(KEY_ERROR, "NetworkError");
            return result;
        }
        try {
            AuthResponse res = authManager.requestAuthWithBackgroundResolution(false);
            if (res.auth != null) {
                if (!AuthConstants.SCOPE_GET_ACCOUNT_ID.equals(scope))
                    Log.d(TAG, "getToken: " + res);
                result.putString(KEY_AUTHTOKEN, res.auth);
                Bundle details = new Bundle();
                details.putParcelable("TokenData", new TokenData(res.auth, res.expiry, scope.startsWith("oauth2:"), getScopes(res.grantedScopes != null ? res.grantedScopes : scope)));
                result.putBundle("tokenDetails", details);
                result.putString(KEY_ERROR, "OK");
            } else {
                result.putString(KEY_ERROR, "NeedPermission");
                Intent i = new Intent(context, AskPermissionActivity.class);
                i.putExtras(extras);
                i.putExtra(KEY_ANDROID_PACKAGE_NAME, packageName);
                i.putExtra(KEY_ACCOUNT_TYPE, authManager.getAccountType());
                i.putExtra(KEY_ACCOUNT_NAME, account.name);
                i.putExtra(KEY_AUTHTOKEN, scope);
                i.putExtra(KEY_CALLER_UID, getCallingUid());
                i.putExtra(KEY_CALLER_PID, getCallingPid());
                try {
                    if (res.consentDataBase64 != null)
                        i.putExtra(EXTRA_CONSENT_DATA, Base64.decode(res.consentDataBase64, Base64.URL_SAFE));
                } catch (Exception e) {
                    Log.w(TAG, "Can't decode consent data: ", e);
                }
                if (notify) {
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(packageName.hashCode(), new NotificationCompat.Builder(context)
                            .setContentIntent(PendingIntentCompat.getActivity(context, 0, i, 0, false))
                            .setContentTitle(context.getString(R.string.auth_notification_title))
                            .setContentText(context.getString(R.string.auth_notification_content, getPackageLabel(packageName, context.getPackageManager())))
                            .setSmallIcon(android.R.drawable.stat_notify_error)
                            .build());
                }
                result.putParcelable(KEY_USER_RECOVERY_INTENT, i);
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            result.putString(KEY_ERROR, "NetworkError");
        }
        return result;
    }

    @Override
    public Bundle getAccounts(Bundle extras) {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.ACCOUNT);
        String[] accountFeatures = extras.getStringArray(KEY_ACCOUNT_FEATURES);
        String accountType = extras.getString(KEY_ACCOUNT_TYPE);
        Account[] accounts;
        if (accountFeatures != null) {
            try {
                accounts = AccountManager.get(context).getAccountsByTypeAndFeatures(accountType, accountFeatures, null, null).getResult(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.w(TAG, e);
                return null;
            }
        } else {
            accounts = AccountManager.get(context).getAccountsByType(accountType);
        }
        Bundle res = new Bundle();
        res.putParcelableArray(KEY_ACCOUNTS, accounts);
        return res;
    }

    @Override
    public Bundle removeAccount(Account account) {
        Log.w(TAG, "Not implemented: removeAccount(" + account + ")");
        return null;
    }

    @Override
    public Bundle requestGoogleAccountsAccess(String packageName) throws RemoteException {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.ACCOUNT);
        if (SDK_INT >= 26) {
            for (Account account : get(context).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)) {
                AccountManager.get(context).setAccountVisibility(account, packageName, VISIBILITY_VISIBLE);
            }
            Bundle res = new Bundle();
            res.putString("Error", "Ok");
            return res;
        } else {
            Log.w(TAG, "Not implemented: requestGoogleAccountsAccess(" + packageName + ")");
        }
        return null;
    }

    @Override
    public int hasCapabilities(HasCapabilitiesRequest request) throws RemoteException {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.ACCOUNT);
        List<String> services = Arrays.asList(AccountManager.get(context).getUserData(request.account, "services").split(","));
        for (String capability : request.capabilities) {
            if (capability.startsWith("service_") && !services.contains(capability.substring(8)) || !services.contains(capability)) {
                return 6;
            }
        }
        Log.w(TAG, "Not fully implemented: hasCapabilities(" + request.account + ", " + Arrays.toString(request.capabilities) + ")");
        return 1;
    }

    @Override
    public GetHubTokenInternalResponse getHubToken(GetHubTokenRequest request, Bundle extras) throws RemoteException {
        Log.w(TAG, "Not implemented: getHubToken()");
        return null;
    }

    @Override
    @SuppressLint("MissingPermission") // Workaround bug in Android Linter
    public Bundle clearToken(String token, Bundle extras) {
        String packageName = extras.getString(KEY_ANDROID_PACKAGE_NAME);
        if (packageName == null) packageName = extras.getString(KEY_CLIENT_PACKAGE_NAME);
        packageName = PackageUtils.getAndCheckCallingPackage(context, packageName, extras.getInt(KEY_CALLER_UID, 0), extras.getInt(KEY_CALLER_PID, 0));

        Log.d(TAG, "clearToken: token:" + token + " extras:" + extras);
        AccountManager.get(context).invalidateAuthToken(AuthConstants.DEFAULT_ACCOUNT_TYPE, token);

        Bundle res = new Bundle();
        res.putString("Error", "Ok");
        res.putBoolean("booleanResult", true);
        return res;
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
