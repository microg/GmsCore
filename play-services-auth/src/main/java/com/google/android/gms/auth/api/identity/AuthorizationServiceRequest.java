package com.google.android.gms.auth.api.identity;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParcelable;

import android.annotation.Nullable;
import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Represents a request for authorization with token type, client ID, scope, and redirect URI.
 */
@SafeParcelable.Class(creator = "AuthorizationServiceRequestCreator")
public class AuthorizationServiceRequest extends AutoSafeParcelable {

    @SafeParcelable.Field(id = 1)
    @Nullable
    public final String tokenType;

    @SafeParcelable.Field(id = 2)
    @NonNull
    public final String clientId;

    @SafeParcelable.Field(id = 3)
    @Nullable
    public final String scope;

    @SafeParcelable.Field(id = 4)
    @NonNull
    public final String redirectUri;

    public static final Creator<AuthorizationServiceRequest> CREATOR =
            new AutoSafeParcelable.Creator<>(AuthorizationServiceRequest.class);

    private AuthorizationServiceRequest(
            @Nullable String tokenType,
            @NonNull String clientId,
            @Nullable String scope,
            @NonNull String redirectUri) {
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
        return new StringBuilder("AuthorizationServiceRequest{")
                .append("tokenType='").append(tokenType).append('\'')
                .append(", clientId='").append(clientId).append('\'')
                .append(", scope='").append(scope).append('\'')
                .append(", redirectUri='").append(redirectUri).append('\'')
                .append('}')
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizationServiceRequest that = (AuthorizationServiceRequest) o;
        return Objects.equals(tokenType, that.tokenType) &&
               Objects.equals(clientId, that.clientId) &&
               Objects.equals(scope, that.scope) &&
               Objects.equals(redirectUri, that.redirectUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenType, clientId, scope, redirectUri);
    }

    /**
     * Builder class for constructing {@link AuthorizationServiceRequest} instances.
     */
    public static class Builder {
        private String tokenType;
        private String clientId;
        private String scope;
        private String redirectUri;

        private Builder() {}

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
            if (clientId == null || redirectUri == null) {
                throw new IllegalStateException("clientId and redirectUri must not be null");
            }
            return new AuthorizationServiceRequest(tokenType, clientId, scope, redirectUri);
        }
    }
                }
