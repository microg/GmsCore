/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class VerifyAssertionRequest extends AbstractSafeParcelable {

    @Field(2)
    public String requestUri;
    @Field(3)
    public String accessToken;
    @Field(4)
    public String idToken;
    @Field(5)
    public String instanceId;
    @Field(6)
    public String providerId;
    @Field(7)
    public String pendingIdToken;
    @Field(8)
    public String postBody;
    @Field(9)
    public String localId;
    @Field(10)
    public boolean returnIdpCredential;
    @Field(11)
    public boolean returnSecureToken;
    @Field(12)
    public String delegatedProjectNumber;
    @Field(13)
    public String sessionId;
    @Field(14)
    public String queryParameter;
    @Field(15)
    public String tenantId;
    @Field(16)
    public boolean returnRefreshToken;
    @Field(17)
    public String tenantProjectNumber;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<VerifyAssertionRequest> CREATOR = findCreator(VerifyAssertionRequest.class);

}
