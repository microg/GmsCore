/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.net.Uri;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * Parameters to a make credential request from a Web browser.
 */
@PublicApi
@SafeParcelable.Class
public class BrowserPublicKeyCredentialCreationOptions extends BrowserRequestOptions {
    @Field(value = 2, getterName = "getPublicKeyCredentialCreationOptions")
    @NonNull
    private PublicKeyCredentialCreationOptions delegate;
    @Field(value = 3, getterName = "getOrigin")
    @NonNull
    private Uri origin;
    @Field(value = 4, getterName = "getClientDataHash")
    @Nullable
    private byte[] clientDataHash;

    @Constructor
    BrowserPublicKeyCredentialCreationOptions(@Param(2) @NonNull PublicKeyCredentialCreationOptions delegate, @Param(3) @NonNull Uri origin, @Param(4) @Nullable byte[] clientDataHash) {
        this.delegate = delegate;
        this.origin = origin;
        this.clientDataHash = clientDataHash;
    }

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
    public PublicKeyCredentialCreationOptions getPublicKeyCredentialCreationOptions() {
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
        @NonNull
        private PublicKeyCredentialCreationOptions delegate;
        @NonNull
        private Uri origin;
        @Nullable
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
        public BrowserPublicKeyCredentialCreationOptions.Builder setClientDataHash(@NonNull byte[] clientDataHash) {
            this.clientDataHash = clientDataHash;
            return this;
        }

        /**
         * Sets the origin on whose behalf the calling browser is requesting a registration operation.
         */
        public BrowserPublicKeyCredentialCreationOptions.Builder setOrigin(@NonNull Uri origin) {
            this.origin = origin;
            return this;
        }

        /**
         * Sets the parameters to dictate the client behavior during this registration session.
         */
        public BrowserPublicKeyCredentialCreationOptions.Builder setPublicKeyCredentialCreationOptions(@NonNull PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions) {
            this.delegate = publicKeyCredentialCreationOptions;
            return this;
        }

        /**
         * Builds the {@link BrowserPublicKeyCredentialCreationOptions} object.
         */
        @NonNull
        public BrowserPublicKeyCredentialCreationOptions build() {
            return new BrowserPublicKeyCredentialCreationOptions(delegate, origin, clientDataHash);
        }
    }

    public static BrowserPublicKeyCredentialCreationOptions deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<BrowserPublicKeyCredentialCreationOptions> CREATOR = findCreator(BrowserPublicKeyCredentialCreationOptions.class);
}
