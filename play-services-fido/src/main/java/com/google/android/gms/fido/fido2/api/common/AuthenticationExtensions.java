/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

/**
 * Represents extensions that can be passed into FIDO2 APIs. This container class corresponds to the additional
 * parameters requesting additional processing by authenticators.
 * <p>
 * Note that rather than accepting arbitrary objects as specified in WebAuthn, this class requires a structured entry
 * for each supported extension.
 */
@PublicApi
public class AuthenticationExtensions extends AutoSafeParcelable {
    @Field(2)
    @Nullable
    private FidoAppIdExtension fidoAppIdExtension;
    @Field(3)
    @Nullable
    private CableAuthenticationExtension cableAuthenticationExtension;
    @Field(4)
    @Nullable
    private UserVerificationMethodExtension userVerificationMethodExtension;

    @Nullable
    public FidoAppIdExtension getFidoAppIdExtension() {
        return fidoAppIdExtension;
    }

    @Nullable
    public UserVerificationMethodExtension getUserVerificationMethodExtension() {
        return userVerificationMethodExtension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticationExtensions)) return false;

        AuthenticationExtensions that = (AuthenticationExtensions) o;

        if (fidoAppIdExtension != null ? !fidoAppIdExtension.equals(that.fidoAppIdExtension) : that.fidoAppIdExtension != null)
            return false;
        if (cableAuthenticationExtension != null ? !cableAuthenticationExtension.equals(that.cableAuthenticationExtension) : that.cableAuthenticationExtension != null)
            return false;
        return userVerificationMethodExtension != null ? userVerificationMethodExtension.equals(that.userVerificationMethodExtension) : that.userVerificationMethodExtension == null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{fidoAppIdExtension, cableAuthenticationExtension, userVerificationMethodExtension});
    }

    @Override
    public String toString() {
        return ToStringHelper.name("AuthenticationExtensions")
                .field("fidoAppIdExtension", fidoAppIdExtension != null ? fidoAppIdExtension.getAppId() : null)
                .field("cableAuthenticationExtension", cableAuthenticationExtension)
                .field("userVerificationMethodExtension", userVerificationMethodExtension != null ? userVerificationMethodExtension.getUvm() : null)
                .end();
    }

    /**
     * Builder for {@link AuthenticationExtensions}.
     */
    public static class Builder {
        @Nullable
        private FidoAppIdExtension fidoAppIdExtension;
        @Nullable
        private UserVerificationMethodExtension userVerificationMethodExtension;

        /**
         * The constructor of {@link AuthenticationExtensions.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets the App ID extension, which allows for authentication of U2F authenticators previously registered
         * under the supplied App ID.
         */
        public Builder setFido2Extension(@Nullable FidoAppIdExtension appIdExtension) {
            this.fidoAppIdExtension = appIdExtension;
            return this;
        }

        /**
         * Sets the User Verification Method extension, which allows the relying party to ascertain up to three
         * authentication methods that were used.
         */
        public Builder setUserVerificationMethodExtension(@Nullable UserVerificationMethodExtension userVerificationMethodExtension) {
            this.userVerificationMethodExtension = userVerificationMethodExtension;
            return this;
        }

        /**
         * Builds the {@link AuthenticationExtensions} object.
         */
        @NonNull
        public AuthenticationExtensions build() {
            AuthenticationExtensions extensions = new AuthenticationExtensions();
            extensions.fidoAppIdExtension = fidoAppIdExtension;
            extensions.userVerificationMethodExtension = userVerificationMethodExtension;
            return extensions;
        }
    }

    public static final Creator<AuthenticationExtensions> CREATOR = new AutoCreator<>(AuthenticationExtensions.class);
}
