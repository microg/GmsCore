/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
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

import java.util.Objects;

/**
 * Parameters that configure the Clear token request.
 */
@SafeParcelable.Class
public class ClearTokenRequest extends AbstractSafeParcelable {
    @NonNull
    @Field(value = 1, getterName = "getToken")
    private final String token;
    @Nullable
    @Field(value = 2, getterName = "getSessionId")
    private final String sessionId;

    public static SafeParcelableCreatorAndWriter<ClearTokenRequest> CREATOR = findCreator(ClearTokenRequest.class);

    @Constructor
    ClearTokenRequest(@NonNull @Param(1) String token, @Nullable @Param(2) String sessionId) {
        this.token = token;
        this.sessionId = sessionId;
    }

    /**
     * Returns a new {@link ClearTokenRequest.Builder}.
     */
    public static ClearTokenRequest.Builder builder() {
        return new ClearTokenRequest.Builder() {
            @Nullable
            private String token;
            @Nullable
            private String sessionId;

            @Override
            public ClearTokenRequest build() {
                if (token == null) {
                    throw new IllegalStateException("Missing required properties: token");
                }
                return new ClearTokenRequest(token, sessionId);
            }

            @Override
            public ClearTokenRequest.Builder setToken(@NonNull String token) {
                this.token = token;
                return this;
            }

            @Override
            public ClearTokenRequest.Builder setSessionId(@Nullable String sessionId) {
                this.sessionId = sessionId;
                return this;
            }
        };
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ClearTokenRequest)) return false;

        ClearTokenRequest that = (ClearTokenRequest) o;
        return token.equals(that.token) && Objects.equals(sessionId, that.sessionId);
    }

    @NonNull
    public String getToken() {
        return token;
    }

    @Hide
    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{token, sessionId});
    }

    /**
     * Builder for {@link ClearTokenRequest}.
     */
    public static abstract class Builder {
        /**
         * Builds the {@link ClearTokenRequest}.
         */
        public abstract ClearTokenRequest build();

        /**
         * Sets the token being cleared from the cache.
         */
        public abstract ClearTokenRequest.Builder setToken(String token);

        @Hide
        public abstract ClearTokenRequest.Builder setSessionId(String sessionId);
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        CREATOR.writeToParcel(this, parcel, flags);
    }
}
