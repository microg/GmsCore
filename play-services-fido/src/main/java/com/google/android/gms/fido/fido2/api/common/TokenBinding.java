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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.json.JSONException;
import org.json.JSONObject;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * Represents the Token binding information provided by the relying party.
 */
@PublicApi
@SafeParcelable.Class
public class TokenBinding extends AbstractSafeParcelable {
    /**
     * A singleton instance representing that token binding is not supported by the client.
     */
    @NonNull
    public static final TokenBinding NOT_SUPPORTED = new TokenBinding(TokenBindingStatus.NOT_SUPPORTED, null);
    /**
     * A singleton instance representing that token binding is supported by the client, but unused by the relying party.
     */
    @NonNull
    public static final TokenBinding SUPPORTED = new TokenBinding(TokenBindingStatus.SUPPORTED, null);

    @Field(value = 2, getterName = "getTokenBindingStatus")
    @NonNull
    private TokenBindingStatus status;
    @Field(value = 3, getterName = "getTokenBindingId")
    @Nullable
    private String tokenBindingId;

    private TokenBinding() {
    }

    /**
     * Constructs an instance of a {@link TokenBinding} for a provided token binding id.
     */
    public TokenBinding(@Nullable String tokenBindingId) {
        status = TokenBindingStatus.PRESENT;
        this.tokenBindingId = tokenBindingId;
    }

    @Constructor
    TokenBinding(@Param(2) @NonNull TokenBindingStatus status, @Param(3) @Nullable String tokenBindingId) {
        this.status = status;
        this.tokenBindingId = tokenBindingId;
    }

    /**
     * Returns the token binding ID if the token binding status is {@code PRESENT}, otherwise returns null.
     */
    @Nullable
    public String getTokenBindingId() {
        return tokenBindingId;
    }

    @Hide
    @NonNull
    public TokenBindingStatus getTokenBindingStatus() {
        return status;
    }

    /**
     * Returns the stringified {@link TokenBinding.TokenBindingStatus} associated with this instance.
     */
    @NonNull
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
    @NonNull
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

        @NonNull
        private final String value;

        TokenBindingStatus(@NonNull String value) {
            this.value = value;
        }

        @Hide
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
        @NonNull
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

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Hide
    public static final SafeParcelableCreatorAndWriter<TokenBinding> CREATOR = findCreator(TokenBinding.class);
}
