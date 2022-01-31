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

import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;
import static android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
import static org.microg.gms.auth.AuthPrefs.isTrustGooglePermitted;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.microg.gms.common.PackageUtils;
import org.microg.mgms.settings.SettingsContract;

import java.io.IOException;

public class AuthManager {

    private static final String TAG = "GmsAuthManager";
    public static final String PERMISSION_TREE_BASE = "com.mgoogle.android.googleapps.permission.GOOGLE_AUTH.";
    public static final String PREF_AUTH_VISIBLE = SettingsContract.Auth.VISIBLE;
    public static final int ONE_HOUR_IN_SECONDS = 60 * 60;

    private final Context context;
    private final String accountName;
    private final String packageName;
    private final String service;
    private AccountManager accountManager;
    private Account account;
    private String packageSignature;
    private String accountType;

    public AuthManager(Context context, String accountName, String packageName, String service) {
        this.context = context;
        this.accountName = accountName;
        if (packageName.contains("youtube.music")) {
            packageName = "com.google.android.apps.youtube.music";
        } else if (packageName.contains("youtube.unplugged")) {
            packageName = "com.google.android.apps.youtube.unplugged";
        } else if (packageName.contains("youtube.tv")) {
            packageName = "com.google.android.youtube.tv";
        } else if (packageName.contains("youtube")) {
            packageName = "com.google.android.youtube";
        } else if (packageName.contains("apps.photos")) {
            packageName = "com.google.android.apps.photos";
        }
        this.packageName = packageName;
        this.service = service;
    }

    public String getAccountType() {
        if (accountType == null)
            accountType = AuthConstants.DEFAULT_ACCOUNT_TYPE;
        return accountType;
    }

    public AccountManager getAccountManager() {
        if (accountManager == null)
            accountManager = AccountManager.get(context);
        return accountManager;
    }

    public Account getAccount() {
        if (account == null)
            account = new Account(accountName, getAccountType());
        return account;
    }

    public String getPackageSignature() {
        if (packageSignature == null)
            packageSignature = PackageUtils.firstSignatureDigest(context, packageName);
        return packageSignature;
    }

    public String buildTokenKey(String service) {
        return packageName + ":" + getPackageSignature() + ":" + service;
    }

    public String buildTokenKey() {
        return buildTokenKey(service);
    }

    public String buildPermKey() {
        return "perm." + buildTokenKey();
    }

    public void setPermitted(boolean value) {
        setUserData(buildPermKey(), value ? "1" : "0");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && value && packageName != null) {
            // Make account persistently visible as we already granted access
            accountManager.setAccountVisibility(getAccount(), packageName, AccountManager.VISIBILITY_VISIBLE);
        }
    }

    public boolean isPermitted() {
        if (!service.startsWith("oauth")) {
            if (context.getPackageManager().checkPermission(PERMISSION_TREE_BASE + service, packageName) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        String perm = getUserData(buildPermKey());
        return "1".equals(perm);
    }

    public void setExpiry(long expiry) {
        setUserData(buildExpireKey(), Long.toString(expiry));
    }

    public String getUserData(String key) {
        return getAccountManager().getUserData(getAccount(), key);
    }

    public void setUserData(String key, String value) {
        getAccountManager().setUserData(getAccount(), key, value);
    }

    public boolean accountExists() {
        for (Account refAccount : getAccountManager().getAccountsByType(accountType)) {
            if (refAccount.name.equalsIgnoreCase(accountName)) return true;
        }
        return false;
    }

    public String peekAuthToken() {
        Log.d(TAG, "peekAuthToken: " + buildTokenKey());
        return getAccountManager().peekAuthToken(getAccount(), buildTokenKey());
    }

    public String getAuthToken() {
        if (service.startsWith("weblogin:")) return null;
        if (getExpiry() < System.currentTimeMillis() / 1000L) {
            Log.d(TAG, "token present, but expired");
            return null;
        }
        return peekAuthToken();
    }

    public String buildExpireKey() {
        return "EXP." + buildTokenKey();
    }

    public long getExpiry() {
        String exp = getUserData(buildExpireKey());
        if (exp == null) return -1;
        return Long.parseLong(exp);
    }

    public void setAuthToken(String auth) {
        setAuthToken(service, auth);
    }

    public void setAuthToken(String service, String auth) {
        getAccountManager().setAuthToken(getAccount(), buildTokenKey(service), auth);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageName != null && auth != null) {
            // Make account persistently visible as we already granted access
            accountManager.setAccountVisibility(getAccount(), packageName, AccountManager.VISIBILITY_VISIBLE);
        }
    }

    public void storeResponse(AuthResponse response) {
        if (service.startsWith("weblogin:")) return;
        if (response.accountId != null)
            setUserData("GoogleUserId", response.accountId);
        if (response.Sid != null)
            setAuthToken("SID", response.Sid);
        if (response.LSid != null)
            setAuthToken("LSID", response.LSid);
        if (response.auth != null && (response.expiry != 0 || response.storeConsentRemotely)) {
            setAuthToken(response.auth);
            if (response.expiry > 0) {
                setExpiry(response.expiry);
            } else {
                setExpiry(System.currentTimeMillis() / 1000 + ONE_HOUR_IN_SECONDS); // make valid for one hour by default
            }
        }
    }

    private boolean isSystemApp() {
        try {
            int flags = context.getPackageManager().getApplicationInfo(packageName, 0).flags;
            return (flags & FLAG_SYSTEM) > 0 || (flags & FLAG_UPDATED_SYSTEM_APP) > 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public AuthResponse requestAuth(boolean legacy) throws IOException {
        if (service.equals(AuthConstants.SCOPE_GET_ACCOUNT_ID)) {
            AuthResponse response = new AuthResponse();
            response.accountId = response.auth = getAccountManager().getUserData(getAccount(), "GoogleUserId");
            return response;
        }
        if (isPermitted() || isTrustGooglePermitted(context)) {
            String token = getAuthToken();
            if (token != null) {
                AuthResponse response = new AuthResponse();
                response.issueAdvice = "stored";
                response.auth = token;
                return response;
            }
        }
        AuthRequest request = new AuthRequest().fromContext(context)
                .source("android")
                .app(packageName, getPackageSignature())
                .email(accountName)
                .token(getAccountManager().getPassword(account))
                .service(service);
        if (isSystemApp()) request.systemPartition();
        if (isPermitted()) request.hasPermission();
        if (legacy) {
            request.callerIsGms().calledFromAccountManager();
        } else {
            request.callerIsApp();
        }
        AuthResponse response = request.getResponse();
        if (!isPermitted() && !isTrustGooglePermitted(context)) {
            response.auth = null;
        } else {
            storeResponse(response);
        }
        return response;
    }

    public String getService() {
        return service;
    }
}
