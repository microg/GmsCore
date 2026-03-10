/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.accounts.Account;
import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.signin.SignInOptions;
import org.microg.gms.auth.AuthConstants;
import org.microg.gms.common.Hide;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Hide
public class ClientSettings {
    @Nullable
    private final Account account;
    @NonNull
    private final Set<Scope> requiredScopes;
    @NonNull
    private final Set<Scope> allRequestedScopes;
    @NonNull
    private final Map<Api<?>, Set<Scope>> scopesForOptionalApi;
    private final int gravityForPopups;
    @Nullable
    private final View viewForPopups;
    @NonNull
    private final String realClientPackageName;
    @Nullable
    private final String contextClassName;
    @NonNull
    private final SignInOptions signInOptions;
    private Integer sessionId;

    public ClientSettings(@Nullable Account account, @NonNull Set<Scope> requiredScopes, @NonNull Map<Api<?>, Set<Scope>> scopesForOptionalApi, int gravityForPopups, @Nullable View viewForPopups, @NonNull String realClientPackageName, @NonNull String contextClassName, @NonNull SignInOptions signInOptions) {
        this.account = account;
        this.requiredScopes = requiredScopes;
        this.scopesForOptionalApi = scopesForOptionalApi;
        this.gravityForPopups = gravityForPopups;
        this.viewForPopups = viewForPopups;
        this.realClientPackageName = realClientPackageName;
        this.contextClassName = contextClassName;
        this.signInOptions = signInOptions;

        allRequestedScopes = new HashSet<>(requiredScopes);
        for (Set<Scope> scopeSet : scopesForOptionalApi.values()) {
            allRequestedScopes.addAll(scopeSet);
        }
    }

    @NonNull
    public static ClientSettings createDefault(Context context) {
        return new GoogleApiClient.Builder(context).getClientSettings();
    }

    @Nullable
    public Account getAccount() {
        return account;
    }

    @Deprecated
    @Nullable
    public String getAccountName() {
        if (account == null) return null;
        return account.name;
    }

    @NonNull
    public Account getAccountOrDefault() {
        if (account == null) return new Account(GoogleApiClient.DEFAULT_ACCOUNT, AuthConstants.DEFAULT_ACCOUNT_TYPE);
        return account;
    }

    @NonNull
    public Set<Scope> getAllRequestedScopes() {
        return allRequestedScopes;
    }

    @NonNull
    public Set<Scope> getApplicableScopes(@NonNull Api<?> api) {
        Set<Scope> scopes = new HashSet<>(requiredScopes);
        Set<Scope> apiScopes = scopesForOptionalApi.get(api);
        if (apiScopes != null) scopes.addAll(apiScopes);
        return scopes;
    }

    public int getGravityForPopups() {
        return gravityForPopups;
    }

    @NonNull
    public String getRealClientPackageName() {
        return realClientPackageName;
    }

    @NonNull
    public Set<Scope> getRequiredScopes() {
        return requiredScopes;
    }

    @Nullable
    public View getViewForPopups() {
        return viewForPopups;
    }

    @NonNull
    public Map<Api<?>, Set<Scope>> getScopesForOptionalApi() {
        return scopesForOptionalApi;
    }

    @Nullable
    public String getContextClassName() {
        return contextClassName;
    }

    @NonNull
    public SignInOptions getSignInOptions() {
        return signInOptions;
    }

    @Nullable
    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(@NonNull Integer sessionId) {
        this.sessionId = sessionId;
    }
}
