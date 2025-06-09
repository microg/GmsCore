package com.google.android.gms.auth.api.identity;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Class;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Field;

import androidx.annotation.Nullable;

@Class(creator = "AuthorizationServiceResponseCreator")
public class AuthorizationServiceResponse extends AbstractSafeParcelable {

    @Field(id = 1)
    @Nullable
    public final String accessToken;

    @Field(id = 2)
    @Nullable
    public final String refreshToken;

    @Field(id = 3)
    @Nullable
    public final String idToken;

    @Field(id = 4)
    public final long expiresIn;

    public static final Creator<AuthorizationServiceResponse> CREATOR =
            new AutoSafeParcelable.Creator<>(AuthorizationServiceResponse.class);

    private AuthorizationServiceResponse(
            @Nullable String accessToken,
            @Nullable String refreshToken,
            @Nullable String idToken,
            long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.idToken = idToken;
        this.expiresIn = expiresIn;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AuthorizationServiceResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", idToken='" + idToken + '\'' +
                ", expiresIn=" + expiresIn +
                '}';
    }
    
    public static final class Builder {
        private String accessToken;
        private String refreshToken;
        private String idToken;
        private long expiresIn;

        public Builder setAccessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder setIdToken(String idToken) {
            this.idToken = idToken;
            return this;
        }

        public Builder setExpiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public AuthorizationServiceResponse build() {
            return new AuthorizationServiceResponse(accessToken, refreshToken, idToken, expiresIn);
        }
    }
}
