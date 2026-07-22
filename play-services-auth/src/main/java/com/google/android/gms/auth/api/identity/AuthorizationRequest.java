/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import android.accounts.Account;
import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class AuthorizationRequest extends AbstractSafeParcelable {

    @Field(1)
    public List<Scope> requestedScopes;
    @Field(2)
    public String serverClientId;
    @Field(3)
    public boolean serverAuthCodeRequested;
    @Field(4)
    public boolean idTokenRequested;
    @Field(5)
    public Account account;
    @Field(6)
    public String hostedDomainFilter;
    @Field(7)
    public String sessionId;
    @Field(8)
    public boolean forceCodeForRefreshToken;
    @Field(9)
    public Bundle bundle;
    @Field(10)
    public boolean offlineAccess;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthorizationRequest> CREATOR = findCreator(AuthorizationRequest.class);

    @Override
    public String toString() {
        return "AuthorizationRequest{" +
                "requestedScopes=" + requestedScopes +
                ", serverClientId='" + serverClientId + '\'' +
                ", serverAuthCodeRequested=" + serverAuthCodeRequested +
                ", idTokenRequested=" + idTokenRequested +
                ", account=" + account +
                ", hostedDomainFilter='" + hostedDomainFilter + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", forceCodeForRefreshToken=" + forceCodeForRefreshToken +
                ", bundle=" + bundle +
                ", offlineAccess=" + offlineAccess +
                '}';
    }
}
