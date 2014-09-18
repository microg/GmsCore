/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.auth.IAuthManagerService;

import java.util.Arrays;

public class AuthManagerServiceImpl extends IAuthManagerService.Stub {
    public static final String GOOGLE_ACCOUNT_TYPE = "com.google";

    public static final String KEY_AUTHORITY = "authority";
    public static final String KEY_ANDROID_PACKAGE_NAME = "androidPackageName";
    public static final String KEY_CALLBACK_INTENT = "callback_intent";
    public static final String KEY_CALLER_UID = "callerUid";
    public static final String KEY_CLIENT_PACKAGE_NAME = "clientPackageName";
    public static final String KEY_HANDLE_NOTIFICATION = "handle_notification";
    public static final String KEY_REQUEST_ACTIONS = "request_visible_actions";
    public static final String KEY_REQUEST_VISIBLE_ACTIVITIES = "request_visible_actions";
    public static final String KEY_SUPPRESS_PROGRESS_SCREEN = "suppressProgressScreen";
    public static final String KEY_SYNC_EXTRAS = "sync_extras";

    public static final String KEY_AUTH_TOKEN = "authtoken";
    public static final String KEY_ERROR = "Error";
    public static final String KEY_USER_RECOVERY_INTENT = "userRecoveryIntent";

    private Context context;

    private class State {
        String authToken;
    }

    public AuthManagerServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public Bundle getToken(String accountName, String scope, Bundle extras) throws RemoteException {
        String packageName = extras.getString(KEY_ANDROID_PACKAGE_NAME, extras.getString(KEY_CLIENT_PACKAGE_NAME, null));
        int callerUid = extras.getInt(KEY_CALLER_UID, 0);
        checkPackage(packageName, callerUid, getCallingUid());
        boolean notify = extras.getBoolean(KEY_HANDLE_NOTIFICATION, false);

        Log.d("AuthManagerService", "getToken: account:" + accountName + " scope:" + scope + " extras:" + extras);
        AccountManagerFuture<Bundle> authToken = AccountManager.get(context).getAuthToken(new Account(accountName, GOOGLE_ACCOUNT_TYPE), scope, extras, notify, null, new Handler(Looper.getMainLooper()));
        try {
            Bundle requestResult = authToken.getResult();
            if (!requestResult.containsKey(AccountManager.KEY_AUTHTOKEN) && requestResult.containsKey(AccountManager.KEY_INTENT)) {
                Intent intent = requestResult.getParcelable(AccountManager.KEY_INTENT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
            Log.d("getToken", requestResult.toString());
            Bundle result = new Bundle();
            result.putString(KEY_AUTH_TOKEN, requestResult.getString(AccountManager.KEY_AUTHTOKEN));
            result.putString(KEY_ERROR, "Unknown");
            result.putParcelable(KEY_USER_RECOVERY_INTENT, requestResult.getParcelable(AccountManager.KEY_INTENT));
            return result;
        } catch (Exception e) {
            Log.w("AuthManagerService", e);
            throw new RemoteException(e.getMessage());
        }
    }

    private void checkPackage(String packageName, int callerUid, int callingUid) {
        if (callerUid != callingUid) {
            throw new SecurityException("callerUid [" + callerUid + "] and real calling uid [" + callingUid + "] mismatch!");
        }
        String[] packagesForUid = context.getPackageManager().getPackagesForUid(callerUid);
        if (!Arrays.asList(packagesForUid).contains(packageName)) {
            throw new SecurityException("callerUid [" + callerUid + "] is not related to packageName [" + packageName + "]");
        }
    }

    @Override
    public Bundle clearToken(String token, Bundle extras) throws RemoteException {
        return null;
    }
}
