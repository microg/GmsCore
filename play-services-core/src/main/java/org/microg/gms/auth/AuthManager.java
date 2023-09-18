/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.settings.SettingsContract;

import java.io.IOException;

import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;
import static android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
import static android.os.Build.VERSION.SDK_INT;
import static org.microg.gms.auth.AuthPrefs.isTrustGooglePermitted;

public class AuthManager {

    private static final String TAG = "GmsAuthManager";
    public static final String PERMISSION_TREE_BASE = "com.google.android.googleapps.permission.GOOGLE_AUTH.";
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


    private int delegationType;
    private String delegateeUserId;
    private String oauth2Foreground;
    private String oauth2Prompt;
    private String itCaveatTypes;
    private String tokenRequestOptions;
    public String includeEmail;
    public String includeProfile;

    public AuthManager(Context context, String accountName, String packageName, String service) {
        this.context = context;
        this.accountName = accountName;
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
        Uri.Builder builder = Uri.EMPTY.buildUpon();
        if (delegationType != 0 && delegateeUserId != null)
            builder.appendQueryParameter("delegation_type", Integer.toString(delegationType))
                    .appendQueryParameter("delegatee_user_id", delegateeUserId);
        if (tokenRequestOptions != null) builder.appendQueryParameter("token_request_options", tokenRequestOptions);
        if (includeEmail != null) builder.appendQueryParameter("include_email", includeEmail);
        if (includeProfile != null) builder.appendQueryParameter("include_profile", includeEmail);
        String query = builder.build().getEncodedQuery();
        return packageName + ":" + getPackageSignature() + ":" + service + (query != null ? ("?" + query) : "");
    }

    public String buildTokenKey() {
        return buildTokenKey(service);
    }

    public String buildPermKey() {
        return "perm." + buildTokenKey();
    }

    public void setPermitted(boolean value) {
        setUserData(buildPermKey(), value ? "1" : "0");
        if (SDK_INT >= 26 && value && packageName != null) {
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
        if (!"1".equals(perm)) {
            return false;
        }
        return true;
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

    public void setDelegation(int delegationType, String delegateeUserId) {
        if (delegationType != 0 && delegateeUserId != null) {
            this.delegationType = delegationType;
            this.delegateeUserId = delegateeUserId;
        } else {
            this.delegationType = 0;
            this.delegateeUserId = null;
        }
    }

    public void setOauth2Foreground(String oauth2Foreground) {
        this.oauth2Foreground = oauth2Foreground;
    }

    public void setOauth2Prompt(String oauth2Prompt) {
        this.oauth2Prompt = oauth2Prompt;
    }

    public void setItCaveatTypes(String itCaveatTypes) {
        this.itCaveatTypes = itCaveatTypes;
    }

    public void setTokenRequestOptions(String tokenRequestOptions) {
        this.tokenRequestOptions = tokenRequestOptions;
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
        if (System.currentTimeMillis() / 1000L >= getExpiry() - 300L) {
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
        if (SDK_INT >= 26 && packageName != null && auth != null) {
            // Make account persistently visible as we already granted access
            accountManager.setAccountVisibility(getAccount(), packageName, AccountManager.VISIBILITY_VISIBLE);
        }
    }

    public void invalidateAuthToken() {
        String authToken = peekAuthToken();
        invalidateAuthToken(authToken);
    }

    @SuppressLint("MissingPermission")
    public void invalidateAuthToken(String auth) {
        getAccountManager().invalidateAuthToken(accountType, auth);
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
                if (service.startsWith("oauth2:")) {
                    response.grantedScopes = service.substring(7);
                }
                response.expiry = getExpiry();
                return response;
            }
        }
        AuthRequest request = new AuthRequest().fromContext(context)
                .source("android")
                .app(packageName, getPackageSignature())
                .email(accountName)
                .token(getAccountManager().getPassword(account))
                .service(service)
                .delegation(delegationType, delegateeUserId)
                .oauth2Foreground(oauth2Foreground)
                .oauth2Prompt(oauth2Prompt)
                .oauth2IncludeProfile(includeProfile)
                .oauth2IncludeEmail(includeEmail)
                .itCaveatTypes(itCaveatTypes)
                .tokenRequestOptions(tokenRequestOptions)
                .systemPartition(isSystemApp())
                .hasPermission(isPermitted());
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
