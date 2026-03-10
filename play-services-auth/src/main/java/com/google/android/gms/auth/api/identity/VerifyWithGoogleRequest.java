/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class VerifyWithGoogleRequest extends AbstractSafeParcelable {

    @Field(1)
    public List<Scope> requestedScopes;
    @Field(2)
    public String serverClientId;
    @Field(3)
    public boolean offlineAccess;
    @Field(4)
    public String sessionId;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<VerifyWithGoogleRequest> CREATOR = findCreator(VerifyWithGoogleRequest.class);

    @Override
    public String toString() {
        return "VerifyWithGoogleRequest{" +
                "requestedScopes=" + requestedScopes +
                ", serverClientId='" + serverClientId + '\'' +
                ", offlineAccess=" + offlineAccess +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
