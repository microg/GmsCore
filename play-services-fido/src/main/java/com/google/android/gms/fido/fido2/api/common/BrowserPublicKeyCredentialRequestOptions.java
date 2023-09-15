/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * Parameters for a signature request from a Web Browser.
 */
@PublicApi
public class BrowserPublicKeyCredentialRequestOptions extends BrowserRequestOptions {
    @Field(2)
    @NonNull
    private PublicKeyCredentialRequestOptions delegate;
    @Field(3)
    @NonNull
    private Uri origin;
    @Field(4)
    @Nullable
    private byte[] clientDataHash;

    @Override
    @Nullable
    public AuthenticationExtensions getAuthenticationExtensions() {
        return delegate.getAuthenticationExtensions();
    }

    @Override
    @NonNull
    public byte[] getChallenge() {
        return delegate.getChallenge();
    }

    @Override
    @Nullable
    public byte[] getClientDataHash() {
        return clientDataHash;
    }

    @Override
    @NonNull
    public Uri getOrigin() {
        return origin;
    }

    @NonNull
    public PublicKeyCredentialRequestOptions getPublicKeyCredentialRequestOptions() {
        return delegate;
    }

    @Override
    @Nullable
    public Integer getRequestId() {
        return delegate.getRequestId();
    }

    @Override
    @Nullable
    public Double getTimeoutSeconds() {
        return delegate.getTimeoutSeconds();
    }

    @Override
    @Nullable
    public TokenBinding getTokenBinding() {
        return delegate.getTokenBinding();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BrowserPublicKeyCredentialRequestOptions)) return false;

        BrowserPublicKeyCredentialRequestOptions that = (BrowserPublicKeyCredentialRequestOptions) o;

        if (delegate != null ? !delegate.equals(that.delegate) : that.delegate != null) return false;
        if (origin != null ? !origin.equals(that.origin) : that.origin != null) return false;
        return Arrays.equals(clientDataHash, that.clientDataHash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{delegate, origin, Arrays.hashCode(clientDataHash)});
    }

    @Override
    @NonNull
    public String toString() {
        return ToStringHelper.name("BrowserPublicKeyCredentialRequestOptions")
                .value(delegate)
                .field("origin", origin)
                .field("clientDataHash", clientDataHash)
                .end();
    }

    /**
     * Builder for {@link BrowserPublicKeyCredentialRequestOptions}.
     */
    public static class Builder {
        @NonNull
        private PublicKeyCredentialRequestOptions delegate;
        @NonNull
        private Uri origin;
        @Nullable
        private byte[] clientDataHash;

        /**
         * The constructor of {@link BrowserPublicKeyCredentialRequestOptions.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets a clientDataHash value to sign over in place of assembling and hashing clientDataJSON during the
         * signature request.
         * <p>
         * Note: This is optional and only provided for contexts where the unhashed information necessary to assemble
         * WebAuthn clientDataJSON is not available. If set, the resulting {@link AuthenticatorAssertionResponse} will
         * return an invalid value for {@code getClientDataJSON()}. Generally, browser clients should use
         * {@link PublicKeyCredentialRequestOptions.Builder#setChallenge(byte[])} instead.
         *
         * @return
         */
        public Builder setClientDataHash(@NonNull byte[] clientDataHash) {
            this.clientDataHash = clientDataHash;
            return this;
        }

        /**
         * Sets the origin on whose behalf the calling browser is requesting an authentication operation.
         */
        public Builder setOrigin(@NonNull Uri origin) {
            this.origin = origin;
            return this;
        }

        /**
         * Sets the parameters to dictate client behavior during this authentication session.
         */
        public Builder setPublicKeyCredentialRequestOptions(@NonNull PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions) {
            this.delegate = publicKeyCredentialRequestOptions;
            return this;
        }

        /**
         * Builds the {@link BrowserPublicKeyCredentialRequestOptions} object.
         */
        @NonNull
        public BrowserPublicKeyCredentialRequestOptions build() {
            BrowserPublicKeyCredentialRequestOptions options = new BrowserPublicKeyCredentialRequestOptions();
            options.delegate = delegate;
            options.origin = origin;
            options.clientDataHash = clientDataHash;
            return options;
        }
    }

    public static BrowserPublicKeyCredentialRequestOptions deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    public static final Creator<BrowserPublicKeyCredentialRequestOptions> CREATOR = new AutoCreator<>(BrowserPublicKeyCredentialRequestOptions.class);
}
