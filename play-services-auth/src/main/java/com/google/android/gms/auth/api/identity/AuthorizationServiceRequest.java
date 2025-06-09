package com.google.android.gms.auth.api.identity;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParcelable;

import android.annotation.Nullable;

@SafeParcelable.Class(creator = "AuthorizationServiceRequestCreator")
public class AuthorizationServiceRequest extends AutoSafeParcelable {
    @SafeParcelable.Field(id = 1)
    @Nullable
    public final String tokenType;

    @SafeParcelable.Field(id = 2)
    @Nullable
    public final String clientId;

    @SafeParcelable.Field(id = 3)
    @Nullable
    public final String scope;

    @SafeParcelable.Field(id = 4)
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

    public static class Builder {
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
