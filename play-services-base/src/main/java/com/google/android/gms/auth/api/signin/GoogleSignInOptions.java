/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.signin;

import android.accounts.Account;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import org.microg.gms.auth.AuthConstants;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * {@code GoogleSignInOptions} contains options used to configure the {@link Auth#GOOGLE_SIGN_IN_API}.
 */
public class GoogleSignInOptions extends AutoSafeParcelable {
    /**
     * Default and recommended configuration for Games Sign In.
     * <ul>
     * <li>If your app has a server, you can build a configuration via {@code new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)} and
     * further configure {@link GoogleSignInOptions.Builder#requestServerAuthCode(String)}.</li>
     * <li>If you want to customize Games sign-in options, you can build a configuration via {@code new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)}
     * and further configure {@link Games.GamesOptions} via {@link GoogleSignInOptions.Builder#addExtension(GoogleSignInOptionsExtension)}.</li>
     * </ul>
     * To maximize chance of auto-sign-in, do NOT use {@link GoogleSignInOptions.Builder#requestScopes(Scope, Scope...)} to request additional scopes and do
     * NOT use {@link GoogleSignInOptions.Builder#requestIdToken(String)} to request user's real Google identity assertion.
     */
    @NonNull
    public static final GoogleSignInOptions DEFAULT_GAMES_SIGN_IN = null;

    /**
     * Default configuration for Google Sign In. You can get a stable user ID and basic profile info back via {@link GoogleSignInAccount#getId()} after you
     * trigger sign in from either {@link GoogleSignInApi#silentSignIn} or {@link GoogleSignInApi#getSignInIntent}. If you require more information for the
     * sign in result, please build a configuration via {@code new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)}.
     */
    @NonNull
    public static final GoogleSignInOptions DEFAULT_SIGN_IN = null;

    @Field(1)
    private int versionCode = 3;
    @Field(2)
    private ArrayList<Scope> scopes;
    @Field(3)
    private Account account;
    @Field(4)
    private boolean idTokenRequested;
    @Field(5)
    private boolean serverAuthCodeRequested;
    @Field(6)
    private boolean forceCodeForRefreshToken;
    @Field(7)
    private String serverClientId;
    @Field(8)
    private String hostedDomain;
    //    @Field(9)
//    private ArrayList<GoogleSignInOptionsExtension> extensions;
    @Field(10)
    private String logSessionId;

    private GoogleSignInOptions() {
    }

    /**
     * Gets an array of all the requested scopes. If you use DEFAULT_SIGN_IN, this array will also include those scopes set by default in DEFAULT_SIGN_IN.
     * <p>
     * A usage of this method could be set the scopes for the contextual SignInButton. E.g., {@code signInButton.setScopes(googleSignInOptions.getScopeArray())}
     */
    @NonNull
    public Scope[] getScopeArray() {
        return null;
    }

    /**
     * Builder for {@link GoogleSignInOptions}.
     */
    public static final class Builder {
        private final Set<Scope> scopes;
        private boolean requestIdToken;
        private boolean requestServerAuthCode;
        private boolean forceCodeForRefreshToken;
        @Nullable
        private String serverClientId;
        @Nullable
        private Account account;
        @Nullable
        private String hostedDomain;

        public Builder() {
            this.scopes = new HashSet<>();
        }

        public Builder(GoogleSignInOptions options) {
            this.scopes = new HashSet<>(options.scopes);
            this.requestIdToken = options.idTokenRequested;
            this.requestServerAuthCode = options.serverAuthCodeRequested;
            this.forceCodeForRefreshToken = options.forceCodeForRefreshToken;
            this.serverClientId = options.serverClientId;
            this.account = options.account;
            this.hostedDomain = options.hostedDomain;
        }

        /**
         * Specifies additional sign-in options via the given extension.
         *
         * @param extension A sign-in extension used to further configure API specific sign-in options. Supported values include: {@link Games.GamesOptions}.
         */
        @NonNull
        public Builder addExtension(GoogleSignInOptionsExtension extension) {
            return this;
        }

        /**
         * Specifies that email info is requested by your application. Note that we don't recommend keying user by email address since email address might
         * change. Keying user by ID is the preferable approach.
         */
        @NonNull
        public Builder requestEmail() {
            this.scopes.add(new Scope(Scopes.EMAIL));
            return this;
        }

        /**
         * Specifies that user ID is requested by your application.
         */
        @NonNull
        public Builder requestId() {
            this.scopes.add(new Scope(Scopes.OPENID));
            return this;
        }

        /**
         * Specifies that an ID token for authenticated users is requested. Requesting an ID token requires that the server client ID be specified.
         *
         * @param serverClientId The client ID of the server that will verify the integrity of the token.
         */
        @NonNull
        public Builder requestIdToken(@NonNull String serverClientId) {
            this.requestIdToken = true;
            this.serverClientId = serverClientId;
            return this;
        }

        /**
         * Specifies that user's profile info is requested by your application.
         */
        @NonNull
        public Builder requestProfile() {
            this.scopes.add(new Scope(Scopes.PROFILE));
            return this;
        }

        /**
         * Specifies OAuth 2.0 scopes your application requests. See {@link Scopes} for more information.
         *
         * @param scope  An OAuth 2.0 scope requested by your app.
         * @param scopes More OAuth 2.0 scopes requested by your app.
         */
        @NonNull
        public Builder requestScopes(@NonNull Scope scope, @NonNull Scope... scopes) {
            this.scopes.add(scope);
            this.scopes.addAll(Arrays.asList(scopes));
            return this;
        }

        /**
         * Specifies that offline access is requested. Requesting offline access requires that the server client ID be specified.
         * <p>
         * You don't need to use {@link #requestIdToken(String)} when you use this option. When your server exchanges the code for tokens, an ID token will be
         * returned together (as long as you either use {@link #requestEmail()} or {@link #requestProfile()} along with your configuration).
         * <p>
         * The first time you retrieve a code, a refresh_token will be granted automatically. Subsequent requests will only return codes that can be exchanged for access token.
         *
         * @param serverClientId The client ID of the server that will need the auth code.
         */
        public Builder requestServerAuthCode(String serverClientId) {
            return requestServerAuthCode(serverClientId, false);
        }

        /**
         * Specifies that offline access is requested. Requesting offline access requires that the server client ID be specified.
         * <p>
         * You don't need to use {@link #requestIdToken(String)} when you use this option. When your server exchanges the code for tokens, an ID token will be
         * returned together (as long as you either use {@link #requestEmail()} or {@link #requestProfile()} along with this configuration).
         *
         * @param serverClientId           The client ID of the server that will need the auth code.
         * @param forceCodeForRefreshToken If true, the granted code can be exchanged for an access token and a refresh token. The first time you retrieve a
         *                                 code, a refresh_token will be granted automatically. Subsequent requests will require additional user consent. Use
         *                                 false by default; only use true if your server has suffered some failure and lost the user's refresh token.
         */
        public Builder requestServerAuthCode(String serverClientId, boolean forceCodeForRefreshToken) {
            this.requestServerAuthCode = true;
            this.forceCodeForRefreshToken = true;
            this.serverClientId = serverClientId;
            return this;

        }

        /**
         * Specifies an account name on the device that should be used. If this is never called, the client will use the current default account for this application.
         *
         * @param accountName The account name on the device that should be used to sign in.
         */
        public GoogleSignInOptions.Builder setAccountName(String accountName) {
            this.account = new Account(accountName, AuthConstants.DEFAULT_ACCOUNT_TYPE);
            return this;
        }

        /**
         * Specifies a hosted domain restriction. By setting this, sign in will be restricted to accounts of the user in the specified domain.
         *
         * @param hostedDomain domain of the user to restrict (for example, "mycollege.edu")
         */
        public GoogleSignInOptions.Builder setHostedDomain(String hostedDomain) {
            this.hostedDomain = hostedDomain;
            return this;
        }

        /**
         * Builds the {@link GoogleSignInOptions} object.
         *
         * @return a {@link GoogleSignInOptions} instance.
         */
        @NonNull
        public GoogleSignInOptions build() {
            GoogleSignInOptions options = new GoogleSignInOptions();
            if (scopes.contains(new Scope(Scopes.GAMES))) {
                scopes.remove(new Scope(Scopes.GAMES_LITE));
            }
            if (requestIdToken && (account == null || !scopes.isEmpty())) {
                scopes.add(new Scope(Scopes.OPENID));
            }
            options.scopes = new ArrayList<>(scopes);
            options.idTokenRequested = requestIdToken;
            options.serverAuthCodeRequested = requestServerAuthCode;
            options.forceCodeForRefreshToken = forceCodeForRefreshToken;
            options.serverClientId = serverClientId;
            options.account = account;
            options.hostedDomain = hostedDomain;
            return options;
        }
    }

    public static final Creator<GoogleSignInOptions> CREATOR = new AutoCreator<>(GoogleSignInOptions.class);
}
