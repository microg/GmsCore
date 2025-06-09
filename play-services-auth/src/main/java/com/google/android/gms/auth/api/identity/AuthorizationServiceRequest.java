package com.google.android.gms.auth.api.identity;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Class;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Field;

import androidx.annotation.Nullable;

@Class(creator = "AuthorizationServiceRequestCreator")
public class AuthorizationServiceRequest extends AbstractSafeParcelable {

    @Field(id = 1)
    @Nullable
    public final String tokenType;

    @Field(id = 2)
    @Nullable
    public final String clientId;

    @Field(id = 3)
    @Nullable
    public final String scope;

    @Field(id = 4)
    @Nullable
    public final String redirectUri;

    public static final Creator<AuthorizationServiceRequest> CREATOR =
            new AutoSafeParcelable.Creator<>(AuthorizationServiceRequest.class);

    private AuthorizationServiceRequest(
            @Nullable String tokenType,
            @Nullable String clientId,
            @Nullable String scope,
            @Nullable String redirectUri) {
        this.tokenType = tokenType;
        this.clientId = clientId;
        this.scope = scope;
        this.redirectUri = redirectUri;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AuthorizationServiceRequest{" +
                "tokenType='" + tokenType + '\'' +
                ", clientId='" + clientId + '\'' +
                ", scope='" + scope + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                '}';
    }
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AuthorizationServiceRequest that = (AuthorizationServiceRequest) o;

    if (tokenType != null ? !tokenType.equals(that.tokenType) : that.tokenType != null) return false;
    if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
    if (scope != null ? !scope.equals(that.scope) : that.scope != null) return false;
    return redirectUri != null ? redirectUri.equals(that.redirectUri) : that.redirectUri == null;
}

@Override
public int hashCode() {
    int result = tokenType != null ? tokenType.hashCode() : 0;
    result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
    result = 31 * result + (scope != null ? scope.hashCode() : 0);
    result = 31 * result + (redirectUri != null ? redirectUri.hashCode() : 0);
    return result;
        }
    public static final class Builder {
        private String tokenType;
        private String clientId;
        private String scope;
        private String redirectUri;

        public Builder setTokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder setClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder setScope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public AuthorizationServiceRequest build() {
            return new AuthorizationServiceRequest(tokenType, clientId, scope, redirectUri);
        }
    }
  }
