/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.signin;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

/**
 * Api interface for Sign In with Google.
 *
 * @deprecated Use Credential Manager for authentication or Google Identity Services for authorization.
 */
@Deprecated
public interface GoogleSignInApi {
    /**
     * String {@link Intent} extra key for getting the SignInAccount from the {@link Intent} data returned on {@link Activity#onActivityResult(int, int, Intent)}
     * when sign-in succeeded.
     */
    @NonNull
    String EXTRA_SIGN_IN_ACCOUNT = "signInAccount";

    /**
     * Gets an {@link Intent} to start the Google Sign In flow by calling {@link Activity#startActivityForResult(Intent, int)}.
     *
     * @param client The {@link GoogleApiClient} to service the call.
     * @return the {@link Intent} used for start the sign-in flow.
     */
    @NonNull
    Intent getSignInIntent(GoogleApiClient client);

    /**
     * Helper function to extract out {@link GoogleSignInResult} from the {@link Activity#onActivityResult(int, int, Intent)} for Sign In.
     *
     * @param data the {@link Intent} returned on {@link Activity#onActivityResult(int, int, Intent)} when sign in completed.
     * @return The {@link GoogleSignInResult} object. Make sure to pass the {@link Intent} you get back from {@link Activity#onActivityResult(int, int, Intent)
     * for Sign In, otherwise result will be null.
     */
    @Nullable
    GoogleSignInResult getSignInResultFromIntent(@NonNull Intent data);

    /**
     * Revokes access given to the current application. Future sign-in attempts will require the user to re-consent to all requested scopes.
     * Applications are required to provide users that are signed in with Google the ability to disconnect their Google account from the app. If the
     * user deletes their account, you must delete the information that your app obtained from the Google APIs.
     *
     * @param client The connected {@link GoogleApiClient} to service the call.
     * @return the PendingResult for notification and access to the result when it's available.
     */
    @NonNull
    PendingResult<Status> revokeAccess(@NonNull GoogleApiClient client);

    /**
     * Signs out the current signed-in user if any. It also clears the account previously selected by the user and a future sign in attempt will require
     * the user pick an account again.
     *
     * @param client The connected {@link GoogleApiClient} to service the call.
     * @return the PendingResult for notification and access to the result when it's available.
     */
    @NonNull
    PendingResult<Status> signOut(@NonNull GoogleApiClient client);

    /**
     * Returns the {@link GoogleSignInAccount} information for the user who is signed in to this app. If no user is signed in, try to sign the
     * user in without displaying any user interface.
     * <p>
     * Client activities may call the returned {@link OptionalPendingResult#isDone()} to decide whether to show a loading indicator and set callbacks
     * to handle an asynchronous result, or directly proceed to the next step.
     * <p>
     * The GoogleSignInResult will possibly contain an ID token which may be used to authenticate and identify sessions that you establish with
     * your application servers. If you use the ID token expiry time to determine your session lifetime, you should retrieve a refreshed ID token, by
     * calling silentSignIn prior to each API call to your application server.
     * <p>
     * Calling silentSignIn can also help you detect user revocation of access to your application on other platforms and you can call
     * {@link #getSignInIntent(GoogleApiClient)} again to ask the user to re-authorize.
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
     * @param client The {@link GoogleApiClient} to service the call.
     * @return {@link OptionalPendingResult} that will yield a {@link GoogleSignInResult}. Check for an immediate result with
     * {@link OptionalPendingResult#isDone()}; or set a callback to handle asynchronous results.
     */
    @NonNull
    OptionalPendingResult<GoogleSignInResult> silentSignIn(@NonNull GoogleApiClient client);
}
