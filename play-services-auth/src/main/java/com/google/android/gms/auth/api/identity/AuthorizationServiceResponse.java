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
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AuthorizationServiceResponse that = (AuthorizationServiceResponse) o;

    if (expiresIn != that.expiresIn) return false;
    if (accessToken != null ? !accessToken.equals(that.accessToken) : that.accessToken != null)
        return false;
    if (refreshToken != null ? !refreshToken.equals(that.refreshToken) : that.refreshToken != null)
        return false;
    return idToken != null ? idToken.equals(that.idToken) : that.idToken == null;
}

@Override
public int hashCode() {
    int result = accessToken != null ? accessToken.hashCode() : 0;
    result = 31 * result + (refreshToken != null ? refreshToken.hashCode() : 0);
    result = 31 * result + (idToken != null ? idToken.hashCode() : 0);
    result = 31 * result + Long.hashCode(expiresIn);
    return result;
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
