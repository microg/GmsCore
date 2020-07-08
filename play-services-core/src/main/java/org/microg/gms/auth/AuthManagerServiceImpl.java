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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.auth.IAuthManagerService;
import com.google.android.gms.R;
import com.google.android.gms.auth.AccountChangeEventsRequest;
import com.google.android.gms.auth.AccountChangeEventsResponse;
import com.google.android.gms.auth.TokenData;
import com.google.android.gms.common.api.Scope;

import org.microg.gms.common.PackageUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_CALLER_PID;
import static org.microg.gms.auth.AskPermissionActivity.EXTRA_CONSENT_DATA;

public class AuthManagerServiceImpl extends IAuthManagerService.Stub {
    private static final String TAG = "GmsAuthManagerSvc";

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

    public static final String KEY_ERROR = "Error";
    public static final String KEY_USER_RECOVERY_INTENT = "userRecoveryIntent";

    private final Context context;

    public AuthManagerServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public Bundle getToken(String accountName, String scope, Bundle extras) throws RemoteException {
        String packageName = extras.getString(KEY_ANDROID_PACKAGE_NAME);
        if (packageName == null || packageName.isEmpty())
            packageName = extras.getString(KEY_CLIENT_PACKAGE_NAME);
        packageName = PackageUtils.getAndCheckCallingPackage(context, packageName, extras.getInt(KEY_CALLER_UID, 0), extras.getInt(KEY_CALLER_PID, 0));
        boolean notify = extras.getBoolean(KEY_HANDLE_NOTIFICATION, false);

        Log.d(TAG, "getToken: account:" + accountName + " scope:" + scope + " extras:" + extras + ", notify: " + notify);

        /*
         * TODO: This scope seems to be invalid (according to https://developers.google.com/oauthplayground/),
         * but is used in some applications anyway. Removing it is unlikely a good solution, but works for now.
         */
        scope = scope.replace("https://www.googleapis.com/auth/identity.plus.page.impersonation ", "");

        AuthManager authManager = new AuthManager(context, accountName, packageName, scope);
        Bundle result = new Bundle();
        result.putString(KEY_ACCOUNT_NAME, accountName);
        result.putString(KEY_ACCOUNT_TYPE, authManager.getAccountType());
        try {
            AuthResponse res = authManager.requestAuth(false);
            if (res.auth != null) {
                Log.d(TAG, "getToken: " + res);
                result.putString(KEY_AUTHTOKEN, res.auth);
                Bundle details = new Bundle();
                details.putParcelable("TokenData", new TokenData(res.auth, res.expiry, scope.startsWith("oauth2:"), getScopes(scope)));
                result.putBundle("tokenDetails", details);
                result.putString(KEY_ERROR, "OK");
            } else {
                result.putString(KEY_ERROR, "NeedPermission");
                Intent i = new Intent(context, AskPermissionActivity.class);
                i.putExtras(extras);
                i.putExtra(KEY_ANDROID_PACKAGE_NAME, packageName);
                i.putExtra(KEY_ACCOUNT_TYPE, authManager.getAccountType());
                i.putExtra(KEY_ACCOUNT_NAME, accountName);
                i.putExtra(KEY_AUTHTOKEN, scope);
                try {
                    if (res.consentDataBase64 != null)
                        i.putExtra(EXTRA_CONSENT_DATA, Base64.decode(res.consentDataBase64, Base64.URL_SAFE));
                } catch (Exception e) {
                    Log.w(TAG, "Can't decode consent data: ", e);
                }
                if (notify) {
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(packageName.hashCode(), new NotificationCompat.Builder(context)
                            .setContentIntent(PendingIntent.getActivity(context, 0, i, 0))
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
    public Bundle getTokenWithAccount(Account account, String scope, Bundle extras) throws RemoteException {
        return getToken(account.name, scope, extras);
    }

    @Override
    public Bundle clearToken(String token, Bundle extras) throws RemoteException {
        String packageName = extras.getString(KEY_ANDROID_PACKAGE_NAME);
        if (packageName == null) packageName = extras.getString(KEY_CLIENT_PACKAGE_NAME);
        packageName = PackageUtils.getAndCheckCallingPackage(context, packageName, extras.getInt(KEY_CALLER_UID, 0), extras.getInt(KEY_CALLER_PID, 0));

        Log.d(TAG, "clearToken: token:" + token + " extras:" + extras);
        AccountManager.get(context).invalidateAuthToken(AuthConstants.DEFAULT_ACCOUNT_TYPE, token);
        return null;
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
