/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.net.Uri;

import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * Parameters to a make credential request from a Web browser.
 */
@PublicApi
public class BrowserPublicKeyCredentialCreationOptions extends BrowserRequestOptions {
    @Field(2)
    private PublicKeyCredentialCreationOptions delegate;
    @Field(3)
    private Uri origin;
    @Field(4)
    private byte[] clientDataHash;

    @Override
    public AuthenticationExtensions getAuthenticationExtensions() {
        return delegate.getAuthenticationExtensions();
    }

    @Override
    public byte[] getChallenge() {
        return delegate.getChallenge();
    }

    @Override
    public byte[] getClientDataHash() {
        return clientDataHash;
    }

    @Override
    public Uri getOrigin() {
        return origin;
    }

    public PublicKeyCredentialCreationOptions getPublicKeyCredentialCreationOptions() {
        return delegate;
    }

    @Override
    public Integer getRequestId() {
        return delegate.getRequestId();
    }

    @Override
    public Double getTimeoutSeconds() {
        return delegate.getTimeoutSeconds();
    }

    @Override
    public TokenBinding getTokenBinding() {
        return delegate.getTokenBinding();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BrowserPublicKeyCredentialCreationOptions)) return false;

        BrowserPublicKeyCredentialCreationOptions that = (BrowserPublicKeyCredentialCreationOptions) o;

        if (delegate != null ? !delegate.equals(that.delegate) : that.delegate != null) return false;
        if (origin != null ? !origin.equals(that.origin) : that.origin != null) return false;
        return Arrays.equals(clientDataHash, that.clientDataHash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{delegate, origin, Arrays.hashCode(clientDataHash)});
    }

    @Override
    public String toString() {
        return ToStringHelper.name("BrowserPublicKeyCredentialCreationOptions")
                .value(delegate)
                .field("origin", origin)
                .field("clientDataHash", clientDataHash)
                .end();
    }

    /**
     * Builder for {@link BrowserPublicKeyCredentialCreationOptions}.
     */
    public static class Builder {
        private PublicKeyCredentialCreationOptions delegate;
        private Uri origin;
        private byte[] clientDataHash;

        /**
         * The constructor of {@link BrowserPublicKeyCredentialCreationOptions.Builder}.
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
         * {@link PublicKeyCredentialCreationOptions.Builder#setChallenge(byte[])} instead.
         *
         * @return
         */
        public BrowserPublicKeyCredentialCreationOptions.Builder setClientDataHash(byte[] clientDataHash) {
            this.clientDataHash = clientDataHash;
            return this;
        }

        /**
         * Sets the origin on whose behalf the calling browser is requesting a registration operation.
         */
        public BrowserPublicKeyCredentialCreationOptions.Builder setOrigin(Uri origin) {
            this.origin = origin;
            return this;
        }

        /**
         * Sets the parameters to dictate the client behavior during this registration session.
         */
        public BrowserPublicKeyCredentialCreationOptions.Builder setPublicKeyCredentialCreationOptions(PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions) {
            this.delegate = publicKeyCredentialCreationOptions;
            return this;
        }

        /**
         * Builds the {@link BrowserPublicKeyCredentialCreationOptions} object.
         */
        public BrowserPublicKeyCredentialCreationOptions build() {
            BrowserPublicKeyCredentialCreationOptions options = new BrowserPublicKeyCredentialCreationOptions();
            options.delegate = delegate;
            options.origin = origin;
            options.clientDataHash = clientDataHash;
            return options;
        }
    }

    public static BrowserPublicKeyCredentialCreationOptions deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    public static final Creator<BrowserPublicKeyCredentialCreationOptions> CREATOR = new AutoCreator<>(BrowserPublicKeyCredentialCreationOptions.class);
}
