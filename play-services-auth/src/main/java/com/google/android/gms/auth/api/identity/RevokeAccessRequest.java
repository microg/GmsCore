/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.accounts.Account;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

import java.util.List;
import java.util.Objects;

/**
 * Parameters that configure the Revoke Access request.
 */
@SafeParcelable.Class
public class RevokeAccessRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getScopes")
    @NonNull
    private final List<Scope> scopes;
    @Field(value = 2, getterName = "getAccount")
    @NonNull
    private final Account account;
    @Field(value = 3, getterName = "getSessionId")
    @Nullable
    private final String sessionId;

    @Constructor
    RevokeAccessRequest(@NonNull @Param(1) List<Scope> scopes, @NonNull @Param(2) Account account, @Nullable @Param(3) String sessionId) {
        this.scopes = scopes;
        this.account = account;
        this.sessionId = sessionId;
    }

    /**
     * Returns a new {@link RevokeAccessRequest.Builder}.
     */
    @NonNull
    public static RevokeAccessRequest.Builder builder() {
        return new Builder() {
            private List<Scope> scopes;
            private Account account;
            private String sessionId;

            @NonNull
            @Override
            public RevokeAccessRequest build() {
                if (scopes == null) throw new IllegalStateException("Missing required properties: scopes");
                if (account == null) throw new IllegalStateException("Missing required properties: account");
                return new RevokeAccessRequest(scopes, account, sessionId);
            }

            @NonNull
            @Override
            public Builder setAccount(@NonNull Account account) {
                this.account = account;
                return this;
            }

            @NonNull
            @Override
            public Builder setScopes(@NonNull List<Scope> scopes) {
                this.scopes = scopes;
                return this;
            }

            @NonNull
            @Override
            public Builder setSessionId(@Nullable String sessionId) {
                this.sessionId = sessionId;
                return this;
            }
        };
    }

    /**
     * Returns the account that the application is revoking access for.
     */
    @NonNull
    public Account getAccount() {
        return account;
    }

    /**
     * Returns the scopes that access is being revoked for.
     */
    @NonNull
    public List<Scope> getScopes() {
        return scopes;
    }

    @Hide
    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof RevokeAccessRequest)) return false;

        RevokeAccessRequest that = (RevokeAccessRequest) o;
        return scopes.equals(that.scopes) && account.equals(that.account) && Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{scopes, account, sessionId});
    }

    /**
     * Builder for {@link RevokeAccessRequest}.
     */
    public static abstract class Builder {
        @NonNull
        public abstract RevokeAccessRequest build();

        @NonNull
        public abstract Builder setAccount(@NonNull Account account);

        @NonNull
        public abstract Builder setScopes(@NonNull List<Scope> scopes);

        @NonNull
        @Hide
        public abstract Builder setSessionId(@Nullable String sessionId);
    }

    public static final SafeParcelableCreatorAndWriter<RevokeAccessRequest> CREATOR = findCreator(RevokeAccessRequest.class);

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        CREATOR.writeToParcel(this, parcel, flags);
    }

    @Override
    public String toString() {
        return "RevokeAccessRequest{" +
                "scopes=" + scopes +
                ", account=" + account +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
