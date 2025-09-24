/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.signin;

import android.accounts.Account;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.internal.GoogleSignInOptionsExtensionParcelable;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.microg.gms.auth.AuthConstants;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

import java.util.*;

/**
 * {@code GoogleSignInOptions} contains options used to configure the {@link Auth#GOOGLE_SIGN_IN_API}.
 */
@SafeParcelable.Class
public class GoogleSignInOptions extends AbstractSafeParcelable implements Api.ApiOptions.Optional {
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
    public static final GoogleSignInOptions DEFAULT_GAMES_SIGN_IN = new Builder().requestScopes(new Scope(Scopes.GAMES_LITE)).build();

    /**
     * Default configuration for Google Sign In. You can get a stable user ID and basic profile info back via {@link GoogleSignInAccount#getId()} after you
     * trigger sign in from either {@link GoogleSignInApi#silentSignIn} or {@link GoogleSignInApi#getSignInIntent}. If you require more information for the
     * sign in result, please build a configuration via {@code new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)}.
     */
    @NonNull
    public static final GoogleSignInOptions DEFAULT_SIGN_IN = new Builder().requestId().requestProfile().build();

    @Field(value = 1, defaultValue = "3")
    final int versionCode;
    @Field(value = 2, getterName = "getScopes")
    private final List<Scope> scopes;
    @Field(value = 3, getterName = "getAccount")
    private final Account account;
    @Field(value = 4, getterName = "isIdTokenRequested")
    private final boolean idTokenRequested;
    @Field(value = 5, getterName = "isServerAuthCodeRequested")
    private final boolean serverAuthCodeRequested;
    @Field(value = 6, getterName = "isForceCodeForRefreshToken")
    private final boolean forceCodeForRefreshToken;
    @Field(value = 7, getterName = "getServerClientId")
    private final String serverClientId;
    @Field(value = 8, getterName = "getHostedDomain")
    private final String hostedDomain;
    @Field(value = 9, getterName = "getExtensions")
    private final List<GoogleSignInOptionsExtensionParcelable> extensions;
    @Field(value = 10, getterName = "getLogSessionId")
    private final String logSessionId;

    @Hide
    public static final Scope SCOPE_PROFILE = new Scope(Scopes.PROFILE);
    @Hide
    public static final Scope SCOPE_EMAIL = new Scope(Scopes.EMAIL);
    @Hide
    public static final Scope SCOPE_OPENID = new Scope(Scopes.OPENID);
    @Hide
    public static final Scope SCOPE_GAMES_LITE = new Scope(Scopes.GAMES_LITE);
    @Hide
    public static final Scope SCOPE_GAMES = new Scope(Scopes.GAMES);

    private GoogleSignInOptions(List<Scope> scopes, Account account, boolean idTokenRequested, boolean serverAuthCodeRequested, boolean forceCodeForRefreshToken, String serverClientId, String hostedDomain, Map<Integer, GoogleSignInOptionsExtensionParcelable> extensions, String logSessionId) {
        this(3, scopes, account, idTokenRequested, serverAuthCodeRequested, forceCodeForRefreshToken, serverClientId, hostedDomain, new ArrayList<>(extensions.values()), logSessionId);
    }

    @Constructor
    GoogleSignInOptions(@Param(1) int versionCode, @Param(2) List<Scope> scopes, @Param(3) Account account, @Param(4) boolean idTokenRequested, @Param(5) boolean serverAuthCodeRequested, @Param(6) boolean forceCodeForRefreshToken, @Param(7) String serverClientId, @Param(8) String hostedDomain, @Param(9) List<GoogleSignInOptionsExtensionParcelable> extensions, @Param(10) String logSessionId) {
        this.versionCode = versionCode;
        this.scopes = scopes;
        this.account = account;
        this.idTokenRequested = idTokenRequested;
        this.serverAuthCodeRequested = serverAuthCodeRequested;
        this.forceCodeForRefreshToken = forceCodeForRefreshToken;
        this.serverClientId = serverClientId;
        this.hostedDomain = hostedDomain;
        this.extensions = extensions;
        this.logSessionId = logSessionId;
    }

    /**
     * Gets an array of all the requested scopes. If you use DEFAULT_SIGN_IN, this array will also include those scopes set by default in DEFAULT_SIGN_IN.
     * <p>
     * A usage of this method could be set the scopes for the contextual SignInButton. E.g., {@code signInButton.setScopes(googleSignInOptions.getScopeArray())}
     */
    @NonNull
    public Scope[] getScopeArray() {
        return scopes.toArray(new Scope[0]);
    }

    @Hide
    public List<Scope> getScopes() {
        return Collections.unmodifiableList(scopes);
    }

    @Hide
    public Account getAccount() {
        return account;
    }

    @Hide
    public boolean isIdTokenRequested() {
        return idTokenRequested;
    }

    @Hide
    public boolean isServerAuthCodeRequested() {
        return serverAuthCodeRequested;
    }

    @Hide
    public boolean isForceCodeForRefreshToken() {
        return forceCodeForRefreshToken;
    }

    @Hide
    public String getServerClientId() {
        return serverClientId;
    }

    @Hide
    public String getHostedDomain() {
        return hostedDomain;
    }

    @Hide
    public List<GoogleSignInOptionsExtensionParcelable> getExtensions() {
        return Collections.unmodifiableList(extensions);
    }

    @Hide
    public String getLogSessionId() {
        return logSessionId;
    }

    /**
     * Builder for {@link GoogleSignInOptions}.
     */
    public static final class Builder {
        private final Set<Scope> scopes;
        private boolean idTokenRequested;
        private boolean serverAuthCodeRequested;
        private boolean forceCodeForRefreshToken;
        @Nullable
        private String serverClientId;
        @Nullable
        private Account account;
        @Nullable
        private String hostedDomain;
        private final Map<Integer, GoogleSignInOptionsExtensionParcelable> extensionMap = new HashMap<>();
        private String logSessionId;

        public Builder() {
            this.scopes = new HashSet<>();
        }

        public Builder(GoogleSignInOptions options) {
            this.scopes = new HashSet<>(options.scopes);
            this.idTokenRequested = options.idTokenRequested;
            this.serverAuthCodeRequested = options.serverAuthCodeRequested;
            this.forceCodeForRefreshToken = options.forceCodeForRefreshToken;
            this.serverClientId = options.serverClientId;
            this.account = options.account;
            this.hostedDomain = options.hostedDomain;
            if (options.extensions != null) {
                for (GoogleSignInOptionsExtensionParcelable extension : options.extensions) {
                    extensionMap.put(extension.getType(), extension);
                }
            }
        }

        /**
         * Specifies additional sign-in options via the given extension.
         *
         * @param extension A sign-in extension used to further configure API specific sign-in options. Supported values include: {@link Games.GamesOptions}.
         */
        @NonNull
        public Builder addExtension(GoogleSignInOptionsExtension extension) {
            if (this.extensionMap.containsKey(extension.getExtensionType())) {
                throw new IllegalStateException("Only one extension per type may be added");
            }
            List<Scope> scopes = extension.getImpliedScopes();
            if (scopes != null) {
                this.scopes.addAll(scopes);
            }
            this.extensionMap.put(extension.getExtensionType(), new GoogleSignInOptionsExtensionParcelable(extension));
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
            this.idTokenRequested = true;
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
            this.serverAuthCodeRequested = true;
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

        @Hide
        public GoogleSignInOptions.Builder setLogSessionId(String logSessionId) {
            this.logSessionId = logSessionId;
            return this;
        }

        /**
         * Builds the {@link GoogleSignInOptions} object.
         *
         * @return a {@link GoogleSignInOptions} instance.
         */
        @NonNull
        public GoogleSignInOptions build() {
            if (scopes.contains(SCOPE_GAMES)) {
                scopes.remove(SCOPE_GAMES_LITE);
            }
            if (idTokenRequested && (account == null || !scopes.isEmpty())) {
                requestId();
            }
            return new GoogleSignInOptions(new ArrayList<>(scopes), account, idTokenRequested, serverAuthCodeRequested, forceCodeForRefreshToken, serverClientId, hostedDomain, extensionMap, logSessionId);
        }
    }

    private static final String JSON_SCOPES = "scopes";
    private static final String JSON_ACCOUNT_NAME = "accountName";
    private static final String JSON_ID_TOKEN_REQUESTED = "idTokenRequested";
    private static final String JSON_FORCE_CODE_FOR_REFRESH_TOKEN = "forceCodeForRefreshToken";
    private static final String JSON_SERVER_AUTH_REQUESTED = "serverAuthRequested";
    private static final String JSON_SERVER_CLIENT_ID = "serverClientId";
    private static final String JSON_HOSTED_DOMAIN = "hostedDomain";

    public static GoogleSignInOptions fromJson(String jsonString) throws JSONException {
        if (jsonString == null) return null;
        JSONObject json = new JSONObject(jsonString);
        List<Scope> scopes = new ArrayList<>();
        JSONArray jsonScopes = json.getJSONArray(JSON_SCOPES);
        for (int i = 0; i < jsonScopes.length(); i++) {
            scopes.add(new Scope(jsonScopes.getString(i)));
        }
        return new GoogleSignInOptions(
                scopes,
                json.has(JSON_ACCOUNT_NAME) ? new Account(json.optString(JSON_ACCOUNT_NAME), AuthConstants.DEFAULT_ACCOUNT_TYPE) : null,
                json.getBoolean(JSON_ID_TOKEN_REQUESTED),
                json.getBoolean(JSON_SERVER_AUTH_REQUESTED),
                json.getBoolean(JSON_FORCE_CODE_FOR_REFRESH_TOKEN),
                json.has(JSON_SERVER_CLIENT_ID) ? json.optString(JSON_SERVER_CLIENT_ID) : null,
                json.has(JSON_HOSTED_DOMAIN) ? json.optString(JSON_HOSTED_DOMAIN) : null,
                new HashMap<>(),
                null
        );
    }

    @NonNull
    public String toJson() {
        JSONObject json = new JSONObject();
        try {
            JSONArray jsonScopes = new JSONArray();
            for (Scope scope : scopes) {
                jsonScopes.put(scope.getScopeUri());
            }
            json.put(JSON_SCOPES, jsonScopes);
            if (account != null) json.put(JSON_ACCOUNT_NAME, account.name);
            json.put(JSON_ID_TOKEN_REQUESTED, idTokenRequested);
            json.put(JSON_FORCE_CODE_FOR_REFRESH_TOKEN, forceCodeForRefreshToken);
            json.put(JSON_SERVER_AUTH_REQUESTED, serverAuthCodeRequested);
            if (serverClientId != null) json.put(JSON_SERVER_CLIENT_ID, serverClientId);
            if (hostedDomain != null) json.put(JSON_HOSTED_DOMAIN, hostedDomain);
            return json.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        List<String> sortedScopeStrings = new ArrayList<>();
        for (Scope scope : scopes) {
            sortedScopeStrings.add(scope.getScopeUri());
        }
        Collections.sort(sortedScopeStrings);
        int hash = sortedScopeStrings.hashCode() + 31;
        hash = (account == null ? 0 : account.hashCode()) + (hash * 31);
        hash = (serverClientId == null ? 0 : serverClientId.hashCode()) + (hash * 31);
        hash = (forceCodeForRefreshToken ? 1 : 0) + (hash * 31);
        hash = (idTokenRequested ? 1 : 0) + (hash * 31);
        hash = (serverAuthCodeRequested ? 1 : 0) + (hash * 31);
        hash = (logSessionId == null ? 0 : logSessionId.hashCode()) + (hash * 31);
        return hash;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("GoogleSignInOptions")
                .field("scopes", scopes)
                .field("account", account)
                .field("idTokenRequested", idTokenRequested)
                .field("forceCodeForRefreshToken", forceCodeForRefreshToken)
                .field("serverAuthCodeRequested", serverAuthCodeRequested)
                .field("serverClientId", serverClientId)
                .field("hostedDomain", hostedDomain)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        CREATOR.writeToParcel(this, parcel, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleSignInOptions> CREATOR = findCreator(GoogleSignInOptions.class);
}
