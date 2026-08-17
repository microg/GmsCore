/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.api.signin;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInApi;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

public class GoogleSignInApiImpl implements GoogleSignInApi {
    @NonNull
    @Override
    public Intent getSignInIntent(GoogleApiClient client) {
        throw new UnsupportedOperationException();
        //return GoogleSignInCommon.getSignInIntent(client.getContext(), client.getClient(Auth.GOOGLE_SIGN_IN_API_CLIENT_KEY).getOptions());
    }

    @Nullable
    @Override
    public GoogleSignInResult getSignInResultFromIntent(@NonNull Intent data) {
        return GoogleSignInCommon.getSignInResultFromIntent(data);
    }

    @NonNull
    @Override
    public PendingResult<Status> revokeAccess(@NonNull GoogleApiClient client) {
        return GoogleSignInCommon.revokeAccess(client, client.getContext(), false);
    }

    @NonNull
    @Override
    public PendingResult<Status> signOut(@NonNull GoogleApiClient client) {
        return GoogleSignInCommon.signOut(client, client.getContext(), false);
    }

    @NonNull
    @Override
    public OptionalPendingResult<GoogleSignInResult> silentSignIn(@NonNull GoogleApiClient client) {
        throw new UnsupportedOperationException();
        //return GoogleSignInCommon.silentSignIn(client, client.getContext(), client.getClient(Auth.GOOGLE_SIGN_IN_API_CLIENT_KEY).getOptions());
    }
}
