/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.signin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.internal.PendingResultUtil;
import com.google.android.gms.tasks.Task;
import org.microg.gms.auth.api.signin.GoogleSignInCommon;

/**
 * A client for interacting with the Google Sign In API.
 *
 * @deprecated Use Credential Manager for authentication or Google Identity Services for authorization.
 */
@Deprecated
public class GoogleSignInClient extends GoogleApi<GoogleSignInOptions> {
    GoogleSignInClient(@NonNull Context context, GoogleSignInOptions options) {
        super(context, Auth.GOOGLE_SIGN_IN_API, options);
    }

    GoogleSignInClient(@NonNull Activity activity, GoogleSignInOptions options) {
        super(activity, Auth.GOOGLE_SIGN_IN_API, options);
    }

    private boolean isLocalFallback() {
        return false;
    }

    /**
     * Gets an {@link Intent} to start the Google Sign In flow by calling {@link Activity#startActivityForResult(Intent, int)}.
     *
     * @return The {@link Intent} used for start the sign-in flow.
     */
    @NonNull
    public Intent getSignInIntent() {
        throw new UnsupportedOperationException();
    }

    /**
     * Revokes access given to the current application. Future sign-in attempts will require the user to re-consent to all requested scopes.
     * Applications are required to provide users that are signed in with Google the ability to disconnect their Google account from the app. If the
     * user deletes their account, you must delete the information that your app obtained from the Google APIs.
     *
     * @return A {@link Task} that may be used to check for failure, success or completion
     */
    @NonNull
    public Task<Void> revokeAccess() {
        return PendingResultUtil.toVoidTask(GoogleSignInCommon.revokeAccess(asGoogleApiClient(), getApplicationContext(), isLocalFallback()));
    }

    /**
     * Signs out the current signed-in user if any. It also clears the account previously selected by the user and a future sign in attempt will require
     * the user pick an account again.
     *
     * @return A {@link Task} that may be used to check for failure, success or completion
     */
    @NonNull
    public Task<Void> signOut() {
        return PendingResultUtil.toVoidTask(GoogleSignInCommon.signOut(asGoogleApiClient(), getApplicationContext(), isLocalFallback()));
    }

    /**
     * Returns the {@link GoogleSignInAccount} information for the user who is signed in to this app. If no user is signed in, try to sign the
     * user in without displaying any user interface.
     * <p>
     * The GoogleSignInAccount will possibly contain an ID token which may be used to authenticate and identify sessions that you establish with
     * your application servers. If you use the ID token expiry time to determine your session lifetime, you should retrieve a refreshed ID token, by
     * calling silentSignIn prior to each API call to your application server.
     * <p>
     * Calling silentSignIn can also help you detect user revocation of access to your application on other platforms and you can call
     * {@link #getSignInIntent()} again to ask the user to re-authorize.
     * <p>
     * If your user has never previously signed in to your app on the current device, we can still try to sign them in, without displaying user
     * interface, if they have signed in on a different device.
     * <p>
     * We attempt to sign users in if:
     * <ul>
     *     <li>There is one and only one matching account on the device that has previously signed in to your application, and</li>
     *     <li>the user previously granted all of the scopes your app is requesting for this sign in.</li>
     * </ul>
     *
     * @return A {@link Task} that will yield a {@link GoogleSignInAccount}. Check for an immediate result with {@link Task#isSuccessful()}; or set a
     * callback to handle asynchronous results.
     */
    @NonNull
    public Task<GoogleSignInAccount> silentSignIn() {
        return PendingResultUtil.toTask(GoogleSignInCommon.silentSignIn(asGoogleApiClient(), getApplicationContext(), getApiOptions(), isLocalFallback()), (result) -> ((GoogleSignInResult) result).getSignInAccount());
    }
}
