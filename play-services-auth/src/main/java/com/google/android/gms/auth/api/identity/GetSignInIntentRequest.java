/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

import java.util.Arrays;
import java.util.Objects;

/**
 * Request to get a Google sign-in intent.
 */
@SafeParcelable.Class
public class GetSignInIntentRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getServerClientId")
    private final String serverClientId;
    @Field(value = 2, getterName = "getHostedDomainFilter")
    private final String hostedDomainFilter;
    @Field(value = 3, getterName = "getSessionId")
    private final String sessionId;
    @Field(value = 4, getterName = "getNonce")
    private final String nonce;
    @Field(value = 5, getterName = "requestVerifiedPhoneNumber")
    private final boolean requestVerifiedPhoneNumber;
    @Field(value = 6, getterName = "getTheme")
    private final int theme;

    @Constructor
    GetSignInIntentRequest(@Param(1) String serverClientId, @Param(2) String hostedDomainFilter, @Param(3) String sessionId, @Param(4) String nonce, @Param(5) boolean requestVerifiedPhoneNumber, @Param(6) int theme) {
        this.serverClientId = serverClientId;
        this.hostedDomainFilter = hostedDomainFilter;
        this.sessionId = sessionId;
        this.nonce = nonce;
        this.requestVerifiedPhoneNumber = requestVerifiedPhoneNumber;
        this.theme = theme;
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the {@code hostedDomainFilter} if it was set in the request, or {@code null} otherwise.
     */
    public String getHostedDomainFilter() {
        return hostedDomainFilter;
    }

    /**
     * Returns the nonce that was set in the request.
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Returns the {@code serverClientId} that was set in the request.
     */
    public String getServerClientId() {
        return serverClientId;
    }

    @Hide
    public String getSessionId() {
        return sessionId;
    }

    @Hide
    public int getTheme() {
        return theme;
    }

    /**
     * Returns whether a verified phone number is requested.
     *
     * @deprecated No replacement.
     */
    @Deprecated
    public boolean requestVerifiedPhoneNumber() {
        return requestVerifiedPhoneNumber;
    }

    /**
     * Builder class for {@link GetSignInIntentRequest}.
     */
    public static class Builder {
        private String serverClientId;
        private String hostedDomainFilter;
        private String sessionId;
        private String nonce;
        private boolean requestVerifiedPhoneNumber;
        private int theme;

        @NonNull
        public GetSignInIntentRequest build() {
            return new GetSignInIntentRequest(serverClientId, hostedDomainFilter, sessionId, nonce, requestVerifiedPhoneNumber, theme);
        }

        /**
         * Sets the hosted domain filter (e.g. myuniveristy.edu). Default is no filter.
         */
        @NonNull
        public Builder filterByHostedDomain(@Nullable String hostedDomainFilter) {
            this.hostedDomainFilter = hostedDomainFilter;
            return this;
        }

        /**
         * Sets the nonce to use when generating a Google ID token. The values for nonce can be any random string and is used to
         * prevent replay-attack. Default is no nonce.
         *
         * @param nonce the nonce to use during ID token generation
         */
        @NonNull
        public Builder setNonce(@Nullable String nonce) {
            this.nonce = nonce;
            return this;
        }

        /**
         * Sets whether to prompt the users to share a verified phone number associated with their Google accounts.
         * <p>
         * Note that if the user selects a Google account which has previously been used to sign up to your app, they won't be
         * prompted with the phone number selection.
         *
         * @deprecated No replacement.
         */
        @Deprecated
        @NonNull
        public Builder setRequestVerifiedPhoneNumber(boolean requestsVerifiedPhoneNumber) {
            this.requestVerifiedPhoneNumber = requestsVerifiedPhoneNumber;
            return this;
        }

        /**
         * Sets the client ID of the server that will verify the integrity of the token. Calling this method to set the {@code serverClientId} is
         * required.
         */
        @NonNull
        public Builder setServerClientId(@NonNull String serverClientId) {
            this.serverClientId = serverClientId;
            return this;
        }

        @NonNull
        public Builder setSessionId(@Nullable String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        @NonNull
        public Builder setTheme(int theme) {
            this.theme = theme;
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetSignInIntentRequest)) return false;

        GetSignInIntentRequest that = (GetSignInIntentRequest) o;

        if (requestVerifiedPhoneNumber != that.requestVerifiedPhoneNumber) return false;
        if (theme != that.theme) return false;
        if (!Objects.equals(serverClientId, that.serverClientId)) return false;
        if (!Objects.equals(hostedDomainFilter, that.hostedDomainFilter)) return false;
        if (!Objects.equals(sessionId, that.sessionId)) return false;
        return Objects.equals(nonce, that.nonce);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{serverClientId, hostedDomainFilter, serverClientId, nonce, requestVerifiedPhoneNumber, theme});
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetSignInIntentRequest> CREATOR = findCreator(GetSignInIntentRequest.class);

}
