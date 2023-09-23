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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.Scope;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.microg.gms.auth.AuthConstants;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that holds the basic account information of the signed in Google user.
 */
public class GoogleSignInAccount extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 3;
    @Field(2)
    @Nullable
    private String id;
    @Field(3)
    @Nullable
    private String tokenId;
    @Field(4)
    @Nullable
    private String email;
    @Field(5)
    @Nullable
    private String displayName;
    @Field(6)
    @Nullable
    private Uri photoUrl;
    @Field(7)
    private String serverAuthCode;
    @Field(8)
    private long expirationTime;
    @Field(9)
    private String obfuscatedIdentifier;
    @Field(10)
    private ArrayList<Scope> grantedScopes;
    @Field(11)
    @Nullable
    private String givenName;
    @Field(12)
    @Nullable
    private String familyName;

    private GoogleSignInAccount() {
    }

    @Hide
    public GoogleSignInAccount(@Nullable String id, @Nullable String tokenId, @Nullable String email, @Nullable String displayName, @Nullable Uri photoUrl, String serverAuthCode, long expirationTime, String obfuscatedIdentifier, Set<Scope> grantedScopes, @Nullable String givenName, @Nullable String familyName) {
        this.id = id;
        this.tokenId = tokenId;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.serverAuthCode = serverAuthCode;
        this.expirationTime = expirationTime;
        this.obfuscatedIdentifier = obfuscatedIdentifier;
        this.grantedScopes = new ArrayList<>(grantedScopes);
        this.givenName = givenName;
        this.familyName = familyName;
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
        return tokenId;
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
    public String getObfuscatedIdentifier() {
        return obfuscatedIdentifier;
    }

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
    public static GoogleSignInAccount fromJson(@Nullable String jsonString) throws JSONException {
        if (jsonString == null) return null;
        JSONObject json = new JSONObject(jsonString);
        GoogleSignInAccount account = new GoogleSignInAccount();
        account.id = json.optString(JSON_ID);
        account.tokenId = json.has(JSON_TOKEN_ID) ? json.optString(JSON_TOKEN_ID) : null;
        account.email = json.has(JSON_EMAIL) ? json.optString(JSON_EMAIL) : null;
        account.displayName = json.has(JSON_DISPLAY_NAME) ? json.optString(JSON_DISPLAY_NAME) : null;
        account.givenName = json.has(JSON_GIVEN_NAME) ? json.optString(JSON_GIVEN_NAME) : null;
        account.familyName = json.has(JSON_FAMILY_NAME) ? json.optString(JSON_FAMILY_NAME) : null;
        account.photoUrl = json.has(JSON_PHOTO_URL) ? Uri.parse(json.optString(JSON_PHOTO_URL)) : null;
        account.serverAuthCode = json.has(JSON_SERVER_AUTH_CODE) ? json.optString(JSON_SERVER_AUTH_CODE) : null;
        account.expirationTime = Long.parseLong(json.getString(JSON_EXPIRATION_TIME));
        account.obfuscatedIdentifier = json.getString(JSON_OBFUSCATED_IDENTIFIER);
        account.grantedScopes = new ArrayList<>();
        JSONArray jsonGrantedScopes = json.getJSONArray(JSON_GRANTED_SCOPES);
        for (int i = 0; i < jsonGrantedScopes.length(); i++) {
            account.grantedScopes.add(new Scope(jsonGrantedScopes.getString(i)));
        }
        return account;
    }

    @Hide
    @NonNull
    public String toJson() {
        JSONObject json = new JSONObject();
        try {
            if (id != null) json.put(JSON_ID, id);
            if (tokenId != null) json.put(JSON_TOKEN_ID, tokenId);
            if (email != null) json.put(JSON_EMAIL, email);
            if (displayName != null) json.put(JSON_DISPLAY_NAME, displayName);
            if (givenName != null) json.put(JSON_GIVEN_NAME, givenName);
            if (familyName != null) json.put(JSON_FAMILY_NAME, familyName);
            if (photoUrl != null) json.put(JSON_PHOTO_URL, photoUrl.toString());
            if (serverAuthCode != null) json.put(JSON_SERVER_AUTH_CODE, serverAuthCode);
            json.put(JSON_EXPIRATION_TIME, expirationTime);
            json.put(JSON_OBFUSCATED_IDENTIFIER, obfuscatedIdentifier);
            JSONArray jsonGrantedScopes = new JSONArray();
            for (Scope grantedScope : grantedScopes) {
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
                .field("tokenId", tokenId)
                .field("email", email)
                .field("displayName", displayName)
                .field("givenName", givenName)
                .field("familyName", familyName)
                .field("photoUrl", photoUrl)
                .field("serverAuthCode", serverAuthCode)
                .field("expirationTime", expirationTime)
                .field("obfuscatedIdentifier", obfuscatedIdentifier)
                .end();
    }

    public static final Creator<GoogleSignInAccount> CREATOR = new AutoCreator<>(GoogleSignInAccount.class);
}
