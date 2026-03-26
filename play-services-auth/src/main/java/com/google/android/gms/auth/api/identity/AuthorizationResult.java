/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.app.PendingIntent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

import java.util.List;
import java.util.Objects;

/**
 * Result returned from a request to authorize
 */
@SafeParcelable.Class
public class AuthorizationResult extends AbstractSafeParcelable {

    @Nullable
    @Field(value = 1, getterName = "getServerAuthCode")
    private final String serverAuthCode;
    @Nullable
    @Field(value = 2, getterName = "getAccessToken")
    private final String accessToken;
    @Nullable
    @Field(value = 3, getterName = "getIdToken")
    private final String idToken;
    @NonNull
    @Field(value = 4, getterName = "getGrantedScopes")
    private final List<String> grantedScopes;
    @Nullable
    @Field(value = 5, getterName = "toGoogleSignInAccount")
    private final GoogleSignInAccount googleSignInAccount;
    @Nullable
    @Field(value = 6, getterName = "getPendingIntent")
    private final PendingIntent pendingIntent;

    public static final SafeParcelableCreatorAndWriter<AuthorizationResult> CREATOR = findCreator(AuthorizationResult.class);

    @Constructor
    public AuthorizationResult(@Nullable @Param(1) String serverAuthCode, @Nullable @Param(2) String accessToken, @Nullable @Param(3) String idToken, @NonNull @Param(4) List<String> grantedScopes, @Nullable @Param(5) GoogleSignInAccount googleSignInAccount, @Nullable @Param(6) PendingIntent pendingIntent) {
        this.serverAuthCode = serverAuthCode;
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.grantedScopes = grantedScopes;
        this.googleSignInAccount = googleSignInAccount;
        this.pendingIntent = pendingIntent;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof AuthorizationResult)) return false;

        AuthorizationResult that = (AuthorizationResult) o;
        return Objects.equals(serverAuthCode, that.serverAuthCode) && Objects.equals(accessToken, that.accessToken) && Objects.equals(idToken, that.idToken) && grantedScopes.equals(that.grantedScopes) && Objects.equals(pendingIntent, that.pendingIntent) && Objects.equals(googleSignInAccount, that.googleSignInAccount);
    }

    /**
     * Returns the access token.
     */
    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Returns the list of scopes that user had granted access to.
     */
    @NonNull
    public List<String> getGrantedScopes() {
        return grantedScopes;
    }

    @Hide
    @Nullable
    public String getIdToken() {
        return idToken;
    }

    /**
     * Returns the {@link PendingIntent} that can be used to launch the authorization flow.
     */
    @Nullable
    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    /**
     * Returns the server authorization code that can be exchanged by the server for a refresh token.
     */
    @Nullable
    public String getServerAuthCode() {
        return serverAuthCode;
    }

    /**
     * Returns {@code true} if this result contains a resolution that needs to be launched.
     * <p>
     * Please see {@link #getPendingIntent()} for additional context.
     */
    public boolean hasResolution() {
        return pendingIntent != null;
    }

    /**
     * Converts this result to an equivalent {@link GoogleSignInAccount} object, if the authorization operation was successful in returning tokens. If,
     * instead, a {@link PendingIntent} was provided to launch the authorization flow, this will return {@code null}.
     *
     * @return a {@link GoogleSignInAccount} object with the same data contained in this result.
     */
    @Nullable
    public GoogleSignInAccount toGoogleSignInAccount() {
        return googleSignInAccount;
    }

    @Hide
    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("AuthorizationResult")
                .field("serverAuthCode", serverAuthCode)
                .field("accessToken", accessToken)
                .field("idToken", idToken)
                .field("grantedScopes", grantedScopes)
                .field("pendingIntent", pendingIntent)
                .field("googleSignInAccount", googleSignInAccount)
                .end();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{serverAuthCode, accessToken, idToken, grantedScopes, pendingIntent, googleSignInAccount});
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
