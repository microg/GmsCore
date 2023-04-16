/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

/**
 * Represents the Token binding information provided by the relying party.
 */
@PublicApi
public class TokenBinding extends AutoSafeParcelable {
    /**
     * A singleton instance representing that token binding is not supported by the client.
     */
    public static final TokenBinding NOT_SUPPORTED = new TokenBinding(TokenBindingStatus.NOT_SUPPORTED, null);
    /**
     * A singleton instance representing that token binding is supported by the client, but unused by the relying party.
     */
    public static final TokenBinding SUPPORTED = new TokenBinding(TokenBindingStatus.SUPPORTED, null);

    @Field(2)
    private TokenBindingStatus status;
    @Field(3)
    private String tokenBindingId;

    private TokenBinding() {
    }

    /**
     * Constructs an instance of a {@link TokenBinding} for a provided token binding id.
     */
    public TokenBinding(String tokenBindingId) {
        status = TokenBindingStatus.PRESENT;
        this.tokenBindingId = tokenBindingId;
    }

    private TokenBinding(TokenBindingStatus status, String tokenBindingId) {
        this.status = status;
        this.tokenBindingId = tokenBindingId;
    }

    /**
     * Returns the token binding ID if the token binding status is {@code PRESENT}, otherwise returns null.
     */
    public String getTokenBindingId() {
        return tokenBindingId;
    }

    /**
     * Returns the stringified {@link TokenBinding.TokenBindingStatus} associated with this instance.
     */
    public String getTokenBindingStatusAsString() {
        return status.toString();
    }

    /**
     * Returns this {@link TokenBinding} object as a {@link JSONObject}.
     */
    public JSONObject toJsonObject() {
        try {
            return new JSONObject().put("status", this.status).put("id", this.tokenBindingId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenBinding)) return false;

        TokenBinding that = (TokenBinding) o;

        if (status != that.status) return false;
        return tokenBindingId != null ? tokenBindingId.equals(that.tokenBindingId) : that.tokenBindingId == null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{status, tokenBindingId});
    }

    @Override
    public String toString() {
        return ToStringHelper.name("TokenBinding")
                .value(tokenBindingId)
                .field("status", status)
                .end();
    }

    /**
     * The token binding status specified by the client.
     */
    public enum TokenBindingStatus implements Parcelable {
        /**
         * The client supports token binding and the relying party is using it.
         */
        PRESENT("present"),
        /**
         * The client supports token binding but the relying party is not using it.
         */
        SUPPORTED("supported"),
        /**
         * The client does not support token binding.
         */
        NOT_SUPPORTED("not-supported");
        private String value;

        TokenBindingStatus(String value) {
            this.value = value;
        }

        @PublicApi(exclude = true)
        public static TokenBindingStatus fromString(String str) throws UnsupportedTokenBindingStatusException {
            for (TokenBindingStatus value : values()) {
                if (value.value.equals(str)) return value;
            }
            throw new UnsupportedTokenBindingStatusException("TokenBindingStatus " + str + " not supported");
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(value);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<TokenBindingStatus> CREATOR = new Creator<TokenBindingStatus>() {
            @Override
            public TokenBindingStatus createFromParcel(Parcel in) {
                try {
                    return fromString(in.readString());
                } catch (UnsupportedTokenBindingStatusException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TokenBindingStatus[] newArray(int size) {
                return new TokenBindingStatus[size];
            }
        };

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Exception thrown when an unsupported or unrecognized {@link TokenBinding.TokenBindingStatus} is encountered.
     */
    public static class UnsupportedTokenBindingStatusException extends Exception {
        public UnsupportedTokenBindingStatusException(String message) {
            super(message);
        }
    }

    @PublicApi(exclude = true)
    public static final Creator<TokenBinding> CREATOR = new AutoCreator<>(TokenBinding.class);
}
