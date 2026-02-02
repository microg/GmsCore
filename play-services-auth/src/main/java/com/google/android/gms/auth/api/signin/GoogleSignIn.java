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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import org.microg.gms.auth.api.signin.GoogleSignInCommon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Entry point for the Google Sign In API. See GoogleSignInClient.
 *
 * @deprecated Use Credential Manager for authentication or Google Identity Services for authorization.
 */
@Deprecated
public class GoogleSignIn {
    private GoogleSignIn() {
        // Disallow instantiation
    }

    /**
     * Gets a {@link GoogleSignInAccount} object to use with other authenticated APIs. Please specify the additional configurations required by the
     * authenticated API, e.g. {@link com.google.android.gms.fitness.FitnessOptions} indicating what data types you'd like to access.
     */
    @NonNull
    public static GoogleSignInAccount getAccountForExtension(@NonNull Context context, @NonNull GoogleSignInOptionsExtension extension) {
        GoogleSignInAccount lastSignedInAccount = getLastSignedInAccount(context);
        if (lastSignedInAccount == null) lastSignedInAccount = GoogleSignInAccount.createDefault();
        return lastSignedInAccount.requestExtraScopes(extension.getImpliedScopes().toArray(new Scope[0]));
    }

    /**
     * Gets a {@link GoogleSignInAccount} object to use with other authenticated APIs. Please specify the scope(s) required by the authenticated API.
     */
    @NonNull
    public static GoogleSignInAccount getAccountForScopes(@NonNull Context context, @NonNull Scope scope, @NonNull Scope... scopes) {
        GoogleSignInAccount lastSignedInAccount = getLastSignedInAccount(context);
        if (lastSignedInAccount == null) lastSignedInAccount = GoogleSignInAccount.createDefault();
        lastSignedInAccount.requestExtraScopes(scope);
        lastSignedInAccount.requestExtraScopes(scopes);
        return lastSignedInAccount;
    }

    /**
     * Create a new instance of {@link GoogleSignInClient}
     * <p>
     * See also {@link #getClient(Activity, GoogleSignInOptions)} for GoogleSignInOptions configuration.
     *
     * @param context A Context used to provide information about the application's environment.
     */
    @NonNull
    public static GoogleSignInClient getClient(@NonNull Context context, @NonNull GoogleSignInOptions options) {
        return new GoogleSignInClient(context, options);
    }

    /**
     * Create a new instance of {@link GoogleSignInClient}
     *
     * @param activity An {@link Activity} that will be used to manage the lifecycle of the GoogleSignInClient.
     * @param options  A {@link GoogleSignInOptions} used to configure the GoogleSignInClient. It is recommended to build out a GoogleSignInOptions starting
     *                 with {@code new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)}, configuring either ID token or Server Auth Code
     *                 options if you have a server. Later, in-context incrementally auth to additional scopes for other Google services access.
     * @return A instance of {@link GoogleSignInClient}
     */
    @NonNull
    public static GoogleSignInClient getClient(@NonNull Activity activity, @NonNull GoogleSignInOptions options) {
        return new GoogleSignInClient(activity, options);
    }

    /**
     * Gets the last account that the user signed in with.
     *
     * @return {@link GoogleSignInAccount} from last known successful sign-in. If user has never signed in before or has signed out / revoked
     * access, {@code null} is returned.
     */
    @Nullable
    public static GoogleSignInAccount getLastSignedInAccount(@NonNull Context context) {
        return Storage.getInstance(context).getSavedDefaultGoogleSignInAccount();
    }

    /**
     * Returns a {@link GoogleSignInAccount} present in the result data for the associated Activity started via
     * {@link GoogleSignInClient#getSignInIntent()}.
     *
     * @param data the {@link Intent} returned via {@link Activity#onActivityResult(int, int, Intent)} when sign in completed.
     * @return A completed {@link Task} containing a {@link GoogleSignInAccount} object.
     */
    @NonNull
    public static Task<GoogleSignInAccount> getSignedInAccountFromIntent(@Nullable Intent data) {
        GoogleSignInResult signInResultFromIntent = GoogleSignInCommon.getSignInResultFromIntent(data);
        GoogleSignInAccount signInAccount = signInResultFromIntent.getSignInAccount();
        Status status = signInResultFromIntent.getStatus();
        if (!signInResultFromIntent.isSuccess() || signInAccount == null) {
            if (status == null) {
                return Tasks.forException(new ApiException(Status.INTERNAL_ERROR));
            } else {
                return Tasks.forException(new ApiException(status));
            }
        }
        return Tasks.forResult(signInAccount);
    }

    /**
     * Determines if the given account has been granted permission to all scopes associated with the given extension.
     *
     * @param account   the account to be checked.
     * @param extension the extension to be checked.
     * @return {@code true} if the given account has been granted permission to all scopes associated with the given extension.
     */
    public static boolean hasPermissions(@Nullable GoogleSignInAccount account, @NonNull GoogleSignInOptionsExtension extension) {
        return hasPermissions(account, extension.getImpliedScopes().toArray(new Scope[0]));
    }

    /**
     * Determines if the given account has been granted permission to all given scopes.
     *
     * @param account the account to be checked.
     * @param scopes  the collection of scopes to be checked.
     * @return {@code true} if the given account has been granted permission to all given scopes.
     */
    public static boolean hasPermissions(@Nullable GoogleSignInAccount account, @NonNull Scope... scopes) {
        if (account == null) return false;
        Set<Scope> scopeSet = new HashSet<>();
        Collections.addAll(scopeSet, scopes);
        return account.getGrantedScopes().containsAll(scopeSet);
    }

    private static Intent createRequestPermissionsIntent(@NonNull Activity activity, @Nullable GoogleSignInAccount account, @NonNull Scope... scopes) {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder();
        if (scopes.length > 0) builder.requestScopes(scopes[0], scopes);
        if (account != null && account.getEmail() != null && !account.getEmail().isEmpty())
            builder.setAccountName(account.getEmail());
        return getClient(activity, builder.build()).getSignInIntent();
    }

    /**
     * Requests a collection of permissions to be granted to the given account. If the account does not have the requested permissions the user
     * will be presented with a UI for accepting them. Once the user has accepted or rejected a response will returned via
     * {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param activity    the target activity that will receive the response.
     * @param requestCode code associated with the request. It will match the {@code requestCode} associated with the response returned via {@link Activity#onActivityResult(int, int, Intent)}.
     * @param account     the account for which the permissions will be requested. If {@code null} the user may have the option to choose.
     * @param scopes      the extra collection of scopes to be requested.
     */
    public static void requestPermissions(@NonNull Activity activity, int requestCode, @Nullable GoogleSignInAccount account, @NonNull Scope... scopes) {
        activity.startActivityForResult(createRequestPermissionsIntent(activity, account, scopes), requestCode);
    }

    /**
     * Requests a collection of permissions associated with the given extension to be granted to the given account. If the account does not have
     * the requested permissions the user will be presented with a UI for accepting them. Once the user has accepted or rejected a response will
     * returned via {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param activity    the target activity that will receive the response.
     * @param requestCode code associated with the request. It will match the {@code requestCode} associated with the response returned via {@link Activity#onActivityResult(int, int, Intent)}.
     * @param account     the account for which the permissions will be requested. If {@code null} the user may have the option to choose.
     * @param extension   the extension associated with a set of permissions to be requested.
     */
    public static void requestPermissions(@NonNull Activity activity, int requestCode, @Nullable GoogleSignInAccount account, @NonNull GoogleSignInOptionsExtension extension) {
        requestPermissions(activity, requestCode, account, extension.getImpliedScopes().toArray(new Scope[0]));
    }

    /**
     * @param fragment the fragment to launch permission resolution Intent from.
     */
    public static void requestPermissions(@NonNull Fragment fragment, int requestCode, @Nullable GoogleSignInAccount account, @NonNull GoogleSignInOptionsExtension extension) {
        requestPermissions(fragment, requestCode, account, extension.getImpliedScopes().toArray(new Scope[0]));
    }

    /**
     * @param fragment the fragment to launch permission resolution Intent from.
     */
    public static void requestPermissions(@NonNull Fragment fragment, int requestCode, @Nullable GoogleSignInAccount account, @NonNull Scope... scopes) {
        fragment.startActivityForResult(createRequestPermissionsIntent(fragment.getActivity(), account, scopes), requestCode);
    }
}
