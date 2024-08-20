/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.common.BlockingServiceConnection;
import com.google.android.gms.common.internal.GmsClientSupervisor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class GoogleAuthUtil {
    private static final String TAG = "GoogleAuthUtil";
    public static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    private static final String[] ACCEPTABLE_ACCOUNT_TYPES = new String[]{"com.google", "com.google.work", "cn.google"};
    @SuppressLint({"InlinedApi"})
    private static final String KEY_ANDROID_PACKAGE_NAME = "androidPackageName";
    private static final String KEY_CLIENT_PACKAGE_NAME = "clientPackageName";
    private static final String KEY_SCOPE_PERMISSION = "scope_permission";
    private static final ComponentName GET_TOKEN_COMPONENT = new ComponentName("com.google.android.gms", "com.google.android.gms.auth.GetToken");

    /** @deprecated */
    @Deprecated
    public static String getTokenWithNotification(Context context, String accountName, String scope, Bundle extras) throws Exception {
        Account account = new Account(accountName, GOOGLE_ACCOUNT_TYPE);
        return getTokenWithNotification(context, account, scope, extras);
    }

    /** @deprecated */
    @Deprecated
    public static String getTokenWithNotification(Context context, String accountName, String scope, Bundle extras, Intent callbackIntent) throws Exception {
        Account account = new Account(accountName, GOOGLE_ACCOUNT_TYPE);
        return getTokenWithNotification(context, account, scope, extras, callbackIntent);
    }

    /** @deprecated */
    @Deprecated
    public static String getTokenWithNotification(Context context, String accountName, String scope, Bundle extras, String authority, Bundle syncExtras) throws Exception {
        Account account = new Account(accountName, GOOGLE_ACCOUNT_TYPE);
        return getTokenWithNotification(context, account, scope, extras, authority, syncExtras);
    }

    public static String getTokenWithNotification(Context context, Account account, String scope, Bundle extras) throws Exception {
        Bundle result = extras;
        if (extras == null) {
            result = new Bundle();
        }

        result.putBoolean("handle_notification", true);
        return getAuthTokenData(context, account, scope, result).getToken();
    }

    public static String getTokenWithNotification(Context context, Account account, String scope, Bundle extras, Intent callbackIntent) throws Exception {
        if (callbackIntent == null) {
            throw new IllegalArgumentException("Callback cannot be null.");
        } else {
            String intentUri = callbackIntent.toUri(Intent.URI_INTENT_SCHEME);

            try {
                Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException var7) {
                throw new IllegalArgumentException("Parameter callback contains invalid data. It must be serializable using toUri() and parseUri().");
            }

            Bundle result = extras == null ? new Bundle() : extras;
            extras = result;
            result.putParcelable("callback_intent", callbackIntent);
            extras.putBoolean("handle_notification", true);
            return getAuthTokenData(context, account, scope, extras).getToken();
        }
    }

    public static String getTokenWithNotification(Context context, Account account, String scope, Bundle extras, String authority, Bundle syncExtras) throws Exception {
        extras = extras == null ? new Bundle() : extras;
        Bundle result = syncExtras == null ? new Bundle() : syncExtras;
        syncExtras = result;
        ContentResolver.validateSyncExtrasBundle(result);
        extras.putString("authority", authority);
        extras.putBundle("sync_extras", syncExtras);
        extras.putBoolean("handle_notification", true);
        return getAuthTokenData(context, account, scope, extras).getToken();
    }

    private static TokenData getAuthTokenData(Context context, Account account, String scope, Bundle extras) throws Exception {
        if (extras == null) {
            extras = new Bundle();
        }
        return getTokenDataFromService(context, account, scope, extras);
    }

    /** @deprecated */
    @Deprecated
    public static String getToken(Context context, String accountName, String scope) throws Exception {
        Account account = new Account(accountName, "com.google");
        return getToken(context, account, scope);
    }

    /** @deprecated */
    @Deprecated
    public static String getToken(Context context, String accountName, String scope, Bundle extras) throws Exception {
        Account account = new Account(accountName, "com.google");
        return getToken(context, account, scope, extras);
    }

    public static String getToken(Context context, Account account, String scope) throws Exception {
        return getToken(context, account, scope, new Bundle());
    }

    public static String getToken(Context context, Account account, String scope, String clientPackageName) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_CLIENT_PACKAGE_NAME, clientPackageName);
        bundle.putString(KEY_ANDROID_PACKAGE_NAME, clientPackageName);
        return getToken(context, account, scope, bundle);
    }

    public static String getToken(Context context, Account account, String scope, Bundle extra) throws Exception {
        checkAccountsAvailable(account);
        TokenData tokenDataFromService = getTokenDataFromService(context, account, scope, extra);
        if (tokenDataFromService != null) {
            return tokenDataFromService.getToken();
        }
        return null;
    }

    public static TokenData getTokenDataFromService(Context context, Account account, String scope, Bundle extra) throws Exception {
        checkAccountsAvailable(account);
        Bundle result = extra == null ? new Bundle() : new Bundle(extra);
        String clientPackageName;
        if (TextUtils.isEmpty(result.getString(KEY_CLIENT_PACKAGE_NAME))) {
            clientPackageName = context.getApplicationInfo().packageName;
            result.putString(KEY_CLIENT_PACKAGE_NAME, clientPackageName);
        }else{
            clientPackageName = result.getString(KEY_CLIENT_PACKAGE_NAME);
        }
        if (TextUtils.isEmpty(result.getString(KEY_ANDROID_PACKAGE_NAME))) {
            result.putString(KEY_ANDROID_PACKAGE_NAME, clientPackageName);
        }
        result.putBoolean(KEY_SCOPE_PERMISSION, true);
        result.putLong("service_connection_start_time_millis", SystemClock.elapsedRealtime());
        TokenDataBinder dataBinder = new TokenDataBinder(account, scope, result);
        Log.d(TAG, "getTokenDataFromService: clientPackageName: " + clientPackageName);
        return getTokenService(context, GET_TOKEN_COMPONENT, dataBinder);
    }

    /** @deprecated */
    @Deprecated
    @RequiresPermission("android.permission.MANAGE_ACCOUNTS")
    public static void invalidateToken(Context context, String authKey) {
        AccountManager.get(context).invalidateAuthToken(GOOGLE_ACCOUNT_TYPE, authKey);
    }

    public static void clearToken(Context context, String token) throws Exception {
        Bundle result = new Bundle();
        String clientPackageName;
        if (TextUtils.isEmpty(result.getString(KEY_CLIENT_PACKAGE_NAME))) {
            clientPackageName = context.getApplicationInfo().packageName;
            result.putString(KEY_CLIENT_PACKAGE_NAME, clientPackageName);
        }else{
            clientPackageName = result.getString(KEY_CLIENT_PACKAGE_NAME);
        }
        if (TextUtils.isEmpty(result.getString(KEY_ANDROID_PACKAGE_NAME))) {
            result.putString(KEY_ANDROID_PACKAGE_NAME, clientPackageName);
        }
        ClearTokenDataBinder dataBinder = new ClearTokenDataBinder(token, result);
        getTokenService(context, GET_TOKEN_COMPONENT, dataBinder);
    }

    public static List<AccountChangeEvent> getAccountChangeEvents(Context context, int eventId, String accountName) throws Exception {
        ChangeEventDataBinder dataBinder = new ChangeEventDataBinder(accountName, eventId);
        return getTokenService(context, GET_TOKEN_COMPONENT, dataBinder);
    }

    public static String getAccountId(Context context, String accountName) throws Exception {
        return getToken(context, accountName, "^^_account_id_^^", new Bundle());
    }

    @TargetApi(23)
    public static Bundle removeAccount(Context context, Account account) throws Exception {
        checkAccountsAvailable(account);
        RemoveAccountDataBinder dataBinder = new RemoveAccountDataBinder(account);
        return getTokenService(context, GET_TOKEN_COMPONENT, dataBinder);
    }

    @TargetApi(26)
    public static Boolean requestGoogleAccountsAccess(Context context) throws Exception {
        String clientPackageName = context.getApplicationInfo().packageName;
        AccessAccountDataBinder dataBinder = new AccessAccountDataBinder(clientPackageName);
        return getTokenService(context, GET_TOKEN_COMPONENT, dataBinder);
    }

    private static void checkAccountsAvailable(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        } else if (TextUtils.isEmpty(account.name)) {
            throw new IllegalArgumentException("Account name cannot be empty!");
        } else {
            String[] accountTypes;
            int length = (accountTypes = ACCEPTABLE_ACCOUNT_TYPES).length;
            for(int i = 0; i < length; ++i) {
                if (accountTypes[i].equals(account.type)) {
                    return;
                }
            }
            throw new IllegalArgumentException("Account type not supported");
        }
    }

    private static <T> T getTokenService(Context context, ComponentName componentName, DataBinder<T> binder) throws Exception {
        BlockingServiceConnection blockingServiceConnection = new BlockingServiceConnection();
        GmsClientSupervisor gmsClientSupervisor = GmsClientSupervisor.getInstance(context);
        boolean bindServiceStatus = gmsClientSupervisor.bindService(componentName, blockingServiceConnection, TAG);
        if (bindServiceStatus) {
            T data = null;
            try {
                IBinder service = blockingServiceConnection.getService();
                data = binder.getBinderData(service);
            } catch (InterruptedException | RemoteException exception) {
                Log.d(TAG, "getTokenService: Error on service connection.", exception);
            } finally {
                gmsClientSupervisor.unbindService(componentName, blockingServiceConnection, TAG);
            }
            return data;
        } else {
            throw new IOException("Could not bind to service.");
        }
    }
}
