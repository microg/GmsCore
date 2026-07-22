/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.signin;

import android.accounts.Account;
import android.net.Uri;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.GoogleApiClient;
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
 * Class that holds the basic account information of the signed in Google user.
 */
@SafeParcelable.Class
public class GoogleSignInAccount extends AbstractSafeParcelable {
    @Field(value = 1, defaultValue = "3")
    final int versionCode;
    @Field(value = 2, getterName = "getId")
    @Nullable
    private final String id;
    @Field(value = 3, getterName = "getIdToken")
    @Nullable
    private final String idToken;
    @Field(value = 4, getterName = "getEmail")
    @Nullable
    private final String email;
    @Field(value = 5, getterName = "getDisplayName")
    @Nullable
    private final String displayName;
    @Field(value = 6, getterName = "getPhotoUrl")
    @Nullable
    private final Uri photoUrl;
    @Field(value = 7, getterName = "getServerAuthCode")
    private final String serverAuthCode;
    @Field(value = 8, getterName = "getExpirationTimeSecs")
    private final long expirationTimeSecs;
    @Field(value = 9, getterName = "getObfuscatedIdentifier")
    private final String obfuscatedIdentifier;
    @Field(value = 10, getter = "new java.util.ArrayList<>($object.getGrantedScopes())")
    private final List<Scope> grantedScopes;
    @Field(value = 11, getterName = "getGivenName")
    @Nullable
    private final String givenName;
    @Field(value = 12, getterName = "getFamilyName")
    @Nullable
    private final String familyName;

    private Set<Scope> requestedScopes;

    private static final String JSON_ID = "id";
    private static final String JSON_TOKEN_ID = "tokenId";
    private static final String JSON_EMAIL = "email";
    private static final String JSON_DISPLAY_NAME = "displayName";
    private static final String JSON_GIVEN_NAME = "givenName";
    private static final String JSON_FAMILY_NAME = "familyName";
    private static final String JSON_PHOTO_URL = "photoUrl";
    private static final String JSON_SERVER_AUTH_CODE = "serverAuthCode";
    private static final String JSON_EXPIRATION_TIME = "expirationTime";
    private static final String JSON_OBFUSCATED_IDENTIFIER = "obfuscatedIdentifier";
    private static final String JSON_GRANTED_SCOPES = "grantedScopes";

    @Hide
    public GoogleSignInAccount(@Nullable String id, @Nullable String idToken, @Nullable String email, @Nullable String displayName, @Nullable Uri photoUrl, String serverAuthCode, long expirationTimeSecs, String obfuscatedIdentifier, Set<Scope> grantedScopes, @Nullable String givenName, @Nullable String familyName) {
        this(3, id, idToken, email, displayName, photoUrl, serverAuthCode, expirationTimeSecs, obfuscatedIdentifier, new ArrayList<>(grantedScopes), givenName, familyName);
    }

    @Constructor
    GoogleSignInAccount(@Param(1) int versionCode, @Param(2) @Nullable String id, @Param(3) @Nullable String idToken, @Param(4) @Nullable String email, @Param(5) @Nullable String displayName, @Param(6) @Nullable Uri photoUrl, @Param(7) String serverAuthCode, @Param(8) long expirationTimeSecs, @Param(9) String obfuscatedIdentifier, @Param(10) List<Scope> grantedScopes, @Param(11) @Nullable String givenName, @Param(12) @Nullable String familyName) {
        this.versionCode = versionCode;
        this.id = id;
        this.idToken = idToken;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.serverAuthCode = serverAuthCode;
        this.expirationTimeSecs = expirationTimeSecs;
        this.obfuscatedIdentifier = obfuscatedIdentifier;
        this.grantedScopes = grantedScopes;
        this.givenName = givenName;
        this.familyName = familyName;
    }

    @NonNull
    @Hide
    public static GoogleSignInAccount createDefault() {
        return fromAccount(new Account(GoogleApiClient.DEFAULT_ACCOUNT, AuthConstants.DEFAULT_ACCOUNT_TYPE));
    }

    @NonNull
    @Hide
    public static GoogleSignInAccount fromAccount(@NonNull Account account) {
        return fromAccountAndScopes(account, new HashSet<>());
    }

    @NonNull
    @Hide
    public static GoogleSignInAccount fromAccountAndScopes(@NonNull Account account, @NonNull Scope scope, @NonNull Scope... scopes) {
        Set<Scope> scopeSet = new HashSet<Scope>();
        scopeSet.add(scope);
        Collections.addAll(scopeSet, scopes);
        return fromAccountAndScopes(account, scopeSet);
    }

    @NonNull
    @Hide
    public static GoogleSignInAccount fromAccountAndScopes(@NonNull Account account, @NonNull Set<Scope> scopes) {
        return new GoogleSignInAccount(null, null, account.name, null, null, null, 0L, account.name, scopes, null, null);
    }

    /**
     * A convenient wrapper for {@link #getEmail()} which returns an android.accounts.Account object. See {@link #getEmail()} doc for details.
     */
    public @Nullable Account getAccount() {
        if (email == null) return null;
        return new Account(email, AuthConstants.DEFAULT_ACCOUNT_TYPE);
    }

    /**
     * Returns the display name of the signed in user if you built your configuration starting from
     * {@code new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)} or with {@link GoogleSignInOptions.Builder#requestProfile()} configured;
     * {@code null} otherwise. Not guaranteed to be present for all users, even when configured.
     */
    public @Nullable String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the email address of the signed in user if {@link GoogleSignInOptions.Builder#requestEmail()} was configured; {@code null} otherwise.
     * <p>
     * Applications should not key users by email address since a Google account's email address can change. Use {@link #getId()} as a key instead.
     * <p>
     * Important: Do not use this returned email address to communicate the currently signed in user to your backend server. Instead, send an ID token
     * ({@link GoogleSignInOptions.Builder#requestIdToken(String)}), which can be securely validated on the server; or send server auth code
     * ({@link GoogleSignInOptions.Builder#requestServerAuthCode(String)}) which can be in turn exchanged for id token.
     *
     * @return
     */
    public @Nullable String getEmail() {
        return email;
    }

    /**
     * Returns the family name of the signed in user if you built your configuration starting from
     * {@code new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)} or with {@link GoogleSignInOptions.Builder#requestProfile()} configured;
     * {@code null} otherwise. Not guaranteed to be present for all users, even when configured.
     */
    public @Nullable String getFamilyName() {
        return familyName;
    }

    /**
     * Returns the given name of the signed in user if you built your configuration starting from
     * {@code new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)} or with {@link GoogleSignInOptions.Builder#requestProfile()} configured;
     * {@code null} otherwise. Not guaranteed to be present for all users, even when configured.
     */
    public @Nullable String getGivenName() {
        return givenName;
    }

    /**
     * Returns all scopes that have been authorized to your application.
     * <p>
     * This can be a larger set than what you have requested via {@link GoogleSignInOptions}. We recommend apps requesting minimum scopes at user sign in time
     * and later requesting additional scopes incrementally when user is using a certain feature. For those apps following this incremental auth practice,
     * they can use the returned scope set to determine all authorized scopes (across platforms and app re-installs) to turn on bonus features accordingly.
     * The returned set can also be larger due to other scope handling logic.
     */
    public @NonNull Set<Scope> getGrantedScopes() {
        return new HashSet<>(grantedScopes);
    }

    /**
     * Returns the unique ID for the Google account if you built your configuration starting from
     * {@code new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)} or with {@link GoogleSignInOptions.Builder#requestId()} configured;
     * {@code null} otherwise.
     * <p>
     * This is the preferred unique key to use for a user record.
     * <p>
     * Important: Do not use this returned Google ID to communicate the currently signed in user to your backend server. Instead, send an ID token
     * ({@link GoogleSignInOptions.Builder#requestIdToken(String)}), which can be securely validated on the server; or send a server auth code
     * ({@link GoogleSignInOptions.Builder#requestServerAuthCode(String)}) which can be in turn exchanged for id token.
     */
    public @Nullable String getId() {
        return id;
    }

    /**
     * Returns an ID token that you can send to your server if {@link GoogleSignInOptions.Builder#requestIdToken(String)} was configured; {@code null} otherwise.
     * <p>
     * ID token is a JSON Web Token signed by Google that can be used to identify a user to a backend.
     */
    public @Nullable String getIdToken() {
        return idToken;
    }

    /**
     * Returns the photo url of the signed in user if the user has a profile picture and you built your configuration either starting from
     * {@code new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)} or with {@link GoogleSignInOptions.Builder#requestProfile()} configured;
     * {@code null} otherwise. Not guaranteed to be present for all users, even when configured.
     */
    public @Nullable Uri getPhotoUrl() {
        return photoUrl;
    }

    /**
     * Returns a one-time server auth code to send to your web server which can be exchanged for access token and sometimes refresh token if
     * {@link GoogleSignInOptions.Builder#requestServerAuthCode(String)} is configured; {@code null} otherwise.
     */
    public @Nullable String getServerAuthCode() {
        return serverAuthCode;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof GoogleSignInAccount)) return false;
        return ((GoogleSignInAccount) obj).obfuscatedIdentifier.equals(obfuscatedIdentifier) && ((GoogleSignInAccount) obj).getGrantedScopes().equals(getGrantedScopes());
    }

    @Hide
    public long getExpirationTimeSecs() {
        return expirationTimeSecs;
    }

    @Hide
    public String getObfuscatedIdentifier() {
        return obfuscatedIdentifier;
    }

    @Hide
    public Set<Scope> getRequestedScopes() {
        Set<Scope> requestedScopes = new HashSet<Scope>(this.grantedScopes);
        requestedScopes.addAll(this.requestedScopes);
        return requestedScopes;
    }

    @Hide
    public boolean isExpired() {
        return System.currentTimeMillis() / 1000 >= this.expirationTimeSecs - 300;
    }

    @NonNull
    @Hide
    public GoogleSignInAccount requestExtraScopes(@NonNull Scope... scopes) {
        if (scopes != null) {
            Collections.addAll(this.requestedScopes, scopes);
        }
        return this;
    }

    @Nullable
    @Hide
    public static GoogleSignInAccount fromJson(@Nullable String jsonString) throws JSONException {
        if (jsonString == null) return null;
        JSONObject json = new JSONObject(jsonString);
        Set<Scope> grantedScopes = new HashSet<>();
        JSONArray jsonGrantedScopes = json.getJSONArray(JSON_GRANTED_SCOPES);
        for (int i = 0; i < jsonGrantedScopes.length(); i++) {
            grantedScopes.add(new Scope(jsonGrantedScopes.getString(i)));
        }
        return new GoogleSignInAccount(
                json.optString(JSON_ID),
                json.has(JSON_TOKEN_ID) ? json.optString(JSON_TOKEN_ID) : null,
                json.has(JSON_EMAIL) ? json.optString(JSON_EMAIL) : null,
                json.has(JSON_DISPLAY_NAME) ? json.optString(JSON_DISPLAY_NAME) : null,
                json.has(JSON_PHOTO_URL) ? Uri.parse(json.optString(JSON_PHOTO_URL)) : null,
                json.has(JSON_SERVER_AUTH_CODE) ? json.optString(JSON_SERVER_AUTH_CODE) : null,
                Long.parseLong(json.getString(JSON_EXPIRATION_TIME)),
                json.getString(JSON_OBFUSCATED_IDENTIFIER),
                grantedScopes,
                json.has(JSON_GIVEN_NAME) ? json.optString(JSON_GIVEN_NAME) : null,
                json.has(JSON_FAMILY_NAME) ? json.optString(JSON_FAMILY_NAME) : null
        );
    }

    @Hide
    @NonNull
    public String toJson() {
        JSONObject json = new JSONObject();
        try {
            if (id != null) json.put(JSON_ID, id);
            if (idToken != null) json.put(JSON_TOKEN_ID, idToken);
            if (email != null) json.put(JSON_EMAIL, email);
            if (displayName != null) json.put(JSON_DISPLAY_NAME, displayName);
            if (givenName != null) json.put(JSON_GIVEN_NAME, givenName);
            if (familyName != null) json.put(JSON_FAMILY_NAME, familyName);
            if (photoUrl != null) json.put(JSON_PHOTO_URL, photoUrl.toString());
            if (serverAuthCode != null) json.put(JSON_SERVER_AUTH_CODE, serverAuthCode);
            json.put(JSON_EXPIRATION_TIME, expirationTimeSecs);
            json.put(JSON_OBFUSCATED_IDENTIFIER, obfuscatedIdentifier);
            JSONArray jsonGrantedScopes = new JSONArray();
            Scope[] grantedScopesArray = grantedScopes.toArray(new Scope[grantedScopes.size()]);
            Arrays.sort(grantedScopesArray, (s1, s2) -> s1.getScopeUri().compareTo(s2.getScopeUri()));
            for (Scope grantedScope : grantedScopesArray) {
                jsonGrantedScopes.put(grantedScope.getScopeUri());
            }
            json.put(JSON_GRANTED_SCOPES, jsonGrantedScopes);
            return json.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        return (obfuscatedIdentifier.hashCode() + 527) * 31 + getGrantedScopes().hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("GoogleSignInAccount")
                .field("id", id)
                .field("tokenId", idToken)
                .field("email", email)
                .field("displayName", displayName)
                .field("givenName", givenName)
                .field("familyName", familyName)
                .field("photoUrl", photoUrl)
                .field("serverAuthCode", serverAuthCode)
                .field("expirationTime", expirationTimeSecs)
                .field("obfuscatedIdentifier", obfuscatedIdentifier)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        CREATOR.writeToParcel(this, parcel, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleSignInAccount> CREATOR = findCreator(GoogleSignInAccount.class);
}
