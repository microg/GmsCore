/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.signin;

import android.accounts.Account;
import android.content.Context;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.internal.IAccountAccessor;
import com.google.android.gms.common.internal.ResolveAccountRequest;
import com.google.android.gms.signin.SignInClient;
import com.google.android.gms.signin.internal.ISignInCallbacks;
import com.google.android.gms.signin.internal.ISignInService;
import com.google.android.gms.signin.internal.SignInRequest;
import com.google.android.gms.signin.internal.SignInResponse;
import org.microg.gms.auth.AuthConstants;
import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ApiClientSettings;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public class SignInClientImpl extends GmsClient<ISignInService> implements SignInClient {
    private static final String TAG = "SignInClientImpl";
    private final int sessionId;
    private final Account account;

    public SignInClientImpl(Context context, ApiClientSettings clientSettings, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.SIGN_IN.ACTION);
        serviceId = GmsService.SIGN_IN.SERVICE_ID;

        account = new Account(clientSettings.accountName != null ? clientSettings.accountName : AuthConstants.DEFAULT_ACCOUNT, AuthConstants.DEFAULT_ACCOUNT_TYPE);
        extras.putParcelable("com.google.android.gms.signin.internal.clientRequestedAccount", account);

        sessionId = clientSettings.sessionId;
        extras.putInt("com.google.android.gms.common.internal.ClientSettings.sessionId", sessionId);

        extras.putBoolean("com.google.android.gms.signin.internal.offlineAccessRequested", false);
        extras.putBoolean("com.google.android.gms.signin.internal.idTokenRequested", false);
        extras.putString("com.google.android.gms.signin.internal.serverClientId", null);
        extras.putBoolean("com.google.android.gms.signin.internal.usePromptModeForAuthCode", true);
        extras.putBoolean("com.google.android.gms.signin.internal.forceCodeForRefreshToken", false);
        extras.putString("com.google.android.gms.signin.internal.hostedDomain", null);
        extras.putString("com.google.android.gms.signin.internal.logSessionId", null);
        extras.putBoolean("com.google.android.gms.signin.internal.waitForAccessTokenRefresh", false);

        if (clientSettings.packageName != null && !context.getPackageName().equals(clientSettings.packageName)) {
            extras.putString("com.google.android.gms.signin.internal.realClientPackageName", clientSettings.packageName);
        }
    }

    @Override
    protected ISignInService interfaceFromBinder(IBinder binder) {
        return ISignInService.Stub.asInterface(binder);
    }

    @Override
    public void clearAccountFromSessionStore() {
        try {
            getServiceInterface().clearAccountFromSessionStore(sessionId);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void saveDefaultAccount(@NonNull IAccountAccessor accountAccessor, boolean crossClient) {
        try {
            getServiceInterface().saveDefaultAccount(accountAccessor, sessionId, crossClient);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void signIn(@NonNull ISignInCallbacks callbacks) {
        try {
            SignInRequest request = new SignInRequest();
            request.request = new ResolveAccountRequest();
            request.request.account = account;
            request.request.sessionId = sessionId;
            if (account.name.equals(AuthConstants.DEFAULT_ACCOUNT)) {
                request.request.signInAccountHint = Storage.getInstance(getContext()).getSavedDefaultGoogleSignInAccount();
            }
            getServiceInterface().signIn(request, callbacks);
        } catch (Exception e) {
            Log.w(TAG, e);
            try {
                SignInResponse response = new SignInResponse();
                response.connectionResult = new ConnectionResult(ConnectionResult.INTERNAL_ERROR);
                callbacks.onSignIn(response);
            } catch (Exception ignored) {
            }
        }
    }
}
