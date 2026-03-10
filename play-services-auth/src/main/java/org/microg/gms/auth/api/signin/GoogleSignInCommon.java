/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.api.signin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.internal.SignInConfiguration;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

public class GoogleSignInCommon {

    @NonNull
    public static Intent getSignInIntent(Context context, GoogleSignInOptions options) {
        SignInConfiguration configuration = new SignInConfiguration(context.getPackageName(), options);
        Bundle configurationBundle = new Bundle();
        configurationBundle.putParcelable("config", configuration);
        Intent intent = new Intent("com.google.android.gms.auth.GOOGLE_SIGN_IN");
        intent.setPackage(context.getPackageName());
        //intent.setClass(context, GoogleSignInHub.class);
        intent.putExtra("config", configurationBundle);
        return intent;
    }

    @NonNull
    public static GoogleSignInResult getSignInResultFromIntent(@Nullable Intent data) {
        if (data == null) return new GoogleSignInResult(null, Status.INTERNAL_ERROR);
        Status status = data.getParcelableExtra("googleSignInStatus");
        GoogleSignInAccount account = data.getParcelableExtra("googleSignInAccount");
        if (account != null) return new GoogleSignInResult(account, Status.SUCCESS);
        if (status == null) status = Status.INTERNAL_ERROR;
        return new GoogleSignInResult(null, status);
    }

    public static PendingResult<Status> revokeAccess(GoogleApiClient client, Context context, boolean isLocalFallback) {
        throw new UnsupportedOperationException();
    }

    public static PendingResult<Status> signOut(GoogleApiClient client, Context context, boolean isLocalFallback) {
        throw new UnsupportedOperationException();
    }

    public static OptionalPendingResult<GoogleSignInResult> silentSignIn(GoogleApiClient client, Context context, GoogleSignInOptions options, boolean isLocalFallback) {
        throw new UnsupportedOperationException();
    }
}
